package kr.co.stageon.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import kr.co.stageon.performance.domain.Performance;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 관리자 공연 등록·수정 화면의 입력 DTO입니다. */
public record AdminPerformanceRequest(
        @Size(max = 50) String kopisId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 50) String genre,
        @Size(max = 500) String posterUrl,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @PositiveOrZero Integer runtimeMinutes,
        @Size(max = 100) String ageText,
        String story,
        String rawPriceText,
        String rawScheduleText,
        @NotNull Performance.Status status,
        LocalDateTime kopisUpdatedAt
) {
}
