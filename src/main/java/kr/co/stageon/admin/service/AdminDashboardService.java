package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.DashboardStatsDto;
import kr.co.stageon.admin.dto.RecentReservationDto;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;
import kr.co.stageon.queue.domain.WaitingQueueHistory;
import kr.co.stageon.queue.repository.WaitingQueueHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 관리자 대시보드 상단 통계, 최근 예매 목록, 외부 연동 상태를 조회합니다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int RECENT_RESERVATION_LIMIT = 5;
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(1);

    private final ReservationRepository reservationRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final PaymentRepository paymentRepository;
    private final WaitingQueueHistoryRepository waitingQueueHistoryRepository;

    /** application.yml에 ai.ollama.base-url 로 설정 가능. 없으면 임시 주소를 기본값으로 사용합니다. */
    @Value("${ai.ollama.base-url:http://192.168.0.2:3000/}")
    private String ollamaBaseUrl;

    public DashboardStatsDto getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        long todayReservationCount = reservationRepository.countByStatusAndReservedAtBetween(
                Reservation.Status.RESERVED, startOfToday, startOfTomorrow);

        long waitingMemberCount = waitingQueueHistoryRepository.countByStatus(
                WaitingQueueHistory.Status.WAITING);

        long heldSeatCount = seatHoldRepository.countByStatusAndExpiresAtAfter(
                SeatHold.Status.ACTIVE, now);

        double paymentSuccessRate = calculatePaymentSuccessRate(startOfToday, startOfTomorrow);

        List<RecentReservationDto> recentReservations = reservationRepository
                .findRecentWithPerformance(PageRequest.of(0, RECENT_RESERVATION_LIMIT))
                .stream()
                .map(r -> new RecentReservationDto(
                        r.getBookingNumber(),
                        r.getSchedule().getPerformance().getTitle(),
                        r.getStatus().name()
                ))
                .toList();

        boolean ollamaConnected = checkOllamaConnected();

        return new DashboardStatsDto(
                todayReservationCount,
                waitingMemberCount,
                heldSeatCount,
                paymentSuccessRate,
                recentReservations,
                ollamaConnected
        );
    }

    private double calculatePaymentSuccessRate(LocalDateTime start, LocalDateTime end) {
        long totalCount = paymentRepository.countByRequestedAtBetween(start, end);
        if (totalCount == 0) {
            return 0.0;
        }
        long successCount = paymentRepository.countByStatusAndRequestedAtBetween(
                Payment.Status.SUCCESS, start, end);
        return Math.round((successCount * 1000.0 / totalCount)) / 10.0; // 소수 1자리 반올림
    }

    /** Ollama(AI) 서버에 짧은 타임아웃으로 GET을 날려 응답 여부만 확인합니다. */
    private boolean checkOllamaConnected() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(HEALTH_CHECK_TIMEOUT)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl))
                    .timeout(HEALTH_CHECK_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception e) {
            log.warn("Ollama 연결 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}