package kr.co.stageon.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** 비공개 S3 객체를 같은 도메인의 /media/* URL로 전달합니다. */
@RestController
@RequiredArgsConstructor
public class MediaController {
    private final ObjectStorageService storageService;

    @GetMapping("/media/{*objectKey}")
    public ResponseEntity<byte[]> get(@PathVariable String objectKey) {
        try {
            StoredObject object = storageService.load(objectKey);
            ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                    .contentLength(object.content().length)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

            if (object.contentType() != null && !object.contentType().isBlank()) {
                response.contentType(MediaType.parseMediaType(object.contentType()));
            } else {
                response.contentType(MediaType.APPLICATION_OCTET_STREAM);
            }
            if (object.eTag() != null && !object.eTag().isBlank()) {
                response.header(HttpHeaders.ETAG, object.eTag());
            }
            return response.body(object.content());
        } catch (StoredObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
