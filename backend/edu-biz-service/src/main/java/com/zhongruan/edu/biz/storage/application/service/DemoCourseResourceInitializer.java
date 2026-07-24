package com.zhongruan.edu.biz.storage.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseMaterialEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMaterialMapper;
import com.zhongruan.edu.biz.storage.config.FileStorageProperties;
import com.zhongruan.edu.biz.storage.infrastructure.persistence.entity.StoredFileEntity;
import com.zhongruan.edu.biz.storage.infrastructure.persistence.mapper.StoredFileMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Materializes the canonical showcase resources so seeded metadata never points at imaginary files. */
@Component
public class DemoCourseResourceInitializer implements ApplicationRunner {
    private static final String MARKDOWN = "text/markdown; charset=UTF-8";
    private static final List<DemoResource> RESOURCES = List.of(
            new DemoResource(22501L, "Java课程导学.md", "demo/course/java-guide.md"),
            new DemoResource(22502L, "数据结构思维导图.md", "demo/course/data-structure-map.md"),
            new DemoResource(22503L, "高数公式表.md", "demo/course/calculus-formula.md"));

    private final Path storageRoot;
    private final StoredFileMapper fileMapper;
    private final CourseMaterialMapper materialMapper;

    public DemoCourseResourceInitializer(
            FileStorageProperties properties,
            StoredFileMapper fileMapper,
            CourseMaterialMapper materialMapper) {
        this.storageRoot = properties.root().toAbsolutePath().normalize();
        this.fileMapper = fileMapper;
        this.materialMapper = materialMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DemoResource demo : RESOURCES) {
            StoredFileEntity file = fileMapper.selectById(demo.fileId());
            if (file == null) {
                continue;
            }
            byte[] content = classpathBytes(demo.objectKey());
            Path target = storageRoot.resolve(demo.objectKey()).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new IllegalStateException("演示资料路径越界: " + demo.objectKey());
            }
            try {
                Files.createDirectories(target.getParent());
                try (InputStream input = new ClassPathResource(demo.objectKey()).getInputStream()) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("无法落盘演示资料: " + demo.objectKey(), exception);
            }

            file.setOriginalName(demo.originalName());
            file.setObjectKey(demo.objectKey());
            file.setStorageProvider("LOCAL");
            file.setFileSize((long) content.length);
            file.setMimeType(MARKDOWN);
            file.setSha256(sha256(content));
            file.setPurpose("COURSE_MATERIAL");
            file.setFileStatus("ACTIVE");
            fileMapper.updateById(file);

            materialMapper.selectList(Wrappers.<CourseMaterialEntity>lambdaQuery()
                            .eq(CourseMaterialEntity::getFileId, demo.fileId()))
                    .forEach(material -> {
                        material.setFileSize((long) content.length);
                        material.setMimeType(MARKDOWN);
                        materialMapper.updateById(material);
                    });
        }
    }

    private byte[] classpathBytes(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("演示资料未打包: " + path, exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK 缺少 SHA-256", exception);
        }
    }

    private record DemoResource(Long fileId, String originalName, String objectKey) {}
}
