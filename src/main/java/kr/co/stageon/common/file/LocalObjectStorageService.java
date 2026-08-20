package kr.co.stageon.common.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** 로컬 개발용 저장소입니다. 운영에서는 stageon.storage.type=s3를 사용합니다. */
@Service
@ConditionalOnProperty(name = "stageon.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageService implements ObjectStorageService {
    private final StorageProperties properties;
    private final Path root;

    public LocalObjectStorageService(StorageProperties properties) {
        this.properties = properties;
        this.root = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile storeImage(MultipartFile file, String prefix) {
        String objectKey = StorageFileSupport.newImageKey(file, prefix);
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(objectKey, StorageFileSupport.publicUrl(properties, objectKey));
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 저장에 실패했습니다.", e);
        }
    }

    @Override
    public StoredObject load(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.isRegularFile(target)) {
            throw new StoredObjectNotFoundException(objectKey);
        }
        try {
            return new StoredObject(Files.readAllBytes(target), Files.probeContentType(target), null);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 삭제에 실패했습니다.", e);
        }
    }

    private Path resolve(String objectKey) {
        Path resolved = root.resolve(StorageFileSupport.normalizeKey(objectKey)).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("저장소 밖의 경로에는 접근할 수 없습니다.");
        }
        return resolved;
    }
}
