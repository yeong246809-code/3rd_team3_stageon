# 🎟️ StageOn

> AI 기반 실시간 예매 & 안전 티켓 거래 플랫폼

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-Wrapper-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Template-005F0F?logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)

## 프로젝트 소개

**StageOn**은 공연과 전시 정보를 한곳에서 탐색하고, 실시간 예매와 안전한 티켓 거래 경험을 제공하는 것을 목표로 하는 팀 프로젝트입니다.

현재는 Figma 디자인을 기반으로 메인 페이지 UI와 Spring Boot 프로젝트 기반을 구축했으며, 이후 예매·랭킹·추천·AI 챗봇 기능을 단계적으로 연결할 예정입니다.

- 프로젝트 유형: 팀 프로젝트 / 포트폴리오
- 개발 인원: 4명
- 주요 사용자: 공연·전시 정보를 빠르게 찾고 예매하려는 사용자
- 디자인: [StageOn Figma](https://www.figma.com/design/J3VQVtr3MeOpH3skhIrHnD/3%EC%B0%A8-team3-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=20-5)

## 주요 화면 및 기능

### 현재 구현

- 공연·전시 메인 페이지
- 티켓 오픈 예정작 카드 목록
- 장르별 랭킹 UI
- 오늘의 추천 공연 및 파트너 공연장 영역
- 반응형 레이아웃
- AI 챗봇 및 맨 위로 이동 플로팅 버튼 UI

### 확장 예정

- 회원가입 및 로그인
- 공연 검색과 상세 정보 조회
- 실시간 좌석 선택 및 예매
- 사용자 맞춤형 공연 추천
- 안전 티켓 거래
- AI 챗봇 연동

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21, HTML5, CSS3, JavaScript |
| Backend | Spring Boot 4, Spring MVC |
| View | Thymeleaf |
| Data | Spring Data JPA, MyBatis, MySQL, Redis |
| Realtime / AI | WebSocket, Spring AI, Ollama |
| Build | Gradle Wrapper |
| Design | Figma |

> 일부 백엔드·데이터·AI 기술은 프로젝트 의존성 구성을 완료한 단계이며, 기능 개발 과정에서 순차적으로 적용합니다.

## 프로젝트 구조

```text
3rd_team_3/
├─ gradle/                         # Gradle Wrapper 설정
├─ src/
│  ├─ main/
│  │  ├─ java/kr/co/stageon/      # Spring Boot 애플리케이션
│  │  └─ resources/
│  │     ├─ static/css/           # 정적 스타일시트
│  │     ├─ templates/            # Thymeleaf 화면
│  │     └─ application.properties
│  └─ test/                       # 테스트 코드
├─ build.gradle
├─ settings.gradle
└─ README.md
```

## 실행 방법

### 요구 사항

- JDK 21
- Git

### 실행

```bash
git clone https://github.com/yeong246809-code/3rd_team3_stageon.git
cd 3rd_team3_stageon
```

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

## 팀원

| No. | 이름 |
| :---: | :---: |
| 1 | <!-- 이름 입력 --> |
| 2 | <!-- 이름 입력 --> |
| 3 | <!-- 이름 입력 --> |
| 4 | <!-- 이름 입력 --> |

## 협업 방식

- 기능 단위 브랜치를 생성하여 작업합니다.
- 작업 완료 후 Pull Request를 생성하고 팀원의 리뷰를 거칩니다.
- 커밋 메시지는 변경 목적이 드러나도록 작성합니다.
- API 키, 환경변수, 로그 및 빌드 결과물은 Git에 포함하지 않습니다.

---

<p align="center">StageOn Team 3 · 2026</p>
