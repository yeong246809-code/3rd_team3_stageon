package kr.co.stageon.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 공연 포스터 저장 위치를 환경별 객체 저장소로 연결합니다. */
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final ObjectStorageService storageService;
    private final StorageProperties properties;

    public StoredFile savePoster(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return storageService.storeImage(file, properties.getS3().getPosterPrefix());
    }
}
