package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 좌석 등급(예: VIP/R/S) 1개 등록 폼 바인딩용 DTO입니다.
 * 등급명은 프리셋 목록(presetName)에서 고르거나, 목록에 없으면 customName에 직접 입력합니다.
 * customName이 입력되어 있으면 그 값이 우선 사용됩니다.
 */
@Getter
@Setter
public class SeatGradeFormDto {
    private String presetName;
    private String customName;
    private String displayColor;
    private Integer sortOrder;

    /** 실제로 저장에 사용할 등급명입니다. */
    public String resolvedName() {
        if (customName != null && !customName.isBlank()) {
            return customName.trim();
        }
        return presetName;
    }
}