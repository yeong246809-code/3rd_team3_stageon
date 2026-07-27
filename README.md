<div align="center">

# 🎟️ StageOn

### 공연의 시작, 스테이지온

공연 탐색부터 실시간 좌석 예매, 맞춤 추천과 안전한 티켓 거래까지<br>
하나의 흐름으로 연결하는 문화 공연 플랫폼입니다.

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-Wrapper-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Template-005F0F?style=flat-square&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![MySQL](https://img.shields.io/badge/MySQL-Connector-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Figma](https://img.shields.io/badge/Figma-Design-F24E1E?style=flat-square&logo=figma&logoColor=white)](https://www.figma.com/design/J3VQVtr3MeOpH3skhIrHnD/3%EC%B0%A8-team3-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=0-1)

[Figma 디자인 보기](https://www.figma.com/design/J3VQVtr3MeOpH3skhIrHnD/3%EC%B0%A8-team3-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=0-1) · [Repository](https://github.com/yeong246809-code/3rd_team3_stageon)

</div>

---

## 🔗 프로젝트 소개

**StageOn**은 뮤지컬, 콘서트, 연극, 전시 등 다양한 문화 콘텐츠를 한곳에서 탐색하고 예매할 수 있도록 기획한 웹 기반 티켓 플랫폼입니다.

단순한 예매 서비스를 넘어 사용자의 취향과 이용 기록을 활용한 **AI 맞춤 추천**, 좌석 현황을 직관적으로 확인하는 **실시간 좌석 선택**, 신뢰할 수 있는 **티켓 거래**, 예매 내역을 모아 보는 **티켓 관리**까지 하나의 서비스 경험으로 연결하는 것을 목표로 합니다.

> 현재 저장소는 Figma 기반의 사용자·관리자 화면, 공연·회차·좌석·예매 조회 API, 핵심 JPA Entity와 MySQL 스키마를 중심으로 구성되어 있습니다. KOPIS, Redis, Seats.io와 AI 기능은 이 구조를 기준으로 단계적으로 연결합니다.

## ✨ 주요 기능

| 기능 | 설명 | 상태 |
| --- | --- | :---: |
| 🔍 공연 탐색 및 검색 | 공연명, 장소, 날짜와 장르를 기준으로 콘텐츠 탐색 |  |
| ⏳ Redis 예매 대기열 | 회차별 대기 순번과 입장 토큰을 이용한 진입 제어 |  |
| 🎫 실시간 좌석 선택 | 공연장 좌석 배치와 잔여 좌석을 확인하고 좌석 선점 |  |
| 💳 예매 및 모의 결제 | 선택 좌석 확인, 예약 생성, 모의 결제와 결과 저장 |  |
| 👤 마이페이지 / 예매 관리 | 회원 정보와 예매 내역을 한곳에서 확인 |  |
| 💬 AI 공연 도우미 | 공연 추천과 예매 관련 FAQ를 제공하는 인터페이스 |  |
| 🛠️ 관리자 운영 | 공연, 공연장, 회차, 좌석 재고와 주문 정보 관리 |  |
| 🖥️ 반응형 화면 | 사용자와 관리자 예매 흐름에 필요한 화면 제공 |  |

## 🧭 서비스 구성

```text
StageOn
├── Main                 메인 대시보드 및 추천 콘텐츠
├── Browse               공연 탐색 및 검색 필터
├── Performance Detail   공연·아티스트·일정 상세 정보
├── Schedule Select      공연 회차 선택
├── Waiting Queue        Redis 대기열 및 입장 처리
├── Seat Selector        실시간 좌석 조회 및 선택
├── Checkout             주문서 작성 및 결제
├── Booking Complete     예매 완료
├── My Page              회원 정보 및 예매 내역 관리
├── AI Recommend         사용자 맞춤 공연 큐레이션
├── Support              AI FAQ 및 예매 안내
└── Admin                공연·공연장·회차·좌석·주문 관리
```

## 🛠️ 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21, HTML5, CSS3, JavaScript |
| Backend | Spring Boot 4.0.7, Spring MVC, Spring WebFlux |
| View | Thymeleaf |
| Persistence | Spring Data JPA, MyBatis, JDBC |
| Database / Cache | MySQL, Redis |
| Realtime | Spring WebSocket |
| AI | Spring AI 2.0.0, Ollama |
| External Data / Seat | KOPIS API, Seats.io |
| Build | Gradle Wrapper |
| Design | Figma |

## 📁 프로젝트 구조

```text
3rd_team3_stageon/
├── database/
│   └── stageon-schema.sql               # MySQL 전체 스키마와 한글 COMMENT
├── docs/
│   ├── PRD.md                            # MVP 요구사항과 기술 설계
│   └── sample-data.sql                   # 화면·조회 확인용 최소 데이터
├── gradle/                              # Gradle Wrapper 설정
├── src/
│   ├── main/
│   │   ├── java/kr/co/stageon/
│   │   │   ├── admin/                    # 관리자 화면
│   │   │   ├── ai/                       # AI 화면과 대화 이력
│   │   │   ├── booking/                  # 좌석·선점·예매
│   │   │   ├── member/                   # 회원
│   │   │   ├── payment/                  # 결제
│   │   │   ├── performance/              # 공연과 회차
│   │   │   ├── queue/                    # 대기열 이력
│   │   │   └── venue/                    # 공연장·홀·좌석도
│   │   └── resources/
│   │       ├── static/                   # CSS와 화면 이미지
│   │       ├── templates/                # 사용자·예매·관리자 화면
│   │       └── application.properties
│   └── test/                            # 테스트 코드
├── build.gradle
├── settings.gradle
└── README.md
```

## 🗄️ 데이터 구조

핵심 예매 데이터는 다음 흐름으로 연결됩니다.

```text
공연 → 공연 회차 → 회차별 판매 좌석 → 좌석 임시 선점 → 예매 → 결제
```

- `seats`는 공연장에 실제로 존재하는 좌석을 관리합니다.
- `schedule_seats`는 특정 공연 회차에서 판매되는 좌석과 실제 판매 가격을 관리합니다.
- `seat_holds`와 `seat_hold_items`는 사용자가 임시 선점한 좌석을 기록합니다.
- `reservations`, `reservation_seats`, `payments`는 최종 예매 좌석과 결제 결과를 보관합니다.
- KOPIS 공연 식별자와 Seats.io의 차트·이벤트·좌석 식별자는 선택값으로 저장할 수 있습니다.

출연진, 공연 이미지, 외부 예매처, 공연별 가격 정책, 환불, 모바일 티켓, 외부 API 원본 스냅샷 테이블은 사용하지 않아도 핵심 예매 흐름에 영향을 주지 않는 선택 구조입니다.

## 👥 Team StageOn

| No. | 이름 | 담당 영역 |
| :---: | :---: | :--- |
| 1 | 이찬영 | **공연 탐색 · AI · Redis 대기열 · 전체 통합**|
| 2 | 남수아 | **회원 · 회차 선택 · 예매내역**|
| 3 | 강채은 | **좌석 · 주문 · 모의 결제 · 예약 확정**|
| 4 | 김민찬 | **관리자 운영 전체**|

## 🤝 협업 방식

- 기능 단위 브랜치를 생성하여 작업합니다.
- 작업 완료 후 Pull Request를 생성하고 팀원 리뷰를 거칩니다.
- 커밋 메시지는 변경 목적을 명확히 알 수 있도록 작성합니다.
- API 키, 환경 변수, 로그와 빌드 결과물은 Git에 포함하지 않습니다.

---

<div align="center">

**StageOn Team 3 · 2026**

</div>
