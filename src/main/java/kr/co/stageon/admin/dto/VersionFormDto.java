package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 버전 등록·수정 폼 바인딩 DTO입니다. */
@Getter
@Setter
public class VersionFormDto {

    private Long id;
    private String version;
    private String category;
    private String title;
    private String description;
    private String author;
    private LocalDate releasedAt;
}