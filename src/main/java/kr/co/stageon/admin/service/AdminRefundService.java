package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.AdminMemberPaymentItemDto;
import kr.co.stageon.admin.dto.AdminMemberSearchItemDto;
import kr.co.stageon.admin.dto.AdminRefundDetailDto;
import kr.co.stageon.admin.dto.AdminRefundListItemDto;
import kr.co.stageon.admin.dto.AdminRefundSearchCondition;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.domain.Refund;
import kr.co.stageon.payment.repository.PaymentRepository;
import kr.co.stageon.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** "환불 관리" 화면의 목록 검색, 상세 조회, 수동 환불 처리(회원·결제 검색 포함)를 담당합니다. */
@Service
@RequiredArgsConstructor
public class AdminRefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    /** 목록 검색입니다. */
    @Transactional(readOnly = true)
    public Page<AdminRefundListItemDto> search(AdminRefundSearchCondition condition, Pageable pageable) {
        Page<Refund> page = refundRepository.search(
                condition.status(),
                condition.category(),
                (condition.keyword() == null || condition.keyword().isBlank()) ? null : condition.keyword(),
                condition.fromDate(),
                condition.toDate(),
                pageable
        );

        return page.map(rf -> {
            Reservation r = rf.getPayment().getReservation();
            return new AdminRefundListItemDto(
                    rf.getId(),
                    r.getBookingNumber(),
                    maskName(r.getMember().getName()),
                    r.getSchedule().getPerformance().getTitle(),
                    rf.getAmount(),
                    rf.getStatus(),
                    rf.getRefundCategory(),
                    rf.getReason(),
                    rf.getRequestedAt(),
                    rf.getProcessedAt()
            );
        });
    }

    /** 상세 모달용 조회입니다. */
    @Transactional(readOnly = true)
    public AdminRefundDetailDto getDetail(Long refundId) {
        Refund rf = refundRepository.findByIdWithDetails(refundId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환불 이력입니다."));

        Payment p = rf.getPayment();
        Reservation r = p.getReservation();

        return new AdminRefundDetailDto(
                rf.getId(),
                r.getBookingNumber(),
                maskName(r.getMember().getName()),
                r.getSchedule().getPerformance().getTitle(),
                r.getSchedule().getStartsAt(),
                p.getId(),
                p.getPayMethod(),
                p.getAmount(),
                p.getCancelAmount(),
                rf.getAmount(),
                rf.getStatus(),
                rf.getRefundCategory(),
                rf.getReason(),
                rf.getPgTid(),
                rf.getRequestedAt(),
                rf.getProcessedAt()
        );
    }

    /** 수동 환불 모달 - 이름/이메일 키워드로 회원을 검색합니다(자동완성용, 최대 10건). */
    @Transactional(readOnly = true)
    public List<AdminMemberSearchItemDto> searchMembers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Page<Member> page = memberRepository.search(null, null, keyword, PageRequest.of(0, 10));
        return page.getContent().stream()
                .map(m -> new AdminMemberSearchItemDto(m.getId(), m.getName(), m.getEmail(), maskPhone(m.getPhone())))
                .toList();
    }

    /** 수동 환불 모달 - 회원 선택 후, 환불 가능액이 남은 SUCCESS 결제 건 목록을 조회합니다. */
    @Transactional(readOnly = true)
    public List<AdminMemberPaymentItemDto> getMemberRefundablePayments(Long memberId) {
        return paymentRepository.findByMemberIdAndStatusOrderByRequestedAtDesc(memberId, Payment.Status.SUCCESS).stream()
                .map(p -> {
                    Reservation r = p.getReservation();
                    BigDecimal refundable = p.getAmount().subtract(p.getCancelAmount());
                    return new AdminMemberPaymentItemDto(
                            p.getId(),
                            r.getBookingNumber(),
                            r.getSchedule().getPerformance().getTitle(),
                            p.getAmount(),
                            p.getCancelAmount(),
                            refundable,
                            p.getRequestedAt()
                    );
                })
                .filter(dto -> dto.refundableAmount().signum() > 0)
                .toList();
    }

    /**
     * 관리자 수동 환불입니다. 예매 취소(강제취소) 없이 결제 건 단위로 부분/전액 환불을 처리합니다.
     * 환불 가능액(결제금액 - 기취소금액)을 초과할 수 없으며, 전액 환불 시 결제 상태를 CANCELED로,
     * 부분 환불 시 결제 상태는 SUCCESS로 유지한 채 취소 누적액만 증가시킵니다.
     */
    @Transactional
    public void manualRefund(Long paymentId, BigDecimal amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다."));

        if (payment.getStatus() != Payment.Status.SUCCESS) {
            throw new IllegalStateException("성공(SUCCESS) 상태의 결제만 환불할 수 있습니다.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }

        BigDecimal refundable = payment.getAmount().subtract(payment.getCancelAmount());
        if (amount.compareTo(refundable) > 0) {
            throw new IllegalArgumentException("환불 가능 금액(" + refundable + "원)을 초과했습니다.");
        }

        if (amount.compareTo(refundable) == 0) {
            payment.markCancelled(amount);
        } else {
            payment.addCancelAmount(amount);
        }

        Refund refund = Refund.createCompleted(payment, amount, Refund.Category.MANUAL, reason, null);
        refundRepository.save(refund);
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