package kr.co.stageon.common.file;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

final class StorageFileSupport {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private StorageFileSupport() {
    }

    static String newImageKey(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지는 10MB 이하만 업로드할 수 있습니다.");
        }

        String extension = IMAGE_EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("JPG, PNG, WEBP, GIF 이미지만 업로드할 수 있습니다.");
        }

        String normalizedPrefix = normalizeKey(prefix);
        return normalizedPrefix + "/" + UUID.randomUUID() + extension;
    }

    static String normalizeKey(String value) {
        if (value == null) {
            throw new IllegalArgumentException("파일 경로가 비어 있습니다.");
        }
        String normalized = value.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank() || normalized.contains("..") || normalized.contains("//")) {
            throw new IllegalArgumentException("올바르지 않은 파일 경로입니다.");
        }
        return normalized;
    }

    static String publicUrl(StorageProperties properties, String objectKey) {
        String prefix = properties.getMediaUrlPrefix();
        prefix = (prefix == null || prefix.isBlank()) ? "/media" : prefix.trim();
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        return prefix + "/" + objectKey;
    }
}
