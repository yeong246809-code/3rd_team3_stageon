package kr.co.stageon.queue.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestDataController {

    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/test/fill-redis")
    public String fillRedis() {
        String waitingKey = "queue:schedule:1:waiting";
        long currentTime = System.currentTimeMillis();

        // 파이프라인 기법을 사용해 20만 건을 1초 만에 고속 저장!
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (int i = 1; i <= 200000; i++) {
                byte[] key = waitingKey.getBytes();
                byte[] value = ("dummy_token_" + i).getBytes();
                // 내 토큰보다 과거에 들어온 사람으로 만들기 위해 점수(시간)를 조작
                connection.zAdd(key, currentTime - 3000000 + i, value);
            }
            return null;
        });

        return "<h1>Redis 20만 명 대기열 세팅 완료!</h1>";
    }
}