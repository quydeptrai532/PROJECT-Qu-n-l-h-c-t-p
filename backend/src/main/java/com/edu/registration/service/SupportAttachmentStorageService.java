package com.edu.registration.service;

import com.edu.registration.config.SupportRequestProperties;
import com.edu.registration.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SupportAttachmentStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupportAttachmentStorageService.class);

    private static final Pattern SAFE_STORED_NAME = Pattern.compile("^[a-fA-F0-9-]{36}\\.[a-z0-9]{2,5}$");
    private static final Set<String> FORBIDDEN_SEGMENTS = Set.of(
            "exe", "dll", "bat", "cmd", "ps1", "sh", "bash", "php", "phtml",
            "jsp", "jspx", "asp", "aspx", "js", "mjs", "html", "htm", "svg",
            "jar", "war", "class", "py", "rb", "pl", "cgi", "com", "scr", "msi"
    );
    private static final Map<String, String> EXTENSION_TO_MIME = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg"
    );

    private final SupportRequestProperties properties;

    public SupportAttachmentStorageService(SupportRequestProperties properties) {
        this.properties = properties != null ? properties : SupportRequestProperties.defaults();
    }

    public record StoredFileResult(
            String originalFileName,
            String storedFileName,
            String contentType,
            long fileSize,
            String checksumSha256,
            Path storedPath
    ) {
    }

    public StoredFileResult store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Tệp đính kèm không được rỗng");
        }

        String cleanOriginalName = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extractAndValidateExtension(cleanOriginalName);

        byte[] content = readBoundedBytes(file, properties.maxFileSizeBytes());
        if (content.length == 0) {
            throw ApiException.badRequest("Tệp đính kèm không được rỗng");
        }

        String detectedMime = detectAndVerifyContentSignature(content, extension);
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path baseDir = resolveBaseDirectory();
        Path targetPath = baseDir.resolve(storedFileName).normalize();
        if (!targetPath.startsWith(baseDir)) {
            throw ApiException.badRequest("Đường dẫn lưu trữ tệp không hợp lệ");
        }

        try {
            Files.createDirectories(baseDir);
            Files.write(targetPath, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            log.error("Không thể lưu tệp đính kèm vào thư mục lưu trữ");
            throw ApiException.badRequest("Không thể lưu tệp đính kèm");
        }

        return new StoredFileResult(
                cleanOriginalName,
                storedFileName,
                detectedMime,
                content.length,
                sha256Hex(content),
                targetPath
        );
    }

    public byte[] readStoredFile(String storedFileName) {
        if (storedFileName == null || !SAFE_STORED_NAME.matcher(storedFileName).matches()) {
            throw ApiException.notFound("Tệp đính kèm không hợp lệ hoặc không tồn tại");
        }
        Path baseDir = resolveBaseDirectory();
        Path targetPath = baseDir.resolve(storedFileName).normalize();
        if (!targetPath.startsWith(baseDir) || !Files.isRegularFile(targetPath)) {
            throw ApiException.notFound("Không tìm thấy tệp đính kèm trên máy chủ");
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException ex) {
            log.error("Không đọc được tệp đính kèm từ lưu trữ");
            throw ApiException.notFound("Không thể đọc tệp đính kèm");
        }
    }

    public void cleanupPaths(Collection<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        Path baseDir = resolveBaseDirectory();
        for (Path path : paths) {
            if (path == null) {
                continue;
            }
            Path normalized = path.toAbsolutePath().normalize();
            if (normalized.startsWith(baseDir)) {
                try {
                    Files.deleteIfExists(normalized);
                } catch (IOException ex) {
                    log.warn("Không thể xóa tệp mồ côi khi hoàn tác giao dịch");
                }
            }
        }
    }

    String sanitizeOriginalFilename(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw ApiException.badRequest("Tên tệp đính kèm không hợp lệ");
        }
        String trimmed = rawName.trim();
        if (trimmed.indexOf('\0') >= 0
                || trimmed.contains("..")
                || trimmed.contains("/")
                || trimmed.contains("\\")
                || trimmed.contains(":")) {
            throw ApiException.badRequest("Tên tệp đính kèm chứa ký tự đường dẫn không hợp lệ");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isISOControl(trimmed.charAt(i))) {
                throw ApiException.badRequest("Tên tệp đính kèm chứa ký tự điều khiển không hợp lệ");
            }
        }
        if (trimmed.length() > 200) {
            throw ApiException.badRequest("Tên tệp đính kèm tối đa 200 ký tự");
        }
        return trimmed;
    }

    String extractAndValidateExtension(String fileName) {
        if (fileName.startsWith(".") || fileName.endsWith(".")) {
            throw ApiException.badRequest("Phần mở rộng tệp không hợp lệ");
        }
        String[] parts = fileName.split("\\.");
        if (parts.length < 2) {
            throw ApiException.badRequest("Tệp đính kèm phải có phần mở rộng hợp lệ");
        }
        for (int i = 1; i < parts.length - 1; i++) {
            String middle = parts[i].toLowerCase(Locale.ROOT);
            if (FORBIDDEN_SEGMENTS.contains(middle)) {
                throw ApiException.badRequest("Tên tệp chứa phần mở rộng thực thi bị cấm");
            }
        }
        String ext = parts[parts.length - 1].toLowerCase(Locale.ROOT);
        if (FORBIDDEN_SEGMENTS.contains(ext)
                || !properties.allowedExtensionSet().contains(ext)
                || !EXTENSION_TO_MIME.containsKey(ext)) {
            throw ApiException.badRequest("Định dạng tệp không được phép. Chỉ chấp nhận: "
                    + String.join(", ", properties.allowedExtensions()));
        }
        return ext;
    }

    private byte[] readBoundedBytes(MultipartFile file, long maxBytes) {
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest("Kích thước tệp vượt quá giới hạn cho phép ("
                    + (maxBytes / (1024 * 1024)) + " MB)");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] data = in.readNBytes((int) Math.min(maxBytes + 1, Integer.MAX_VALUE));
            if (data.length > maxBytes) {
                throw ApiException.badRequest("Kích thước tệp vượt quá giới hạn cho phép ("
                        + (maxBytes / (1024 * 1024)) + " MB)");
            }
            return data;
        } catch (IOException ex) {
            throw ApiException.badRequest("Không thể đọc dữ liệu tệp tải lên");
        }
    }

    String detectAndVerifyContentSignature(byte[] content, String extension) {
        if (startsWithExecutableOrScript(content)) {
            throw ApiException.badRequest("Tệp tải lên chứa định dạng thực thi hoặc mã kịch bản bị từ chối");
        }

        String detectedMime = null;
        if (isPdf(content)) {
            detectedMime = "application/pdf";
        } else if (isPng(content)) {
            detectedMime = "image/png";
        } else if (isJpeg(content)) {
            detectedMime = "image/jpeg";
        }

        String expectedMime = EXTENSION_TO_MIME.get(extension);
        if (detectedMime == null || !detectedMime.equals(expectedMime)) {
            throw ApiException.badRequest("Nội dung thực tế của tệp không khớp với định dạng ." + extension);
        }
        return detectedMime;
    }

    private boolean startsWithExecutableOrScript(byte[] b) {
        if (b.length >= 2 && b[0] == 'M' && b[1] == 'Z') {
            return true; // Windows PE / EXE / DLL
        }
        if (b.length >= 4 && b[0] == 0x7F && b[1] == 'E' && b[2] == 'L' && b[3] == 'F') {
            return true; // ELF executable
        }
        if (b.length >= 2 && b[0] == '#' && b[1] == '!') {
            return true; // Shebang script
        }
        int headLen = Math.min(b.length, 256);
        String head = new String(b, 0, headLen, StandardCharsets.ISO_8859_1)
                .trim()
                .toLowerCase(Locale.ROOT);
        return head.startsWith("<?php")
                || head.startsWith("<script")
                || head.startsWith("<!doctype html")
                || head.startsWith("<html")
                || head.startsWith("<svg");
    }

    private boolean isPdf(byte[] b) {
        if (b.length < 8) {
            return false;
        }
        boolean hasHeader = b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-';
        if (!hasHeader) {
            return false;
        }
        String sample = new String(b, 0, Math.min(b.length, 4096), StandardCharsets.ISO_8859_1)
                .toLowerCase(Locale.ROOT);
        return !sample.contains("<script") && !sample.contains("<?php");
    }

    private boolean isPng(byte[] b) {
        return b.length >= 8
                && b[0] == (byte) 0x89
                && b[1] == 0x50
                && b[2] == 0x4E
                && b[3] == 0x47
                && b[4] == 0x0D
                && b[5] == 0x0A
                && b[6] == 0x1A
                && b[7] == 0x0A;
    }

    private boolean isJpeg(byte[] b) {
        return b.length >= 4
                && b[0] == (byte) 0xFF
                && b[1] == (byte) 0xD8
                && b[2] == (byte) 0xFF;
    }

    private Path resolveBaseDirectory() {
        return Paths.get(properties.storageDir()).toAbsolutePath().normalize();
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }
}
