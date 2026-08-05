package kr.co.stageon.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SeatRealtimeService {

    // 현재 좌석 페이지를 보고 있는 사용자들의 연결(Emitter)을 저장하는 안전한 리스트
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // 1. 사용자가 좌석 페이지에 들어오면 연결 생성
    public SseEmitter subscribe() {
        // 타임아웃 10분 설정 (만료 시 브라우저가 알아서 재연결함)
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        emitters.add(emitter);

        // 연결 종료 시 리스트에서 제거
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            // 첫 연결 시 더미 데이터를 보내 연결을 안정화
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    // 2. 누군가 예매를 취소/이탈해서 좌석이 풀렸을 때 모두에게 방송
    public void notifySeatStatus(Long scheduleSeatId, String status) {
        String payload = String.format("{\"seatId\":%d, \"status\":\"%s\"}", scheduleSeatId, status);

        for (SseEmitter emitter : emitters) {
            try {
                // "seatStatus"라는 이름으로 JSON 데이터 전송
                emitter.send(SseEmitter.event().name("seatStatus").data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}