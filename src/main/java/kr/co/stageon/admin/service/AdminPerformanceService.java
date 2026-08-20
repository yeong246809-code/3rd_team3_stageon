package kr.co.stageon.admin.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import kr.co.stageon.admin.dto.PerformanceFormDto;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import kr.co.stageon.common.file.FileStorageService;
import kr.co.stageon.common.file.StorageTransactionCleanup;
import kr.co.stageon.common.file.StoredFile;
import kr.co.stageon.venue.domain.VenueHall;
import kr.co.stageon.venue.repository.VenueHallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPerformanceService {

    private final PerformanceRepository performanceRepository;
    private final VenueHallRepository venueHallRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private final StorageTransactionCleanup storageCleanup;

    /** 예전 방식(전체 조회)이 필요한 곳이 있을 수 있어 남겨둡니다. */
    @Transactional(readOnly = true)
    public List<Performance> getList() {
        return performanceRepository.findAll();
    }

    /**
     * 관리자 공연 목록을 페이지 단위로 조회합니다.
     * @param page 0부터 시작하는 페이지 번호
     * @param size 한 페이지에 보여줄 개수
     */
    @Transactional(readOnly = true)
    public Page<Performance> getList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return performanceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public PerformanceFormDto getForm(Long id) {
        Performance p = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다. id=" + id));
        PerformanceFormDto dto = PerformanceFormDto.from(p);
        dto.fillSeatPrices(parseSeatPriceJson(p.getRawPriceText()));
        return dto;
    }

    /** draft=true면 임시저장(저장 상태 유지, 목록에 "임시저장됨" 표시), false면 정식 저장입니다. */
    @Transactional
    public Long create(PerformanceFormDto dto, boolean draft) {
        VenueHall hall = resolveHall(dto.getHallId());
        if (hall != null) {
            checkHallConflict(hall.getId(), dto.getStartDate(), dto.getEndDate(), null);
        }

        StoredFile uploaded = fileStorageService.savePoster(dto.getPosterFile());
        if (uploaded != null) {
            storageCleanup.deleteOnRollback(uploaded.objectKey());
        }
        String posterUrl = uploaded != null ? uploaded.publicUrl() : dto.getPosterUrl();
        String posterKey = uploaded != null ? uploaded.objectKey() : null;

        Performance p = Performance.create(
                dto.getKopisId(), dto.getTitle(), dto.getGenre(), posterUrl, posterKey,
                dto.getStartDate(), dto.getEndDate(), dto.getRuntimeMinutes(),
                dto.getAgeText(), dto.getStory(), toStatus(dto.getStatus()), draft,
                hall, dto.getBasePrice()
        );
        p.updateSeatPriceJson(toSeatPriceJson(dto));
        return performanceRepository.save(p).getId();
    }

    @Transactional
    public void update(Long id, PerformanceFormDto dto, boolean draft) {
        Performance p = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다. id=" + id));

        VenueHall hall = resolveHall(dto.getHallId());
        if (hall != null) {
            checkHallConflict(hall.getId(), dto.getStartDate(), dto.getEndDate(), id);
        }

        StoredFile uploaded = fileStorageService.savePoster(dto.getPosterFile());
        String posterUrl = p.getPosterUrl();
        String posterKey = p.getPosterKey();
        if (uploaded != null) {
            storageCleanup.deleteOnRollback(uploaded.objectKey());
            storageCleanup.deleteAfterCommit(p.getPosterKey());
            posterUrl = uploaded.publicUrl();
            posterKey = uploaded.objectKey();
        }

        p.update(
                dto.getKopisId(), dto.getTitle(), dto.getGenre(), posterUrl, posterKey,
                dto.getStartDate(), dto.getEndDate(), dto.getRuntimeMinutes(),
                dto.getAgeText(), dto.getStory(), toStatus(dto.getStatus()), draft,
                hall, dto.getBasePrice()
        );
        p.updateSeatPriceJson(toSeatPriceJson(dto));
    }

    private VenueHall resolveHall(Long hallId) {
        if (hallId == null) {
            return null;
        }
        return venueHallRepository.findById(hallId)
                .orElseThrow(() -> new IllegalArgumentException("공연장(홀)을 찾을 수 없습니다. id=" + hallId));
    }

    /**
     * 동일 홀 + 기간이 겹치는 다른 공연(회차 아님, Performance 자체)이 있는지 검사합니다.
     * 같은 Performance ID(수정 시 자기 자신)는 제외합니다.
     */
    private void checkHallConflict(Long hallId, LocalDate startDate, LocalDate endDate, Long excludePerformanceId) {
        List<Performance> overlapping = performanceRepository
                .findByVenueHallIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(hallId, endDate, startDate);
        boolean conflict = overlapping.stream()
                .anyMatch(other -> !other.getId().equals(excludePerformanceId));
        if (conflict) {
            throw new IllegalStateException("해당 공연장은 같은 기간에 이미 다른 공연이 등록되어 있습니다.");
        }
    }

    /** 등급별 가격 목록을 JSON 문자열로 직렬화합니다. 새 테이블 없이 performances.raw_price_text에 저장합니다. */
    private String toSeatPriceJson(PerformanceFormDto dto) {
        try {
            return objectMapper.writeValueAsString(dto.getSeatPrices());
        } catch (JacksonException e) {
            throw new IllegalStateException("좌석 가격 정보를 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    private List<PerformanceFormDto.SeatPriceItem> parseSeatPriceJson(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PerformanceFormDto.SeatPriceItem>>() {});
        } catch (JacksonException e) {
            return new ArrayList<>();
        }
    }

    private Performance.Status toStatus(String status) {
        return (status == null || status.isBlank())
                ? Performance.Status.UPCOMING
                : Performance.Status.valueOf(status);
    }
}
