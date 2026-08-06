package kr.co.stageon.queue.controller;

import kr.co.stageon.queue.service.WaitingQueueHistoryCreateService;
import kr.co.stageon.queue.service.WaitingQueueHistoryCreateService.QueueEntryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * [남수아 담당]
 * 좌석선택하기 버튼에서 scheduleId를 받아
 * waiting_queue_history INSERT만 수행하는 API입니다.
 *
 * 대기열 화면 이동과 Redis 처리는 다른 담당 영역이므로 포함하지 않습니다.
 */
@RestController
@RequestMapping("/api/waiting-queue-history")
@RequiredArgsConstructor
public class WaitingQueueHistoryApiController {

    private final WaitingQueueHistoryCreateService createService;

    /**
     * 화면에서는 scheduleId만 전달합니다.
     * memberId는 위·변조를 막기 위해 로그인 이메일로 서버에서 조회합니다.
     */
    @PostMapping
    public ResponseEntity<QueueEntryResponse> createHistory(
            @RequestParam Long scheduleId,
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
                authentication.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(QueueEntryResponse.from(result));
    }

    /**
     * 잘못된 회차 번호나 회원 정보가 전달된 경우 400 응답으로 반환합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", exception.getMessage()));
    }

    /**
     * INSERT 결과 확인용 응답입니다.
     * queueToken은 현재 화면에서 이동에 사용하지 않고 콘솔에서만 확인합니다.
     * 이후 Redis 담당자가 같은 API 응답을 이어서 활용할 수 있습니다.
     */
    public record QueueEntryResponse(
            Long historyId,
            Long scheduleId,
            Long memberId,
            String queueToken,
            String queueTokenHash,
            String status,
            LocalDateTime joinedAt
    ) {
        private static QueueEntryResponse from(
                QueueEntryResult result
        ) {
            return new QueueEntryResponse(
                    result.historyId(),
                    result.scheduleId(),
                    result.memberId(),
                    result.queueToken(),
                    result.queueTokenHash(),
                    result.status(),
                    result.joinedAt()
            );
        }
    }
}