package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.SeatBulkFormDto;
import kr.co.stageon.admin.dto.SeatGradeFormDto;
import kr.co.stageon.admin.dto.SeatGradeSummaryDto;
import kr.co.stageon.admin.dto.SeatMapItemDto;
import kr.co.stageon.admin.dto.VenueDashboardDto;
import kr.co.stageon.admin.dto.VenueFormDto;
import kr.co.stageon.admin.dto.VenueHallFormDto;
import kr.co.stageon.admin.dto.VenueStructureRowDto;
import kr.co.stageon.venue.domain.Seat;
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
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final String DEFAULT_GRADE_COLOR = "#6B7280";

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
        return toGradeSummaries(chart.getId());
    }

    private List<SeatGradeSummaryDto> toGradeSummaries(Long seatChartId) {
        List<SeatGrade> grades = seatGradeRepository.findBySeatChartIdOrderBySortOrderAsc(seatChartId);
        return grades.stream()
                .map(g -> new SeatGradeSummaryDto(
                        g.getId(),
                        g.getName(),
                        g.getDisplayColor(),
                        seatRepository.countBySeatGradeIdAndActiveTrue(g.getId())
                ))
                .collect(Collectors.toList());
    }

    /** 선택된 홀의 활성 좌석도 기준 개별 좌석 목록입니다. 공연 등록 화면의 좌석 배치도 미리보기용입니다. */
    @Transactional(readOnly = true)
    public List<SeatMapItemDto> getHallSeatMap(Long hallId) {
        if (hallId == null) {
            return Collections.emptyList();
        }
        SeatChart chart = seatChartRepository
                .findFirstByVenueHallIdAndActiveTrueOrderByVersionDesc(hallId)
                .orElse(null);
        if (chart == null) {
            return Collections.emptyList();
        }
        List<Seat> seats = seatRepository
                .findBySeatChartIdOrderBySectionNameAscRowLabelAscSeatNumberAsc(chart.getId());
        return seats.stream()
                .map(s -> new SeatMapItemDto(
                        s.getId(), s.getSectionName(), s.getRowLabel(), s.getSeatNumber(),
                        s.getSeatGrade().getId(), s.getSeatGrade().getName(), s.getSeatGrade().getDisplayColor()
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
    public VenueFormDto getVenueForm(Long venueId) {
        return VenueFormDto.from(getVenueOrThrow(venueId));
    }

    @Transactional
    public void updateVenue(Long venueId, VenueFormDto dto) {
        Venue v = getVenueOrThrow(venueId);
        v.update(
                dto.getKopisFacilityId(), dto.getName(), dto.getAddress(), dto.getRegion(),
                null, null, dto.getPhone(), dto.getHomepageUrl()
        );
    }

    /**
     * 공연장을 삭제합니다. 연결된 홀·좌석도·좌석등급·좌석까지 전부 함께 삭제됩니다(연쇄 삭제).
     * 되돌릴 수 없으므로 화면에서 반드시 확인창을 거치도록 되어 있습니다.
     */
    @Transactional
    public void deleteVenue(Long venueId) {
        List<VenueHall> halls = venueHallRepository.findByVenueIdAndActiveTrueOrderByNameAsc(venueId);
        for (VenueHall hall : halls) {
            deleteHallCascade(hall);
        }
        venueRepository.deleteById(venueId);
    }

    /** 홀 하나만 삭제합니다. 연결된 좌석도·등급·좌석까지 함께 삭제됩니다(연쇄 삭제). 공연장 자체는 남습니다. */
    @Transactional
    public void deleteHall(Long hallId) {
        VenueHall hall = getHallOrThrow(hallId);
        deleteHallCascade(hall);
    }

    private void deleteHallCascade(VenueHall hall) {
        List<SeatChart> charts = seatChartRepository.findByVenueHallIdOrderByVersionDesc(hall.getId());
        for (SeatChart chart : charts) {
            List<Seat> seats = seatRepository.findBySeatChartIdOrderBySectionNameAscRowLabelAscSeatNumberAsc(chart.getId());
            seatRepository.deleteAll(seats);

            List<SeatGrade> grades = seatGradeRepository.findBySeatChartIdOrderBySortOrderAsc(chart.getId());
            seatGradeRepository.deleteAll(grades);

            seatChartRepository.delete(chart);
        }
        venueHallRepository.delete(hall);
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
     * displayColor를 선택하지 않은 경우 기본 회색(#6B7280)으로 저장됩니다.
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

        String color = (dto.getDisplayColor() != null && !dto.getDisplayColor().isBlank())
                ? dto.getDisplayColor() : DEFAULT_GRADE_COLOR;

        SeatGrade grade = SeatGrade.create(chart, dto.resolvedName(), color, sortOrder);
        return seatGradeRepository.save(grade).getId();
    }

    /** 해당 등급에 연결된 좌석이 없을 때만 삭제합니다(연결된 좌석 보호). */
    @Transactional
    public void deleteGrade(Long gradeId) {
        long seatCount = seatRepository.countBySeatGradeIdAndActiveTrue(gradeId);
        if (seatCount > 0) {
            throw new IllegalStateException("이미 배치된 좌석이 있는 등급은 삭제할 수 없습니다. 좌석을 먼저 정리해주세요.");
        }
        seatGradeRepository.deleteById(gradeId);
    }

    /**
     * 구역 단위로 좌석을 한 번에 생성합니다(예: A구역 10열 x 20석 = 200석).
     * 홀에 좌석도·등급이 먼저 등록되어 있어야 합니다.
     * 이미 동일한 구역·열·번호 좌석이 있으면 예외가 발생합니다(중복 방지).
     *
     * @return 생성된 좌석 수
     */
    @Transactional
    public int bulkCreateSeats(Long hallId, SeatBulkFormDto dto) {
        SeatChart chart = seatChartRepository
                .findFirstByVenueHallIdAndActiveTrueOrderByVersionDesc(hallId)
                .orElseThrow(() -> new IllegalStateException("좌석도가 없습니다. 좌석 등급을 먼저 등록해주세요."));

        SeatGrade grade = seatGradeRepository.findById(dto.getSeatGradeId())
                .orElseThrow(() -> new IllegalArgumentException("등급을 찾을 수 없습니다. id=" + dto.getSeatGradeId()));

        int startRow = (dto.getStartRowNumber() != null) ? dto.getStartRowNumber() : 1;
        int startSeat = (dto.getStartSeatNumber() != null) ? dto.getStartSeatNumber() : 1;

        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < dto.getRowCount(); r++) {
            String rowLabel = String.valueOf(startRow + r);
            for (int n = 0; n < dto.getSeatsPerRow(); n++) {
                String seatNumber = String.valueOf(startSeat + n);
                seats.add(Seat.create(
                        chart, grade, Seat.ObjectType.SEAT,
                        dto.getSectionName(), rowLabel, seatNumber,
                        1, false, false
                ));
            }
        }

        try {
            seatRepository.saveAll(seats);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 동일한 구역·열·번호의 좌석이 존재합니다. 입력값을 확인해주세요.");
        }
        return seats.size();
    }
}