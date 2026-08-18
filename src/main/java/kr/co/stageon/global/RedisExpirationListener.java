package kr.co.stageon.global;

import kr.co.stageon.booking.service.SeatRealtimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisExpirationListener extends KeyExpirationEventMessageListener {

    private final SeatRealtimeService seatRealtimeService;

    public RedisExpirationListener(RedisMessageListenerContainer listenerContainer,
                                   SeatRealtimeService seatRealtimeService) {
        super(listenerContainer);
        this.seatRealtimeService = seatRealtimeService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith("seat:selecting:")) {
            try {
                String seatIdStr = expiredKey.replace("seat:selecting:", "");
                Long seatId = Long.parseLong(seatIdStr);

                log.info("👻 [유령 좌석 감지] Redis 10분 만료. 실시간 방송 전송");
                // 🚨 Redis에서 10분이 지나 증발할 때 실시간 방송!
                seatRealtimeService.notifySeatStatus(seatId, "AVAILABLE");

            } catch (NumberFormatException e) {
                log.error("Redis 만료 키 파싱 오류: {}", expiredKey);
            }
        }
    }
}