package kr.co.stageon.admin.dto;

/** AD08 좌석 일괄 삭제 결과입니다. 선점 이력이 있어 삭제가 거부된 건은 skipped로 집계됩니다. */
public record SeatInventoryDeleteResultDto(
        int deleted,
        int skipped
) {
}