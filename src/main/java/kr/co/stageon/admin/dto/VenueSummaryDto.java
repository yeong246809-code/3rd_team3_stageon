package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** 공연장·좌석 관리 목록 화면용 DTO입니다. */
@Getter
@RequiredArgsConstructor
public class VenueSummaryDto {
    private final Long id;
    private final String name;
    private final String address;
    private final String region;
    private final List<HallSummary> halls;

    @Getter
    @RequiredArgsConstructor
    public static class HallSummary {
        private final Long id;
        private final String name;
        private final Integer seatCapacity;
    }
}