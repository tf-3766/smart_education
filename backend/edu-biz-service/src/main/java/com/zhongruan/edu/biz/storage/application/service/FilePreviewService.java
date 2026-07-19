package com.zhongruan.edu.biz.storage.application.service;

import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.storage.application.service.FileStorageService.StoredFileContent;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.stereotype.Service;

@Service
public class FilePreviewService {
    private static final int MAX_WIDTH = 1600;
    private final FileStorageService fileStorageService;

    public FilePreviewService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public PreviewImage renderPptx(AuthenticatedUser user, Long fileId, int page) {
        StoredFileContent content = fileStorageService.content(user, fileId);
        String name = content.metadata().originalName().toLowerCase();
        if (!name.endsWith(".pptx")) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "当前文件不是可预览的 PPTX 文件");
        }
        try (InputStream input = content.resource().getInputStream(); XMLSlideShow show = new XMLSlideShow(input)) {
            List<XSLFSlide> slides = show.getSlides();
            if (slides.isEmpty()) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "PPTX 中没有可预览的幻灯片");
            }
            if (page < 0 || page >= slides.size()) {
                throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "幻灯片页码超出范围");
            }
            Dimension source = show.getPageSize();
            double scale = Math.min(1.75d, MAX_WIDTH / Math.max(1d, source.getWidth()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.scale(scale, scale);
                slides.get(page).draw(graphics);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", output)) {
                throw new IOException("PNG writer unavailable");
            }
            return new PreviewImage(output.toByteArray(), page, slides.size());
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "PPTX 预览生成失败，请确认文件未损坏");
        }
    }

    public record PreviewImage(byte[] content, int page, int pageCount) {}
}