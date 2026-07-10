package com.zhongruan.edu.biz.storage.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "edu.storage")
public record FileStorageProperties(Path root, DataSize maxFileSize) {
    public FileStorageProperties {
        root = root == null ? Path.of("data", "uploads") : root;
        maxFileSize = maxFileSize == null ? DataSize.ofMegabytes(50) : maxFileSize;
    }
}
