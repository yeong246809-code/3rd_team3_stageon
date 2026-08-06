package kr.co.stageon.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 회차 일괄 등록 후 생성/건너뜀 결과 요약입니다. */
@Getter
@AllArgsConstructor
public class BulkCreateResultDto {
    private final int createdCount;
    private final int skippedCount;
}