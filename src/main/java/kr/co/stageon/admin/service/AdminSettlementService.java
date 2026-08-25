package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.AdminSettlementDetailDto;
import kr.co.stageon.admin.dto.AdminSettlementListItemDto;
import kr.co.stageon.admin.dto.AdminSettlementSearchCondition;
import kr.co.stageon.admin.dto.AdminSettlementStatsDto;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.payment.repository.PaymentRepository;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** AD "정산·매출 관리" 화면의 통계, 공연별 목록, 회차별·예매자 상세를 담당합니다. */
@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final PaymentRepository paymentRepository;
    private final PerformanceRepository performanceRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    /** 상단 통계 카드입니다. */
    @Transactional(readOnly = true)
    public AdminSettlementStatsDto getStats(AdminSettlementSearchCondition condition) {
        BigDecimal gross = paymentRepository.sumGrossAmount(condition.fromDate(), condition.toDate());
        BigDecimal refund = paymentRepository.sumRefundAmount(condition.fromDate(), condition.toDate());
        long count = paymentRepository.countSuccess(condition.fromDate(), condition.toDate());
        return new AdminSettlementStatsDto(gross, refund, gross.subtract(refund), count);
    }

    /** 공연별 매출 목록(검색·기간·페이지네이션)입니다. */
    @Transactional(readOnly = true)
    public Page<AdminSettlementListItemDto> search(AdminSettlementSearchCondition condition, Pageable pageable) {
        String keyword = (condition.keyword() == null || condition.keyword().isBlank()) ? null : condition.keyword();

        Page<PaymentRepository.PerformanceRevenueProjection> page =
                paymentRepository.searchPerformanceRevenue(keyword, condition.fromDate(), condition.toDate(), pageable);

        return page.map(p -> new AdminSettlementListItemDto(
                p.getPerformanceId(),
                p.getTitle(),
                p.getPaymentCount(),
                p.getGrossAmount(),
                p.getRefundAmount(),
                p.getGrossAmount().subtract(p.getRefundAmount())
        ));
    }

    /** 공연 1건의 회차별 매출 breakdown과 예매자 목록(상세 모달)입니다. */
    @Transactional(readOnly = true)
    public AdminSettlementDetailDto getDetail(Long performanceId, AdminSettlementSearchCondition condition) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다."));

        List<PaymentRepository.ScheduleRevenueProjection> rows =
                paymentRepository.findScheduleRevenueByPerformance(performanceId, condition.fromDate(), condition.toDate());

        List<AdminSettlementDetailDto.ScheduleItem> items = rows.stream()
                .map(r -> new AdminSettlementDetailDto.ScheduleItem(
                        r.getScheduleId(),
                        r.getStartsAt(),
                        r.getPaymentCount(),
                        r.getGrossAmount(),
                        r.getRefundAmount(),
                        r.getGrossAmount().subtract(r.getRefundAmount())
                ))
                .toList();

        BigDecimal totalGross = items.stream()
                .map(AdminSettlementDetailDto.ScheduleItem::grossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRefund = items.stream()
                .map(AdminSettlementDetailDto.ScheduleItem::refundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AdminSettlementDetailDto.BookerItem> bookers = getBookers(performanceId, condition);

        return new AdminSettlementDetailDto(
                performanceId,
                performance.getTitle(),
                totalGross,
                totalRefund,
                totalGross.subtract(totalRefund),
                items,
                bookers
        );
    }

    /** 해당 공연을 예매 확정(RESERVED)한 회원 목록입니다. AD09와 동일하게 이름을 마스킹합니다. */
    private List<AdminSettlementDetailDto.BookerItem> getBookers(Long performanceId, AdminSettlementSearchCondition condition) {
        List<Reservation> reservations = reservationRepository.search(
                performanceId, null, Reservation.Status.RESERVED, null, null,
                condition.fromDate(), condition.toDate(), Pageable.unpaged()
        ).getContent();

        List<Long> reservationIds = reservations.stream().map(Reservation::getId).toList();

        Map<Long, Integer> seatCountMap = reservationIds.isEmpty() ? Map.of()
                : reservationSeatRepository.findByReservationIdInOrderByIdAsc(reservationIds).stream()
                .collect(Collectors.groupingBy(rs -> rs.getReservation().getId(), Collectors.summingInt(rs -> 1)));

        return reservations.stream()
                .sorted(Comparator.comparing(Reservation::getReservedAt).reversed())
                .map(r -> new AdminSettlementDetailDto.BookerItem(
                        r.getBookingNumber(),
                        maskName(r.getMember().getName()),
                        seatCountMap.getOrDefault(r.getId(), 0),
                        r.getTotalAmount(),
                        r.getReservedAt()
                ))
                .toList();
    }

    /** 회원명 마스킹: 첫 글자만 남기고 나머지는 '*'로 표시합니다. (AD09 AdminOrderService와 동일한 규칙) */
    private String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}