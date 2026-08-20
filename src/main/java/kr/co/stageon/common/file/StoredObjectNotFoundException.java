package kr.co.stageon.common.file;

public class StoredObjectNotFoundException extends RuntimeException {
    public StoredObjectNotFoundException(String objectKey, Throwable cause) {
        super("저장된 파일을 찾을 수 없습니다: " + objectKey, cause);
    }

    public StoredObjectNotFoundException(String objectKey) {
        super("저장된 파일을 찾을 수 없습니다: " + objectKey);
    }
}
