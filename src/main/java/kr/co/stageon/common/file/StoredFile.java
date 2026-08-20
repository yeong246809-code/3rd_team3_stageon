package kr.co.stageon.common.file;

/** 객체 저장소에 기록된 파일의 영속 식별자와 브라우저 접근 URL입니다. */
public record StoredFile(String objectKey, String publicUrl) {
}
