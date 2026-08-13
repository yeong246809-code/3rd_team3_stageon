package kr.co.stageon.queue.controller;

import kr.co.stageon.queue.service.WaitingQueueHistoryCreateService;
import kr.co.stageon.queue.service.WaitingQueueHistoryCreateService.QueueEntryResult;
import kr.co.stageon.queue.config.WaitingQueueProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * [남수아 담당]
 * 좌석선택하기 버튼에서 scheduleId를 받아
 * MySQL 이력과 Redis 대기열을 등록하고 원본 토큰은 HttpOnly 쿠키로 전달합니다.
 */
@RestController
@RequestMapping("/api/waiting-queue-history")
@RequiredArgsConstructor
public class WaitingQueueHistoryApiController {

    private final WaitingQueueHistoryCreateService createService;
    private final WaitingQueueProperties properties;

    /**
     * 화면에서는 scheduleId만 전달합니다.
     * memberId는 위·변조를 막기 위해 로그인 이메일로 서버에서 조회합니다.
     */
    @PostMapping
    public ResponseEntity<QueueEntryResponse> createHistory(
            @RequestParam Long scheduleId,
            @CookieValue(name = WaitingQueueProperties.COOKIE_NAME, required = false) String currentQueueToken,
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        QueueEntryResult result = createService.create(
                scheduleId,
                authentication.getName(),
                currentQueueToken
        );

        ResponseCookie queueCookie = ResponseCookie
                .from(WaitingQueueProperties.COOKIE_NAME, result.queueToken())
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(properties.getWaitingTtl())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, queueCookie.toString())
                .body(QueueEntryResponse.from(result));
    }

    /**
     * 잘못된 회차 번호나 회원 정보가 전달된 경우 400 응답으로 반환합니다.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            RuntimeException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", exception.getMessage()));
    }

    public record QueueEntryResponse(
            Long historyId,
            Long performanceId,
            Long scheduleId,
            Long memberId,
            String status,
            LocalDateTime joinedAt
    ) {
        private static QueueEntryResponse from(
                QueueEntryResult result
        ) {
            return new QueueEntryResponse(
                    result.historyId(),
                    result.performanceId(),
                    result.scheduleId(),
                    result.memberId(),
                    result.status(),
                    result.joinedAt()
            );
        }
    }
}
