package kr.co.stageon.admin.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/** 배너 등록·수정 폼 바인딩 DTO입니다. */
@Data
public class BannerFormDto {

    private Long id;
    private String title;
    private String description;

    /** 업로드된 새 이미지 파일입니다. 없으면 기존 imageUrl을 유지합니다. */
    private MultipartFile bannerFile;

    /** 화면에 미리보기로 표시되는 현재 이미지 URL(hidden input)입니다. */
    private String imageUrl;

    private Long performanceId;
    private String linkUrl;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodEnd;

    private String badgeText;
    private String button1Text;

    /** "AUTO"(연결 공연 자동 링크), "CUSTOM"(직접 입력), 그 외에는 사전 정의된 페이지 경로입니다. */
    private String button1LinkType = "AUTO";
    private String button1Url;

    private String button2Text;
    private String button2LinkType = "AUTO";
    private String button2Url;

    private boolean active = true;
}