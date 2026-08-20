package kr.co.stageon.common.file;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {
    StoredFile storeImage(MultipartFile file, String prefix);

    StoredObject load(String objectKey);

    void delete(String objectKey);
}
