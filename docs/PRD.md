# StageOn MVP PRD

> AI 기반 실시간 공연 티켓팅 서비스  
> 개발 기간: 1개월 / 개발 인원: 4명 / 목적: 취업 포트폴리오용 팀 프로젝트

## 1. 프로젝트 개요

StageOn은 인기 공연 예매 시 발생하는 순간 트래픽과 동일 좌석 중복 예약 문제를 Redis 대기열, 입장 토큰, 좌석 임시 선점, DB·분산락, 결제 멱등성으로 해결하는 공연 티켓팅 MVP다. 사용자는 공연 검색 또는 AI 추천을 통해 공연을 찾고, 대기열을 통과한 뒤 좌석을 임시 선점하여 모의 결제를 완료한다.

AI는 Ollama와 Qwen3 8B를 사용하지만 좌석 수, 가격, 결제 결과, 대기 순번 등 운영 데이터를 생성하거나 판단하지 않는다. Spring Boot가 조회한 실제 데이터만 설명하며 AI 장애가 일반 예매 흐름에 영향을 주지 않도록 격리한다.

### 기술 기준

- Java 21, Spring Boot, Spring MVC, Spring Security
- Spring Data JPA, MySQL
- Redis, Redisson
- 실시간 알림: SSE
- AI: Ollama + Qwen3 8B
- 외부 데이터: KOPIS API
- 화면: Thymeleaf + JavaScript
- 테스트: JUnit 5, JMeter 또는 nGrinder

### MVP 원칙

- 4명이 4주 안에 완성 가능한 범위만 구현한다.
- 단순 CRUD보다 대기열·동시성·멱등성·장애 복구의 증명에 집중한다.
- 실제 결제, 거래, 정산 등 법적·운영 복잡도가 큰 기능은 제외한다.
- Redis와 AI가 중단되어도 잘못된 예약·결제가 생성되지 않아야 한다.

## 2. 배경과 문제 정의

인기 공연은 예매 시작 직후 요청이 집중된다. 모든 사용자를 좌석 API와 DB로 직접 진입시키면 DB 커넥션이 고갈되고 응답시간이 급격히 증가한다. 동시에 여러 사용자가 동일 좌석을 선택할 경우 중복 예약이 발생할 수 있으며, 네트워크 재시도나 더블 클릭은 결제 중복 생성으로 이어질 수 있다.

StageOn은 다음 네 문제를 MVP의 핵심으로 정의한다.

1. 순간 트래픽을 서버와 DB가 처리 가능한 수준으로 제한한다.
2. 하나의 회차 좌석은 최종적으로 한 예약에만 귀속된다.
3. 좌석 선점 만료와 결제 실패 시 재고를 자동 복구한다.
4. 같은 결제 요청이 반복되어도 결제 레코드는 한 건만 생성한다.

## 3. 프로젝트 목표와 성공 기준

### 제품 목표

- 사용자가 로그인부터 예매내역 확인까지 단절 없이 완료한다.
- 허가된 사용자만 좌석 조회·선점 API를 호출한다.
- 장애 상황에서 상태가 명확하고 복구 가능한 구조를 만든다.
- AI와 외부 API를 핵심 예매 트랜잭션으로부터 분리한다.

### 성공 기준

| 지표 | 목표 |
| --- | --- |
| 동일 좌석 동시 요청 | 100개 요청 중 성공 1건 |
| 결제 멱등성 | 동일 Idempotency-Key 반복 시 Payment 1건 |
| 선점 복구 | TTL 만료 후 좌석이 AVAILABLE로 복구 |
| AI 장애 격리 | Ollama 종료 상태에서도 검색·예매·결제 정상 |
| 대기열 효과 | 적용 전후 API 응답시간·DB 커넥션 수 비교 자료 확보 |
| 부하 테스트 | TPS, 평균·p95 응답시간, 오류율 기록 |
| 핵심 흐름 | 로그인부터 예매내역까지 시연 가능 |

## 4. 주요 사용자 유형

| 유형 | 목적 | 권한 |
| --- | --- | --- |
| 비회원 | 공연 탐색 | 공연 목록·상세·AI FAQ 조회 |
| 회원 | 공연 예매 | 대기열, 좌석 선점, 결제, 예매내역 |
| 관리자 | 공연과 좌석 운영 | 공연·공연장·회차·재고·주문 조회 및 관리 |

인증은 1개월 MVP에 적합한 세션 기반 인증을 우선한다. 권한은 `ROLE_USER`, `ROLE_ADMIN` 두 종류만 사용한다.

## 5. 핵심 사용자 시나리오

### 정상 예매

로그인 → 공연 검색 또는 AI 추천 → 공연 상세 → 회차 선택 → Redis 대기열 등록 → 순번 확인 → 입장 토큰 발급 → 좌석 조회 → 좌석 임시 선점 → 주문 확인 → 모의 결제 → 예약 확정 → 마이페이지 예매내역 확인

### AI 추천

사용자가 자연어로 조건 입력 → AI가 구조화 조건 JSON 추출 → Spring Boot가 DB/KOPIS 동기화 데이터 조회 → AI가 조회 결과만 설명 → 사용자가 공연 상세로 이동

### 실패 복구

- 결제 실패: Payment FAILED → Reservation CANCELLED 또는 PENDING 종료 → 좌석 AVAILABLE 복구
- 선점 만료: HELD TTL 종료 → Reservation EXPIRED → 좌석 AVAILABLE 복구
- AI 장애: 일반 검색 링크와 안내 메시지 제공

## 6. 기능 요구사항

우선순위는 P0 필수, P1 중요, P2 여유 시 구현으로 정의한다.

| 기능 | 요구사항 | 우선순위 | 담당 |
| --- | --- | :---: | --- |
| 로그인·회원가입 | 세션 로그인, 사용자/관리자 권한 분리 | P0 | 팀장 |
| 공연 조회 | 목록, 검색, 상세, 회차 조회 | P0 | 비전공자 B |
| 공연 관리 | 공연·공연장·좌석·회차 등록/수정 | P0 | 비전공자 B |
| Redis 대기열 | 중복 방지, 순번·대기인원 조회, 제한 인원 입장 | P0 | 전공자 B |
| 입장 토큰 | 회차·회원 귀속, TTL, 좌석 API 검증 | P0 | 전공자 B |
| 좌석 조회 | AVAILABLE/HELD/RESERVED/BLOCKED 표시 | P0 | 전공자 A |
| 좌석 선점 | 5분 임시 선점, 소유자 검증, 만료 처리 | P0 | 전공자 A |
| 예약 | PENDING 생성, 좌석 연결, 확정·만료·취소 | P0 | 전공자 A |
| 모의 결제 | 성공·실패 결과, Idempotency-Key 필수 | P0 | 전공자 B |
| 장애 보상 | 결제 실패·선점 만료 시 좌석 복구 | P0 | 전공자 A/B |
| 마이페이지 | 본인 예매 목록·상세 조회 | P0 | 비전공자 B |
| 관리자 현황 | 좌석 재고·선점·예매·주문 조회 | P1 | 비전공자 B |
| KOPIS 연동 | 공연 데이터 수집, 실패 시 로컬 데이터 사용 | P1 | 팀장 |
| AI 조건 추출 | 자연어를 검증 가능한 검색 조건 DTO로 변환 | P1 | 팀장 |
| AI 추천·FAQ | 조회 결과 설명, FAQ 응답, 장애 Fallback | P1 | 팀장 |
| SSE | 대기 순번 또는 입장 허가 갱신 | P1 | 전공자 B |
| 알림 | 화면 내 성공·오류 안내 | P2 | 비전공자 B |

## 7. 비기능 요구사항

### 성능·동시성

- 대기열을 통과하지 않은 요청은 좌석 API 접근 전에 차단한다.
- 좌석 선점 트랜잭션은 짧게 유지한다.
- 동일 좌석 최종 상태는 DB 트랜잭션에서 다시 검증한다.
- 부하 테스트는 좌석 선점 코어가 아닌 대기열 진입·상태 조회 중심으로 분리한다.

### 보안

- 비밀번호는 BCrypt로 단방향 해시한다.
- 관리자 API는 `ROLE_ADMIN`을 요구한다.
- 결제·예약 조회는 리소스 소유자를 검증한다.
- 비밀키와 외부 API 키는 환경변수로 분리한다.
- Thymeleaf 폼은 CSRF 토큰을 유지한다.

### 신뢰성·관측성

- 모든 오류는 공통 오류 코드와 HTTP 상태로 응답한다.
- 요청 ID, 사용자 ID, 회차 ID, 예약 ID를 구조화 로그에 남긴다.
- Actuator로 health 상태를 제공하되 상세 정보는 관리자에게만 공개한다.
- Redis·Ollama·KOPIS 장애를 서로 다른 오류 코드로 구분한다.

### 유지보수성

- 도메인별 Controller/Service/Repository/Entity/DTO 구조를 사용한다.
- 다른 도메인의 Repository를 직접 호출하지 않고 Service 또는 Facade를 거친다.
- 외부 API DTO와 JPA Entity를 분리한다.
- 핵심 정책 값은 설정으로 분리한다: 선점 5분, 입장 토큰 TTL, 회차별 입장 인원.

## 8. 상태 전이

### 대기열

`WAITING → ENTERED → EXPIRED`

- WAITING: ZSET에 등록되어 순번을 기다리는 상태
- ENTERED: 입장 토큰을 발급받은 상태
- EXPIRED: 입장 토큰 TTL 종료 또는 대기열 데이터 만료

### 좌석

`AVAILABLE → HELD → RESERVED`

- `HELD → AVAILABLE`: 선점 만료, 사용자 해제, 결제 실패
- `AVAILABLE ↔ BLOCKED`: 관리자 운영 처리
- RESERVED는 MVP에서 관리자 직접 해제하지 않는다.

### 예약

`PENDING → RESERVED`

- `PENDING → CANCELLED`: 결제 실패 또는 사용자 취소
- `PENDING → EXPIRED`: 선점 만료
- `RESERVED → CANCELLED`: 전체 취소만 허용하는 확장 지점이며 MVP 시연에서는 선택 기능

### 결제

`READY → SUCCESS`

- `READY → FAILED`: 모의 결제 실패
- `READY → CANCELLED`: 결제 화면 이탈 또는 예약 만료

## 9. 예외 처리

| 상황 | 오류 코드 | 처리 |
| --- | --- | --- |
| 대기열 중복 등록 | QUEUE_ALREADY_REGISTERED | 기존 순번 반환 또는 409 |
| 입장 토큰 없음·만료 | QUEUE_TOKEN_EXPIRED | 좌석 API 403, 재진입 안내 |
| 입장 허가 전 접근 | QUEUE_NOT_ENTERED | 좌석 API 403 |
| 동일 좌석 충돌 | SEAT_ALREADY_HELD | 409, 좌석 현황 재조회 |
| 예약 완료 좌석 | SEAT_ALREADY_RESERVED | 409 |
| 선점 만료 | SEAT_HOLD_EXPIRED | 주문·결제 중단, 좌석 복구 |
| 결제 중복 요청 | PAYMENT_DUPLICATED | 기존 결제 결과 반환 |
| 결제 실패 | PAYMENT_FAILED | 예약 종료 및 좌석 복구 |
| Redis 장애 | REDIS_UNAVAILABLE | 대기열·좌석 진입 차단, 503 |
| Ollama 장애 | OLLAMA_UNAVAILABLE | AI만 비활성화, 일반 검색 유지 |
| KOPIS 장애 | EXTERNAL_API_UNAVAILABLE | 로컬 저장 데이터 조회 |

Redis가 불안정할 때 대기열을 우회하여 좌석 API를 여는 Fail-open 전략은 사용하지 않는다. 잘못된 동시 진입보다 안전한 차단을 우선한다.

## 10. 화면 목록

### 사용자 MVP 13개

1. 홈
2. 공연 검색 결과
3. 공연 상세
4. 회차 선택
5. 대기열
6. 좌석 선택
7. 주문 확인
8. 모의 결제
9. 예매 완료
10. 로그인
11. 회원가입
12. 마이페이지·예매내역
13. AI 추천·FAQ

### 관리자 MVP 7개

1. 관리자 로그인
2. 통합 대시보드
3. 공연 등록·수정
4. 공연장·좌석 관리
5. 일정·회차 관리
6. 좌석 재고·선점 현황
7. 예매·주문 조회

## 11. API 목록

### 인증·회원

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/members` | 회원가입 |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/logout` | 로그아웃 |
| GET | `/api/members/me` | 내 정보 |
| GET | `/api/members/me/reservations` | 내 예매내역 |

### 공연

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/performances` | 검색·목록 |
| GET | `/api/performances/{performanceId}` | 상세 |
| GET | `/api/performances/{performanceId}/schedules` | 회차 목록 |
| GET | `/api/schedules/{scheduleId}` | 회차 상세 |

### 대기열·입장

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/queues/{scheduleId}` | 대기열 등록 |
| GET | `/api/queues/{scheduleId}/status` | 순번·상태 조회 |
| GET | `/api/queues/{scheduleId}/events` | SSE 상태 스트림 |
| DELETE | `/api/queues/{scheduleId}` | 대기 취소 |
| POST | `/api/queues/{scheduleId}/enter` | 입장 토큰 발급·검증 |

### 좌석·예약

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/schedules/{scheduleId}/seats` | 좌석 현황 |
| POST | `/api/schedule-seats/{scheduleSeatId}/hold` | 좌석 선점 |
| DELETE | `/api/schedule-seats/{scheduleSeatId}/hold` | 선점 해제 |
| POST | `/api/reservations` | 선점 좌석으로 예약 생성 |
| GET | `/api/reservations/{reservationId}` | 예약 상세 |
| POST | `/api/reservations/{reservationId}/cancel` | 예약 취소 |

### 결제·AI

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/payments` | 모의 결제, Idempotency-Key 필수 |
| GET | `/api/payments/{paymentId}` | 결제 결과 |
| POST | `/api/ai/search-condition` | 자연어 조건 추출 |
| POST | `/api/ai/recommend` | 실제 조회 결과 추천 설명 |
| POST | `/api/ai/chat` | FAQ |

### 관리자

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/admin/performances` | 공연 등록 |
| PUT | `/api/admin/performances/{performanceId}` | 공연 수정 |
| POST | `/api/admin/venues` | 공연장 등록 |
| PUT | `/api/admin/venues/{venueId}/seats` | 좌석 구성 수정 |
| POST | `/api/admin/schedules` | 회차 등록 |
| PUT | `/api/admin/schedules/{scheduleId}` | 회차 수정 |
| GET | `/api/admin/schedules/{scheduleId}/seat-status` | 좌석 재고·선점 현황 |
| GET | `/api/admin/reservations` | 예매·주문 조회 |

## 12. ERD 핵심 엔티티

| 엔티티 | 핵심 필드 |
| --- | --- |
| Member | id, email, passwordHash, name, role |
| Performance | id, kopisId, title, genre, status, venueSummary |
| Venue | id, name, address |
| Seat | id, venueId, section, row, number, gradeId |
| SeatGrade | id, name, basePrice |
| PerformanceSchedule | id, performanceId, venueId, startsAt, status |
| ScheduleSeat | id, scheduleId, seatId, price, status, version |
| Reservation | id, memberId, scheduleId, status, totalAmount, expiresAt |
| ReservationSeat | id, reservationId, scheduleSeatId, price |
| Payment | id, reservationId, idempotencyKey, amount, status, requestedAt |
| WaitingQueueHistory | id, memberId, scheduleId, status, enteredAt |
| AiChatHistory | id, memberId, question, responseType, createdAt |

핵심 관계는 `Venue 1:N Seat`, `Performance 1:N PerformanceSchedule`, `PerformanceSchedule 1:N ScheduleSeat`, `Reservation 1:N ReservationSeat`, `ReservationSeat N:1 ScheduleSeat`, `Reservation 1:1 Payment`다. 물리 좌석 Seat와 회차별 판매 재고 ScheduleSeat를 반드시 분리한다.

## 13. Redis Key 설계

| Key | Type | Value/Member | TTL |
| --- | --- | --- | --- |
| `queue:schedule:{scheduleId}` | ZSET | memberId, score=진입 timestamp | 회차 종료까지 |
| `queue:member:{scheduleId}:{memberId}` | STRING | queueToken | 대기열 TTL |
| `queue:entry:{queueToken}` | HASH | memberId, scheduleId, status | 대기열 TTL |
| `queue:access:{entryToken}` | HASH | memberId, scheduleId | 5~10분 |
| `seat:lock:{scheduleSeatId}` | Redisson Lock | lock owner | 수 초 lease |
| `seat:hold:{scheduleSeatId}` | HASH | memberId, reservationId, expiresAt | 5분 |
| `payment:idempotency:{key}` | STRING | paymentId 또는 처리 결과 | 24시간 이상 |

대기열 ZSET에서 제거되더라도 WaitingQueueHistory는 DB에 최소 이벤트만 저장하여 테스트와 발표 근거로 사용한다.

## 14. 동시성 제어 전략

### 1차: JPA 비관적 락

- `ScheduleSeat`를 `PESSIMISTIC_WRITE`로 조회한다.
- 트랜잭션 안에서 AVAILABLE 여부와 기존 선점 소유자를 재검증한다.
- 선점 정보와 예약 상태를 같은 트랜잭션에서 갱신한다.
- 구현이 단순하고 정합성 증명이 쉬워 MVP 기본 전략으로 사용한다.

### 비교 실험: Redisson 분산락

- `seat:lock:{scheduleSeatId}`를 키로 제한 시간 내 락 획득을 시도한다.
- 락 획득 후에도 DB 상태를 다시 확인한다.
- 다중 인스턴스 환경의 장점을 측정하되 Redis 락만으로 DB 정합성을 보장한다고 주장하지 않는다.

### 결론 기준

- 정확성, 평균·p95 응답시간, DB 락 대기, 실패율을 비교한다.
- 1개월 MVP에서는 두 전략을 동시에 운영하지 않고 프로필 또는 구현체로 선택한다.

## 15. AI 역할과 제한

### 담당 역할

- 자연어 검색 조건을 제한된 JSON 스키마로 추출
- Spring Boot가 조회한 공연 후보의 추천 이유 설명
- 예매 절차, 좌석 등급, 취소 정책 등 FAQ 응답

### 금지 역할

- 잔여 좌석 수 생성 또는 추측
- 가격·대기 순번·결제 성공 여부 판단
- 예약 상태 변경
- 존재하지 않는 공연·공연장 생성
- 좌석 인접 여부 직접 판정

### 안전 구조

1. AI 출력 JSON을 DTO와 Bean Validation으로 검증한다.
2. 허용된 장르·지역·가격 범위만 수용한다.
3. Spring Boot가 DB를 조회한다.
4. AI에는 조회 결과와 설명에 필요한 최소 정보만 전달한다.
5. Ollama 장애 시 `FallbackAiService`가 고정 안내를 반환한다.

## 16. 팀원 역할과 책임

| 팀원 | 책임 | 주요 산출물 |
| --- | --- | --- |
| 전공자 A | 좌석 선점, 예약 코어, 비관적 락, Redisson 비교 | 동시성 테스트, 좌석 복구 테스트 |
| 전공자 B | Redis 대기열, 입장 토큰, 모의 결제, 멱등성 | 대기열·결제 테스트, 부하 테스트 |
| 비전공 팀장 | 공통 구조, KOPIS, Ollama/Qwen, Git·PR, 통합 | AI Fallback, 통합 문서, 발표 |
| 비전공자 B | 공연·공연장·회차, 관리자, 좌석 등급·가격 | 관리자 화면, 운영 데이터 |

각 담당자는 Controller부터 화면 연결과 테스트까지 기능 단위로 소유한다. 공통 코드 변경은 팀장 리뷰를 거친다.

## 17. 주차별 개발 일정

### 1주차 — 설계와 기술 검증

- PRD, ERD, API, 상태 전이 확정
- Spring Security 세션 로그인 검증
- MySQL·Redis Docker Compose
- 비관적 락·Redisson·SSE·Ollama·KOPIS 스파이크
- 공연·공연장·회차 기본 모델과 시드 데이터

### 2주차 — 기본 예매 흐름

- 공연 목록·상세·회차
- 관리자 공연·공연장·회차 관리
- 좌석 조회·선점·예약 PENDING
- 로그인, 홈, 상세, 좌석, 주문 화면

### 3주차 — 핵심 기술 통합

- Redis ZSET 대기열과 입장 토큰
- 결제 멱등성과 실패 보상
- 선점 만료 스케줄러
- KOPIS Provider와 Ollama/Fallback
- 사용자·관리자 전체 흐름 연결

### 4주차 — 검증과 발표

- 100개 동시 요청 테스트
- JMeter/nGrinder 전후 비교
- 장애 시나리오와 오류 화면
- UI·README·배포 환경 정리
- 아키텍처, 테스트 결과, 시연 영상, 발표 자료

## 18. 테스트 계획

### 단위·통합 테스트

- 동일 회차 대기열 중복 등록 방지
- 입장 토큰이 없는 좌석 API 접근 거부
- 만료된 입장 토큰 거부
- 동일 좌석 100개 동시 요청 중 1건 성공
- 선점 만료 후 AVAILABLE 복구
- 동일 Idempotency-Key 반복 시 Payment 1건
- 결제 실패 시 예약·좌석 복구
- 관리자 API 일반 사용자 접근 거부
- Ollama 장애 시 일반 예매 정상
- KOPIS 장애 시 LocalPerformanceProvider 동작

### 부하 테스트

1. 대기열 없이 좌석 조회·선점 요청 집중
2. 대기열 적용 후 허가된 사용자만 진입
3. TPS, 평균·p95 응답시간, 오류율, DB 커넥션 사용량 비교
4. 동일 환경·동일 데이터·동일 사용자 수를 유지한다.

## 19. MVP와 확장 기능 구분

### MVP

- 공연 검색·상세·회차
- 세션 로그인·회원가입
- Redis 대기열과 입장 토큰
- 좌석 임시 선점과 만료
- 비관적 락과 Redisson 비교
- 예약과 모의 결제 멱등성
- 결제 실패·만료 복구
- KOPIS/Local Provider
- AI 검색 조건·추천 설명·FAQ/Fallback
- 관리자 공연·공연장·회차·좌석·주문 조회

### PHASE2

- 찜한 공연
- 이메일·웹 알림
- 전체 예약 취소 자동화
- 다중 서버 확장과 메시지 브로커
- 좌석도 편집 도구 고도화

### OUT

- 실제 PG, 안전 티켓 거래, 에스크로
- 신고·분쟁, 부분 환불
- 정산·수수료, 판매자 지급
- 복잡한 쿠폰·할인
- 모바일 QR 검표
- 관리자 다단계 승인·2FA·권한 매트릭스
- AI 추천 A/B 테스트

## 20. 발표·포트폴리오 강조 포인트

1. 인기 공연 순간 트래픽과 중복 예약이라는 명확한 문제 정의
2. Redis ZSET 대기열 전후 DB 커넥션과 응답시간 비교
3. 비관적 락과 Redisson 분산락을 같은 조건에서 비교한 의사결정
4. 동일 좌석 100개 동시 요청에서 1건만 성공하는 자동화 테스트
5. 결제 멱등키와 실패 보상으로 중복 결제·좌석 유실을 방지한 흐름
6. 선점 TTL 만료 후 자동 복구 로그와 테스트
7. Ollama 중단 시에도 일반 예매가 유지되는 장애 격리
8. AI가 운영 데이터를 생성하지 않는 Guardrail 설계
9. 구현하지 않은 기능을 명확히 제외하여 1개월 내 완성도를 확보한 범위 관리

## 공통 응답과 핵심 오류 코드

```json
{
  "success": false,
  "code": "SEAT_ALREADY_HELD",
  "message": "이미 다른 사용자가 선점한 좌석입니다.",
  "data": null
}
```

필수 오류 코드는 `INVALID_REQUEST`, `UNAUTHORIZED`, `ACCESS_DENIED`, `PERFORMANCE_NOT_FOUND`, `SCHEDULE_NOT_FOUND`, `QUEUE_ALREADY_REGISTERED`, `QUEUE_TOKEN_EXPIRED`, `QUEUE_NOT_ENTERED`, `SEAT_NOT_FOUND`, `SEAT_ALREADY_HELD`, `SEAT_ALREADY_RESERVED`, `SEAT_HOLD_EXPIRED`, `RESERVATION_NOT_FOUND`, `PAYMENT_DUPLICATED`, `PAYMENT_FAILED`, `REDIS_UNAVAILABLE`, `OLLAMA_UNAVAILABLE`, `EXTERNAL_API_UNAVAILABLE`이다.
