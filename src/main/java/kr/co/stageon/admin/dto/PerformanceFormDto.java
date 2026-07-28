package kr.co.stageon.admin.dto;

import kr.co.stageon.performance.domain.Performance;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/** 공연 등록·수정 폼 바인딩용 DTO입니다. */
@Getter
@Setter
public class PerformanceFormDto {
    private Long id;
    private String kopisId;
    private String title;
    private String genre;
    private String posterUrl;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer runtimeMinutes;
    private String ageText;
    private String story;
    private String status;
    private boolean draft;

    /** 폼에서 새로 업로드한 포스터 파일입니다. DB에는 저장되지 않고 posterUrl로 변환되어 사용됩니다. */
    private MultipartFile posterFile;

    public static PerformanceFormDto from(Performance p) {
        PerformanceFormDto dto = new PerformanceFormDto();
        dto.id = p.getId();
        dto.kopisId = p.getKopisId();
        dto.title = p.getTitle();
        dto.genre = p.getGenre();
        dto.posterUrl = p.getPosterUrl();
        dto.startDate = p.getStartDate();
        dto.endDate = p.getEndDate();
        dto.runtimeMinutes = p.getRuntimeMinutes();
        dto.ageText = p.getAgeText();
        dto.story = p.getStory();
        dto.status = p.getStatus().name();
        dto.draft = p.isDraft();
        return dto;
    }
}