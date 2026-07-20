package com.zhongruan.edu.biz.storage.application.service;

import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.storage.application.service.FileStorageService.StoredFileContent;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
public class MaterialTextExtractionService {
    private static final int MAX_TEXT_LENGTH = 300_000;
    private final FileStorageService fileStorageService;
    private final Map<String, ExtractedText> cache = new ConcurrentHashMap<>();
    private final String tesseractPath;
    private final String ocrLanguage;
    private final boolean tesseractAvailable;

    public MaterialTextExtractionService(
            FileStorageService fileStorageService,
            @Value("${app.file-extraction.ocr.enabled:true}") boolean ocrEnabled,
            @Value("${app.file-extraction.ocr.tesseract-path:}") String tesseractPath,
            @Value("${app.file-extraction.ocr.language:chi_sim+eng}") String ocrLanguage) {
        this.fileStorageService = fileStorageService;
        this.tesseractPath = tesseractPath == null ? "" : tesseractPath.trim();
        this.ocrLanguage = ocrLanguage == null || ocrLanguage.isBlank() ? "chi_sim+eng" : ocrLanguage.trim();
        this.tesseractAvailable = ocrEnabled && commandAvailable(this.tesseractPath);
    }

    public ExtractedText extract(AuthenticatedUser user, Long fileId) {
        fileStorageService.content(user, fileId);
        return extract(fileId);
    }

    public ExtractedText extract(Long fileId) {
        if (fileId == null) return new ExtractedText("", "NO_FILE", "资料没有关联平台文件", false);
        StoredFileContent content = fileStorageService.internalContent(fileId);
        String cacheKey = fileId + ":" + content.metadata().sha256();
        return cache.computeIfAbsent(cacheKey, ignored -> extractContent(content));
    }

    private ExtractedText extractContent(StoredFileContent content) {
        String name = content.metadata().originalName();
        String lowerName = name.toLowerCase(Locale.ROOT);
        String mime = content.metadata().mimeType() == null ? "" : content.metadata().mimeType().toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".pdf") || mime.contains("pdf")) return extractPdf(content);
            if ((mime.startsWith("image/") || lowerName.matches(".*\\.(png|jpe?g|gif|webp|tiff?)$")) && !tesseractAvailable) {
                return new ExtractedText("", "OCR_UNAVAILABLE", "图片资料需要 Tesseract OCR；当前机器未检测到可用命令", false);
            }
            return extractWithTika(content, false);
        } catch (Exception exception) {
            return new ExtractedText("", "FAILED", "正文抽取失败：" + safeMessage(exception), false);
        }
    }

    private ExtractedText extractPdf(StoredFileContent content) throws Exception {
        Path path = content.resource().getFile().toPath();
        StringBuilder text = new StringBuilder();
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages() && text.length() < MAX_TEXT_LENGTH; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                if (!pageText.isBlank()) text.append("第 ").append(page).append(" 页\n").append(pageText).append("\n\n");
            }
        }
        if (!text.isEmpty()) return success(text.toString(), "PDFBox 已抽取可选择文本");
        if (!tesseractAvailable) return new ExtractedText("", "OCR_UNAVAILABLE", "PDF 未包含可选择文本，需要 Tesseract OCR；当前机器未检测到可用命令", false);
        return extractWithTika(content, true);
    }

    private ExtractedText extractWithTika(StoredFileContent content, boolean ocrFallback)
            throws java.io.IOException, TikaException, SAXException {
        AutoDetectParser parser = new AutoDetectParser();
        if (tesseractAvailable && !tesseractPath.isBlank()) {
            parser.getAllComponentParsers().stream()
                    .filter(TesseractOCRParser.class::isInstance)
                    .map(TesseractOCRParser.class::cast)
                    .forEach(ocrParser -> ocrParser.setTesseractPath(tesseractPath));
        }
        BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, content.metadata().originalName());
        ParseContext context = new ParseContext();
        if (tesseractAvailable) {
            TesseractOCRConfig config = new TesseractOCRConfig();
            config.setLanguage(ocrLanguage);
            context.set(TesseractOCRConfig.class, config);
        }
        try (InputStream input = content.resource().getInputStream()) {
            parser.parse(input, handler, metadata, context);
        }
        String text = normalize(handler.toString());
        if (text.isBlank()) return new ExtractedText("", "EMPTY", "文件可读取，但没有抽取到正文", false);
        return success(text, ocrFallback ? "Tika + OCR 已抽取扫描 PDF" : "Apache Tika 已抽取正文");
    }

    private ExtractedText success(String text, String message) {
        String normalized = normalize(text);
        boolean truncated = normalized.length() > MAX_TEXT_LENGTH;
        if (truncated) normalized = normalized.substring(0, MAX_TEXT_LENGTH);
        return new ExtractedText(normalized, truncated ? "TRUNCATED" : "EXTRACTED", message, truncated);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\u0000", "").replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n").replaceAll("\n{3,}", "\n\n").trim();
    }

    private boolean commandAvailable(String configuredPath) {
        String executable = configuredPath == null || configuredPath.isBlank()
                ? "tesseract" : Path.of(configuredPath).resolve("tesseract.exe").toString();
        try {
            Process process = new ProcessBuilder(executable, "--version").redirectErrorStream(true).start();
            return process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record ExtractedText(String text, String status, String message, boolean truncated) {}
}