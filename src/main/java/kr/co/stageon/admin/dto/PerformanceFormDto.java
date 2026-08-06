package kr.co.stageon.admin.dto;

import kr.co.stageon.performance.domain.Performance;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    /** 선택된 공연장 홀 ID입니다. 공연장 중복 예약 검증에 사용됩니다. */
    private Long hallId;

    /** 공연 기본 가격입니다. */
    private Integer basePrice;

    /** 모달에서 설정한 좌석 등급별 가격 목록입니다. (hidden input으로 전송) */
    private List<SeatPriceItem> seatPrices = new ArrayList<>();

    /** 폼에서 새로 업로드한 포스터 파일입니다. DB에는 저장되지 않고 posterUrl로 변환되어 사용됩니다. */
    private MultipartFile posterFile;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SeatPriceItem {
        private Long gradeId;
        private Integer price;
    }

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
        dto.basePrice = p.getBasePrice();
        dto.hallId = (p.getVenueHall() != null) ? p.getVenueHall().getId() : null;
        return dto;
    }

    /** 수정 화면에서 기존 등급별 가격을 미리 채울 때 사용합니다(서비스에서 JSON 파싱 후 호출). */
    public void fillSeatPrices(List<SeatPriceItem> items) {
        this.seatPrices = (items != null) ? items : new ArrayList<>();
    }
}