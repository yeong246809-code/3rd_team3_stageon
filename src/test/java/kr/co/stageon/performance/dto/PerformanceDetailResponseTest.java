package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.Performance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceDetailResponseTest {

    @ParameterizedTest
    @CsvSource({
            "150, 2시간 30분",
            "120, 2시간",
            "45, 45분"
    })
    void convertsRuntimeMinutesToReadableText(int runtimeMinutes, String expected) {
        var response = PerformanceDetailResponse.from(performance(runtimeMinutes), null);

        assertThat(response.runtimeText()).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    void leavesRuntimeTextEmptyWhenRuntimeIsMissing(Integer runtimeMinutes) {
        var response = PerformanceDetailResponse.from(performance(runtimeMinutes), null);

        assertThat(response.runtimeText()).isNull();
    }

    private Performance performance(Integer runtimeMinutes) {
        return Performance.create(
                "TEST-1",
                "테스트 공연",
                "뮤지컬",
                null,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                runtimeMinutes,
                null,
                null,
                Performance.Status.ON_SALE,
                false,
                null,
                null
        );
    }
}
