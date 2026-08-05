package kr.co.stageon.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 회차 추가 모달의 "공연장 · 홀" 선택 옵션입니다. */
@Getter
@AllArgsConstructor
public class HallOptionDto {
    private final Long id;
    private final String label;
}