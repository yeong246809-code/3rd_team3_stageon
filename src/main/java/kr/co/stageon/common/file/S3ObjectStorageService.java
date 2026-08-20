package kr.co.stageon.common.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.UncheckedIOException;

@Service
@ConditionalOnProperty(name = "stageon.storage.type", havingValue = "s3")
public class S3ObjectStorageService implements ObjectStorageService {
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3Client;
    private final StorageProperties properties;
    private final String bucket;

    public S3ObjectStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.bucket = properties.getS3().getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3_BUCKET 설정이 필요합니다.");
        }
    }

    @Override
    public StoredFile storeImage(MultipartFile file, String prefix) {
        String objectKey = StorageFileSupport.newImageKey(file, prefix);
        try {
            byte[] content = file.getBytes();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .cacheControl(CACHE_CONTROL)
                    .contentLength((long) content.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
            return new StoredFile(objectKey, StorageFileSupport.publicUrl(properties, objectKey));
        } catch (IOException e) {
            throw new UncheckedIOException("S3 이미지 업로드에 실패했습니다.", e);
        } catch (S3Exception e) {
            throw new IllegalStateException("S3 이미지 업로드에 실패했습니다: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public StoredObject load(String objectKey) {
        String normalizedKey = StorageFileSupport.normalizeKey(objectKey);
        try {
            var response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(normalizedKey)
                    .build());
            return new StoredObject(response.asByteArray(), response.response().contentType(), response.response().eTag());
        } catch (NoSuchKeyException e) {
            throw new StoredObjectNotFoundException(normalizedKey, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new StoredObjectNotFoundException(normalizedKey, e);
            }
            throw new IllegalStateException("S3 이미지를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        String normalizedKey = StorageFileSupport.normalizeKey(objectKey);
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(normalizedKey).build());
    }
}
