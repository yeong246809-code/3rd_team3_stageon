package kr.co.stageon.common.file;

/** 객체 저장소에서 읽어 온 응답 본문과 HTTP 메타데이터입니다. */
public record StoredObject(byte[] content, String contentType, String eTag) {
}
