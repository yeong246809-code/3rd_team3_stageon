package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.AdminOrderDetailDto;
import kr.co.stageon.admin.dto.AdminOrderListItemDto;
import kr.co.stageon.admin.dto.AdminOrderSearchCondition;
import kr.co.stageon.admin.dto.AdminOrderStatsDto;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.ReservationSeat;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.domain.Refund;
import kr.co.stageon.payment.repository.PaymentRepository;
import kr.co.stageon.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/** AD09 "예매·주문 조회" 화면의 검색, 통계, 상세, 강제취소, 재예매를 담당합니다. */
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    /** 목록 검색입니다. 각 행의 좌석수·결제상태는 예매 ID 목록으로 일괄 조회해 채웁니다. */
    @Transactional(readOnly = true)
    public Page<AdminOrderListItemDto> search(AdminOrderSearchCondition condition, Pageable pageable) {
        Page<Reservation> page = reservationRepository.search(
                condition.performanceId(),
                condition.scheduleId(),
                condition.status(),
                condition.paymentStatus(),
                (condition.keyword() == null || condition.keyword().isBlank()) ? null : condition.keyword(),
                condition.fromDate(),
                condition.toDate(),
                pageable
        );

        List<Long> reservationIds = page.getContent().stream().map(Reservation::getId).toList();

        Map<Long, Integer> seatCountMap = reservationIds.isEmpty() ? Map.of()
                : reservationSeatRepository.findByReservationIdInOrderByIdAsc(reservationIds).stream()
                .collect(Collectors.groupingBy(rs -> rs.getReservation().getId(), Collectors.summingInt(rs -> 1)));

        Map<Long, Payment> latestPaymentMap = reservationIds.isEmpty() ? Map.of()
                : paymentRepository.findByReservationIdInOrderByRequestedAtDesc(reservationIds).stream()
                .collect(Collectors.toMap(p -> p.getReservation().getId(), p -> p, (a, b) -> a));

        return page.map(r -> new AdminOrderListItemDto(
                r.getId(),
                r.getBookingNumber(),
                maskName(r.getMember().getName()),
                r.getSchedule().getPerformance().getTitle(),
                r.getSchedule().getStartsAt(),
                seatCountMap.getOrDefault(r.getId(), 0),
                r.getTotalAmount(),
                latestPaymentMap.containsKey(r.getId()) ? latestPaymentMap.get(r.getId()).getStatus() : null,
                r.getStatus(),
                r.getCreatedAt()
        ));
    }

    /** 상단 통계 카드입니다. */
    @Transactional(readOnly = true)
    public AdminOrderStatsDto getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayCount = reservationRepository.countByCreatedAtBetween(todayStart, todayEnd);
        BigDecimal totalRevenue = paymentRepository.sumSuccessAmount();
        long cancelledCount = reservationRepository.countByStatus(Reservation.Status.CANCELLED);
        return new AdminOrderStatsDto(todayCount, totalRevenue, cancelledCount);
    }

    /** 예매 상세(좌석 목록, 결제·환불 이력)입니다. */
    @Transactional(readOnly = true)
    public AdminOrderDetailDto getDetail(Long reservationId) {
        Reservation r = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        List<ReservationSeat> seats = reservationSeatRepository.findByReservationIdOrderByIdAsc(reservationId);
        List<Payment> payments = paymentRepository.findByReservationIdOrderByRequestedAtDesc(reservationId);

        List<Long> paymentIds = payments.stream().map(Payment::getId).toList();
        Map<Long, List<Refund>> refundMap = paymentIds.isEmpty() ? Map.of()
                : refundRepository.findByPaymentIdInOrderByRequestedAtDesc(paymentIds).stream()
                .collect(Collectors.groupingBy(rf -> rf.getPayment().getId()));

        List<AdminOrderDetailDto.SeatItem> seatItems = seats.stream()
                .map(s -> new AdminOrderDetailDto.SeatItem(
                        s.getCapturedSectionName(), s.getCapturedRowLabel(), s.getCapturedSeatNumber(),
                        s.getCapturedGradeName(), s.getCapturedUnitPrice()))
                .toList();

        List<AdminOrderDetailDto.PaymentItem> paymentItems = payments.stream()
                .map(p -> new AdminOrderDetailDto.PaymentItem(
                        p.getId(), p.getProvider(), p.getPayMethod(), p.getAmount(), p.getCancelAmount(),
                        p.getStatus(), p.getRequestedAt(), p.getProcessedAt(),
                        refundMap.getOrDefault(p.getId(), List.of()).stream()
                                .map(rf -> new AdminOrderDetailDto.RefundItem(
                                        rf.getAmount(), rf.getStatus(), rf.getRefundCategory(), rf.getReason(), rf.getProcessedAt()))
                                .toList()
                ))
                .toList();

        return new AdminOrderDetailDto(
                r.getId(), r.getBookingNumber(),
                maskName(r.getMember().getName()), maskPhone(r.getMember().getPhone()),
                r.getSchedule().getPerformance().getTitle(), r.getSchedule().getStartsAt(),
                r.getReceiveMethod(), r.getStatus(),
                r.getSeatAmount(), r.getFeeAmount(), r.getDiscountAmount(), r.getTotalAmount(),
                r.getReservedAt(), r.getCancelledAt(), r.getCancelReason(),
                seatItems, paymentItems
        );
    }

    /** 관리자 강제취소입니다. RESERVED 상태만 취소 가능하며, 좌석을 반납하고 성공 결제가 있으면 환불 이력을 남깁니다. */
    @Transactional
    public void cancelReservation(Long reservationId, String reason) {
        Reservation r = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (r.getStatus() != Reservation.Status.RESERVED) {
            throw new IllegalStateException("확정된(RESERVED) 예매만 강제취소할 수 있습니다.");
        }

        List<ReservationSeat> seats = reservationSeatRepository.findByReservationIdOrderByIdAsc(reservationId);
        for (ReservationSeat rs : seats) {
            ScheduleSeat scheduleSeat = rs.getScheduleSeat();
            if (scheduleSeat.getStatus() == ScheduleSeat.Status.RESERVED || scheduleSeat.getStatus() == ScheduleSeat.Status.HELD) {
                scheduleSeat.release();
            }
        }

        paymentRepository.findFirstByReservationIdAndStatusOrderByRequestedAtDesc(reservationId, Payment.Status.SUCCESS)
                .ifPresent(payment -> {
                    payment.markCancelled(payment.getAmount());
                    Refund refund = Refund.createCompleted(payment, payment.getAmount(), Refund.Category.ADMIN_CANCEL, reason);
                    refundRepository.save(refund);
                });

        r.cancel(reason);
    }

    /**
     * 관리자 재예매입니다. CANCELLED 상태의 예매를 동일 회원·동일 좌석으로 새로 생성합니다.
     * 좌석이 그사이 다른 예매로 판매되어 AVAILABLE 상태가 아니면 실패하며, 이 경우 트랜잭션이
     * 롤백되어 앞서 처리된 좌석 상태도 원복됩니다.
     * <p>
     * 실제 PG 결제 승인을 다시 받지 않으므로, 새 결제 이력은 관리자 처리임을 구분할 수 있도록
     * paymentKey에 "ADMIN-REBOOK-" 접두사를 붙여 SUCCESS 상태로 기록합니다.
     */
    @Transactional
    public Long rebookReservation(Long reservationId) {
        Reservation old = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (old.getStatus() != Reservation.Status.CANCELLED) {
            throw new IllegalStateException("취소된(CANCELLED) 예매만 재예매할 수 있습니다.");
        }

        List<ReservationSeat> oldSeats = reservationSeatRepository.findByReservationIdOrderByIdAsc(reservationId);
        if (oldSeats.isEmpty()) {
            throw new IllegalStateException("좌석 정보가 없어 재예매할 수 없습니다.");
        }

        // 1. 좌석이 지금도 비어있는지(AVAILABLE) 확인하면서 순서대로 선점 -> 확정 처리합니다.
        //    하나라도 이미 판매된 좌석이 있으면 예외를 던져 트랜잭션 전체를 롤백합니다.
        for (ReservationSeat rs : oldSeats) {
            ScheduleSeat scheduleSeat = rs.getScheduleSeat();
            try {
                scheduleSeat.hold();
                scheduleSeat.reserve();
            } catch (IllegalStateException e) {
                throw new IllegalStateException(
                        "이미 다른 예매로 판매된 좌석이 있어 재예매할 수 없습니다. (" +
                                rs.getCapturedSectionName() + " " + rs.getCapturedRowLabel() + rs.getCapturedSeatNumber() + ")");
            }
        }

        // 2. 새 예매를 생성합니다. 관리자 재예매이므로 실제 SeatHold 절차 없이 바로 확정 상태로 생성합니다.
        String newBookingNumber = generateAdminBookingNumber();
        Reservation newReservation = Reservation.create(
                newBookingNumber,
                old.getMember(),
                old.getSchedule(),
                null,
                old.getReceiveMethod(),
                old.getSeatAmount(),
                old.getTotalAmount()
        );
        reservationRepository.save(newReservation);

        // 3. 좌석 스냅샷을 그대로 승계합니다.
        for (ReservationSeat rs : oldSeats) {
            reservationSeatRepository.save(ReservationSeat.create(newReservation, rs.getScheduleSeat()));
        }

        // 4. 결제수단은 기존 이력에서 가져오고, 관리자 처리 이력임을 알 수 있도록 SUCCESS 결제를 새로 남깁니다.
        Payment.PayMethod payMethod = paymentRepository.findByReservationIdOrderByRequestedAtDesc(reservationId)
                .stream().findFirst().map(Payment::getPayMethod).orElse(Payment.PayMethod.CARD);

        Payment newPayment = Payment.builder()
                .reservation(newReservation)
                .paymentKey("ADMIN-REBOOK-" + newBookingNumber)
                .orderId(newBookingNumber)
                .provider(Payment.Provider.TOSSPAYMENTS)
                .payMethod(payMethod)
                .amount(old.getTotalAmount())
                .status(Payment.Status.SUCCESS)
                .requestedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        paymentRepository.save(newPayment);

        return newReservation.getId();
    }

    /** 관리자 재예매용 예매번호를 생성합니다. 일반 고객 예매(PG orderId)와 구분되도록 STG-RB- 접두사를 붙입니다. */
    private String generateAdminBookingNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = new Random().nextInt(9000) + 1000;
        return "STG-RB-" + datePart + rand;
    }

    /** 회원명 마스킹: 첫 글자만 남기고 나머지는 '*'로 표시합니다. */
    private String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /** 전화번호 마스킹: 뒷자리 4자리를 '*'로 표시합니다. */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, phone.length() - 4) + "****";
    }
}