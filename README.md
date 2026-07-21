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

> 현재 저장소에는 Figma 디자인을 기반으로 한 메인 페이지 UI와 Spring Boot 프로젝트 기반이 구현되어 있으며, 나머지 기능은 단계적으로 연동할 예정입니다.

## ✨ 주요 기능

| 기능 | 설명 | 상태 |
| --- | --- | :---: |
| 🔍 공연 탐색 및 검색 | 공연명, 아티스트, 장소, 날짜와 장르를 기준으로 콘텐츠 탐색 | 예정 |
| 🤖 AI 맞춤 추천 | 사용자 취향과 탐색 이력을 기반으로 공연 큐레이션 제공 | 예정 |
| 🎫 실시간 좌석 선택 | 공연장 좌석 배치와 잔여 좌석을 확인하고 좌석 선점 | 예정 |
| 🔒 안전 티켓 거래 | 거래 상태를 추적할 수 있는 사용자 간 티켓 거래 환경 제공 | 예정 |
| 👤 마이페이지 / 티켓 관리 | 예매 내역, 디지털 티켓과 거래 정보를 한곳에서 관리 | 예정 |
| 💬 AI 티켓 도우미 | 원하는 공연을 빠르게 찾을 수 있는 챗봇 인터페이스 | UI 구현 |
| 🖥️ 반응형 메인 페이지 | 티켓 오픈, 장르별 랭킹, 추천 공연과 공연장 콘텐츠 제공 | UI 구현 |

## 🧭 서비스 구성

```text
StageOn
├── Main                 메인 대시보드 및 추천 콘텐츠
├── Browse               공연 탐색 및 검색 필터
├── Performance Detail   공연·아티스트·일정 상세 정보
├── Seat Selector        실시간 좌석 조회 및 선택
├── Checkout             주문서 작성 및 결제
├── Booking Complete     예매 완료 및 티켓 발급
├── My Page              회원 정보 및 예매 내역 관리
├── Ticket Exchange      사용자 간 안전 티켓 거래
├── AI Recommend         사용자 맞춤 공연 큐레이션
└── Support              FAQ 및 고객 문의
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
| Build | Gradle Wrapper |
| Design | Figma |

## 📁 프로젝트 구조

```text
3rd_team3_stageon/
├── gradle/                              # Gradle Wrapper 설정
├── src/
│   ├── main/
│   │   ├── java/kr/co/stageon/          # Spring Boot 애플리케이션
│   │   └── resources/
│   │       ├── static/css/              # 정적 스타일시트
│   │       ├── templates/               # Thymeleaf 화면
│   │       └── application.properties
│   └── test/                            # 테스트 코드
├── build.gradle
├── settings.gradle
└── README.md
```

## 🚀 시작하기

### 요구 사항

- JDK 21
- Git

### 저장소 복제

```bash
git clone https://github.com/yeong246809-code/3rd_team3_stageon.git
cd 3rd_team3_stageon
```

### 애플리케이션 실행

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

실행 후 브라우저에서 `http://localhost:8080`으로 접속합니다.

## 👥 Team StageOn

<!-- 아래 표의 빈 칸에 팀원 이름과 담당 영역을 입력해 주세요. -->

| No. | 이름 | 담당 영역 |
| :---: | :---: | :--- |
| 1 |  |  |
| 2 |  |  |
| 3 |  |  |
| 4 |  |  |

## 🤝 협업 방식

- 기능 단위 브랜치를 생성하여 작업합니다.
- 작업 완료 후 Pull Request를 생성하고 팀원 리뷰를 거칩니다.
- 커밋 메시지는 변경 목적을 명확히 알 수 있도록 작성합니다.
- API 키, 환경 변수, 로그와 빌드 결과물은 Git에 포함하지 않습니다.

---

<div align="center">

**StageOn Team 3 · 2026**

</div>
