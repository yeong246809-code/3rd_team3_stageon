import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 옵션 설정
export const options = {
    stages: [
        { duration: '5s', target: 100 },  // 5초 동안 가상 유저를 0명에서 50명으로 늘림 (Ramp-up)
        { duration: '20s', target: 50 }, // 20초 동안 50명의 유저가 지속적으로 요청을 보냄
        { duration: '5s', target: 0 },   // 마지막 5초 동안 유저를 0명으로 서서히 줄임 (Ramp-down)
    ],
};

export default function () {
    // 우리가 만든 대기열 페이지 URL (토큰은 DB에 있는 테스트용 토큰 사용)
    const url = 'http://localhost:8080/booking/queue?performanceId=1&scheduleId=1&token=hash_token_waiting_0001';

    // GET 요청 보내기
    const res = http.get(url);

    // 응답 검증 (정상적으로 200 OK가 떨어졌는지, 화면 텍스트가 잘 나왔는지)
    check(res, {
        'is status 200': (r) => r.status === 200,
        'page loaded successfully': (r) => r.body.includes('예매 대기 중입니다'), // HTML에 이 문구가 있는지 확인
    });

    // 유저 1명이 요청을 보낸 후 1초 대기 (1초마다 새로고침하는 것과 같음)
    sleep(1);
}