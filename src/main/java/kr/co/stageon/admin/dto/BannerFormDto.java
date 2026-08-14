package kr.co.stageon.admin.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

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
    private String periodStartText;
    private String periodEndText;
    private String badgeText;
    private String button1Text;
    private String button1Url;
    private String button2Text;
    private String button2Url;
    private boolean active = true;
}