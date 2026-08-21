package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.AdminSettlementDetailDto;
import kr.co.stageon.admin.dto.AdminSettlementListItemDto;
import kr.co.stageon.admin.dto.AdminSettlementSearchCondition;
import kr.co.stageon.admin.dto.AdminSettlementStatsDto;
import kr.co.stageon.payment.repository.PaymentRepository;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** AD "정산·매출 관리" 화면의 통계, 공연별 목록, 회차별 상세를 담당합니다. */
@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final PaymentRepository paymentRepository;
    private final PerformanceRepository performanceRepository;

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

    /** 공연 1건의 회차별 매출 breakdown(상세 모달)입니다. */
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

        return new AdminSettlementDetailDto(
                performanceId,
                performance.getTitle(),
                totalGross,
                totalRefund,
                totalGross.subtract(totalRefund),
                items
        );
    }
}