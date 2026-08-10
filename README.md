<div align="center">

# 🎭 StageOn

### Redis 기반 실시간 공연 예매 플랫폼

공연 탐색부터 대기열, 좌석 선점, 모의 결제, 예매 내역까지<br>
하나의 흐름으로 연결하는 3조 팀 프로젝트입니다.

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-Wrapper-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Template-005F0F?style=flat-square&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-Queue-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![Figma](https://img.shields.io/badge/Figma-Design-F24E1E?style=flat-square&logo=figma&logoColor=white)](https://www.figma.com/design/J3VQVtr3MeOpH3skhIrHnD/3%EC%B0%A8-team3-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=0-1)

[Figma 디자인 보기](https://www.figma.com/design/J3VQVtr3MeOpH3skhIrHnD/3%EC%B0%A8-team3-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=0-1) · [Repository](https://github.com/yeong246809-code/3rd_team3_stageon)

[프로젝트 개요](#project-overview) · [현재 진행 상태](#progress) · [핵심 기능](#features) · [기술 스택](#tech-stack) · [팀 구성](#team)

</div>

---

<a id="project-overview"></a>

## 🔗 프로젝트 개요

StageOn은 인기 공연 예매 시 발생하는 순간 트래픽과 동일 좌석 중복 예약 문제를 해결하는 공연 티켓팅 서비스입니다. 1차 목표는 MySQL과 Redis를 기반으로 `공연 탐색 → 대기열 → 좌석 선택·선점 → 예약·모의 결제 → 예매 내역` 흐름을 안정적으로 완성하고 검증하는 것입니다.

핵심 사이트와 테스트가 완성된 뒤에는 PostgreSQL의 `pgvector`와 Ollama를 연결한 RAG 챗봇을 2차 확장으로 추가합니다. 챗봇은 StageOn 안내 문서와 FAQ를 검색하여 근거 기반 답변을 제공하며, 예매 트랜잭션과 분리합니다.

> 기준일: 2026-08-10. Seats.io, 실제 PG 결제, 안전 티켓 거래·에스크로, QR 검표, 환불·정산은 현재 MVP 범위에서 제외합니다.

<a id="progress"></a>

## 📊 현재 진행 상태

| 영역 | 상태 | 확인 내용 |
| --- | :---: | --- |
| 홈·공연·회원·관리자 화면 | 배포 완료 | 공개 서버에서 주요 화면과 조회 흐름 확인 |
| Redis 대기열·입장 토큰 | 진행 중 | 기본 구조 구현, 자동 입장·복구·부하 검증 보완 필요 |
| 로컬 좌석 조회·임시 선점 | 진행 중 | MySQL 좌석 재고와 5분 TTL 만료 처리 연결 중 |
| 예약·모의 결제 | 진행 중 | 예약 확정, 멱등성, 실패 보상 E2E 완성 필요 |
| 자동화 테스트 | 보완 필요 | 테스트 클래스는 있으나 현재 실행 환경의 ClassNotFound 문제 수정 필요 |
| RAG 챗봇 | 2차 예정 | PostgreSQL + pgvector + Ollama, 핵심 사이트 완료 후 진행 |

> 완료는 화면 존재가 아니라 사용자 흐름이 끝까지 동작하고 테스트 또는 재현 가능한 검증 근거가 있을 때로 판단합니다.

<a id="features"></a>

## ✨ 핵심 기능

| 기능 | 설명 | 단계 |
| --- | --- | :---: |
| 공연 탐색 | 공연명·장소·날짜·장르 기반 목록과 상세 조회 | 1차 |
| Redis 대기열 | 회차별 순번, 제한 인원 입장, TTL 입장 토큰 | 1차 |
| 로컬 좌석 선점 | 좌석 상태 조회, 5분 임시 선점, 만료 복구 | 1차 |
| 예약·모의 결제 | 예약 생성, 결제 멱등성, 실패 시 좌석 복구 | 1차 |
| 마이페이지 | 회원 정보와 본인 예매 내역 조회 | 1차 |
| 관리자 운영 | 공연·공연장·회차·좌석 재고·주문 관리 | 1차 |
| RAG 공연 도우미 | 문서 임베딩 검색과 Ollama 기반 근거 답변 | 2차 |

## 🧭 서비스 흐름

```text
StageOn
└── 공연 탐색
    └── 회차 선택
        └── Redis 대기열
            └── 입장 토큰
                └── 좌석 조회
                    └── 5분 임시 선점
                        └── 예약 생성
                            └── 모의 결제
                                └── 예매 확정
                                    └── 마이페이지
```

<a id="tech-stack"></a>

## 🛠️ 기술 스택

| 구분 | 기술 |
| --- | --- |
| **Language** | Java 21, HTML5, CSS3, JavaScript |
| **Backend** | Spring Boot 4.0.7, Spring MVC, Spring Security, WebFlux |
| **View** | Thymeleaf |
| **Persistence** | Spring Data JPA, MyBatis, JDBC |
| **Core Data** | MySQL 8.x, Redis, Redisson |
| **Realtime** | SSE / WebSocket 검토 |
| **External Data** | KOPIS API |
| **Phase 2 AI** | PostgreSQL, pgvector, Ollama, Spring AI |
| **Test** | JUnit 5, JMeter 또는 nGrinder |
| **Build / Design** | Gradle Wrapper, Figma |

## 🗄️ 데이터 구조

물리 좌석과 회차별 판매 재고를 분리합니다.

```text
공연 → 공연 회차 → 회차별 판매 좌석 → 좌석 임시 선점 → 예약 → 결제
```

- **`seats`**: 공연장에 실제로 존재하는 좌석
- **`schedule_seats`**: 특정 회차에서 판매하는 좌석의 가격과 상태
- **`seat_holds`, `seat_hold_items`**: 사용자별 임시 선점과 만료 시간
- **`reservations`, `reservation_seats`, `payments`**: 최종 예매 좌석과 결제 결과
- **`rag_documents`, `rag_document_chunks`**: 2차 RAG 문서와 임베딩 청크

기존 스키마에 남아 있는 Seats.io 식별자 컬럼은 사용하지 않으며 제거 대상으로 관리합니다.

<a id="team"></a>

## 👥 Team StageOn

| No. | 이름 | 담당 영역 |
| :---: | :---: | --- |
| 1 | 이찬영 | **팀장 · Redis · 성능 검증 · 배포 · 2차 RAG** |
| 2 | 남수아 | **회원 · 마이페이지 · 예매 내역** |
| 3 | 강채은 | **좌석 · 예약 · 모의 결제 · 유스케이스** |
| 4 | 김민찬 | **관리자 기능 전반** |

## 📚 문서

| 문서 | 내용 |
| --- | --- |
| [PRD](docs/PRD.md) | 범위, 요구사항, 데이터·동시성 설계, 완료 기준 |
| `database/stageon-schema.sql` | MySQL 스키마 |
| `docs/sample-data.sql` | 화면·조회 검증용 데이터 |

---

<div align="center">

**StageOn Team 3 · 2026**

</div>
