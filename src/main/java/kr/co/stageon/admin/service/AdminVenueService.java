package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.SeatGradeFormDto;
import kr.co.stageon.admin.dto.SeatGradeSummaryDto;
import kr.co.stageon.admin.dto.VenueDashboardDto;
import kr.co.stageon.admin.dto.VenueFormDto;
import kr.co.stageon.admin.dto.VenueHallFormDto;
import kr.co.stageon.admin.dto.VenueStructureRowDto;
import kr.co.stageon.venue.domain.SeatChart;
import kr.co.stageon.venue.domain.SeatGrade;
import kr.co.stageon.venue.domain.Venue;
import kr.co.stageon.venue.domain.VenueHall;
import kr.co.stageon.venue.repository.SeatChartRepository;
import kr.co.stageon.venue.repository.SeatGradeRepository;
import kr.co.stageon.venue.repository.SeatRepository;
import kr.co.stageon.venue.repository.VenueHallRepository;
import kr.co.stageon.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** 관리자 공연장·좌석 관리(AD06) 서비스입니다. */
@Service
@RequiredArgsConstructor
public class AdminVenueService {

    private final VenueRepository venueRepository;
    private final VenueHallRepository venueHallRepository;
    private final SeatChartRepository seatChartRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final SeatRepository seatRepository;

    /** 상단 통계 카드 + "공연장 구조" 테이블(공연장·홀·등급 구성·좌석도 상태)용 데이터입니다. */
    @Transactional(readOnly = true)
    public VenueDashboardDto getDashboard() {
        List<Venue> venues = venueRepository.findAll();
        List<VenueStructureRowDto> rows = new ArrayList<>();
        int hallCount = 0;

        for (Venue v : venues) {
            List<VenueHall> halls = venueHallRepository.findByVenueIdAndActiveTrueOrderByNameAsc(v.getId());

            if (halls.isEmpty()) {
                rows.add(new VenueStructureRowDto(v.getId(), v.getName(), null, "홀 미등록", "-", false));
                continue;
            }

            hallCount += halls.size();
            for (VenueHall h : halls) {
                Optional<SeatChart> chartOpt = seatChartRepository
                        .findFirstByVenueHallIdAndActiveTrueOrderByVersionDesc(h.getId());

                String gradeSummary = "미설정";
                boolean configured = false;
                if (chartOpt.isPresent()) {
                    List<SeatGrade> grades = seatGradeRepository
                            .findBySeatChartIdOrderBySortOrderAsc(chartOpt.get().getId());
                    if (!grades.isEmpty()) {
                        gradeSummary = grades.stream().map(SeatGrade::getName).collect(Collectors.joining("·"));
                        configured = true;
                    }
                }
                rows.add(new VenueStructureRowDto(v.getId(), v.getName(), h.getId(), h.getName(), gradeSummary, configured));
            }
        }

        long totalSeatCount = seatRepository.countByActiveTrue();
        return new VenueDashboardDto(venues.size(), hallCount, totalSeatCount, rows);
    }

    /** 선택된 홀의 활성 좌석도 기준 등급·좌석수를 반환합니다. 홀이 없거나 좌석도가 없으면 빈 목록입니다. */
    @Transactional(readOnly = true)
    public List<SeatGradeSummaryDto> getHallGrades(Long hallId) {
        if (hallId == null) {
            return Collections.emptyList();
        }
        SeatChart chart = seatChartRepository
                .findFirstByVenueHallIdAndActiveTrueOrderByVersionDesc(hallId)
                .orElse(null);
        if (chart == null) {
            return Collections.emptyList();
        }
        List<SeatGrade> grades = seatGradeRepository.findBySeatChartIdOrderBySortOrderAsc(chart.getId());
        return grades.stream()
                .map(g -> new SeatGradeSummaryDto(
                        g.getId(),
                        g.getName(),
                        g.getDisplayColor(),
                        seatRepository.countBySeatGradeIdAndActiveTrue(g.getId())
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createVenue(VenueFormDto dto) {
        Venue v = Venue.create(
                dto.getKopisFacilityId(), dto.getName(), dto.getAddress(), dto.getRegion(),
                null, null, dto.getPhone(), dto.getHomepageUrl()
        );
        return venueRepository.save(v).getId();
    }

    @Transactional(readOnly = true)
    public Venue getVenueOrThrow(Long venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("공연장을 찾을 수 없습니다. id=" + venueId));
    }

    @Transactional(readOnly = true)
    public VenueHall getHallOrThrow(Long hallId) {
        return venueHallRepository.findById(hallId)
                .orElseThrow(() -> new IllegalArgumentException("홀을 찾을 수 없습니다. id=" + hallId));
    }

    @Transactional
    public Long createHall(Long venueId, VenueHallFormDto dto) {
        Venue venue = getVenueOrThrow(venueId);
        VenueHall hall = VenueHall.create(
                venue, dto.getKopisHallId(), dto.getName(), dto.getSeatCapacity(), dto.getAccessibleSeatCount()
        );
        return venueHallRepository.save(hall).getId();
    }

    /**
     * 홀에 좌석 등급을 하나 추가합니다. 홀에 활성 좌석도가 없으면 버전1 좌석도를 자동으로 생성합니다.
     * sortOrder를 입력하지 않으면 기존 등급 개수 + 1로 자동 지정됩니다.
     */
    @Transactional
    public Long addGrade(Long hallId, SeatGradeFormDto dto) {
        VenueHall hall = getHallOrThrow(hallId);

        SeatChart chart = seatChartRepository
                .findFirstByVenueHallIdAndActiveTrueOrderByVersionDesc(hallId)
                .orElseGet(() -> seatChartRepository.save(
                        SeatChart.create(hall, hall.getName() + " 기본 좌석도", 1)
                ));

        List<SeatGrade> existing = seatGradeRepository.findBySeatChartIdOrderBySortOrderAsc(chart.getId());
        int sortOrder = (dto.getSortOrder() != null) ? dto.getSortOrder() : existing.size() + 1;

        SeatGrade grade = SeatGrade.create(chart, dto.getName(), dto.getDisplayColor(), sortOrder);
        return seatGradeRepository.save(grade).getId();
    }
}