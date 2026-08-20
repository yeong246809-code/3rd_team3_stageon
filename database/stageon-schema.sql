-- StageOn canonical MySQL schema.
-- This file defines a clean database. It does not drop or alter existing tables.
-- Optional feature tables may remain empty without affecting the core booking flow.

CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '회원 번호',
    email VARCHAR(190) NOT NULL COMMENT '로그인 이메일',
    password_hash VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    name VARCHAR(80) NOT NULL COMMENT '회원 이름',
    phone VARCHAR(30) NULL COMMENT '전화번호',
    role VARCHAR(20) NOT NULL COMMENT '회원 권한(USER, ADMIN)',
    status VARCHAR(20) NOT NULL COMMENT '계정 상태(ACTIVE, INACTIVE, WITHDRAWN)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '가입 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 및 관리자 계정';

CREATE TABLE performances (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공연 번호',
    kopis_id VARCHAR(50) NULL COMMENT 'KOPIS 공연 식별자',
    title VARCHAR(200) NOT NULL COMMENT '공연명',
    genre VARCHAR(50) NOT NULL COMMENT '공연 장르',
    poster_url VARCHAR(500) NULL COMMENT '대표 포스터 URL',
    poster_key VARCHAR(500) NULL COMMENT 'S3 객체 키',
    start_date DATE NOT NULL COMMENT '공연 시작일',
    end_date DATE NOT NULL COMMENT '공연 종료일',
    runtime_minutes INT NULL COMMENT '공연 시간(분)',
    age_text VARCHAR(100) NULL COMMENT '관람 연령 안내',
    story TEXT NULL COMMENT '공연 줄거리',
    raw_price_text TEXT NULL COMMENT 'KOPIS 티켓 가격 안내 원문',
    raw_schedule_text TEXT NULL COMMENT 'KOPIS 공연 시간 안내 원문',
    status VARCHAR(20) NOT NULL COMMENT '공연 상태(UPCOMING, ON_SALE, ENDED, CANCELLED)',
    kopis_updated_at DATETIME(6) NULL COMMENT 'KOPIS 최종 수정 일시',
    last_synced_at DATETIME(6) NULL COMMENT 'KOPIS 마지막 동기화 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_performance_kopis_id (kopis_id),
    KEY idx_performance_search (status, start_date, genre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공연 기본 정보';

CREATE TABLE banners (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '배너 번호',
    title VARCHAR(200) NOT NULL COMMENT '배너 제목',
    description VARCHAR(500) NULL COMMENT '배너 설명',
    image_url VARCHAR(500) NOT NULL COMMENT '브라우저 접근 URL',
    image_key VARCHAR(500) NULL COMMENT 'S3 객체 키',
    performance_id BIGINT NULL COMMENT '연결 공연 번호',
    link_url VARCHAR(500) NULL COMMENT '배너 링크',
    period_start DATE NULL COMMENT '노출 시작일',
    period_end DATE NULL COMMENT '노출 종료일',
    badge_text VARCHAR(50) NULL COMMENT '배지 문구',
    button1_text VARCHAR(30) NOT NULL COMMENT '첫 번째 버튼 문구',
    button1_url VARCHAR(500) NULL COMMENT '첫 번째 버튼 링크',
    button2_text VARCHAR(30) NOT NULL COMMENT '두 번째 버튼 문구',
    button2_url VARCHAR(500) NULL COMMENT '두 번째 버튼 링크',
    display_order INT NOT NULL COMMENT '표시 순서',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '노출 여부',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    KEY idx_banner_display (is_active, display_order),
    CONSTRAINT fk_banner_performance FOREIGN KEY (performance_id) REFERENCES performances (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='메인 히어로 배너';

CREATE TABLE site_settings (
    setting_key VARCHAR(100) NOT NULL COMMENT '설정 키',
    setting_value VARCHAR(500) NOT NULL COMMENT '설정 값',
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사이트 전역 설정';

CREATE TABLE venues (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공연시설 번호',
    kopis_facility_id VARCHAR(50) NULL COMMENT 'KOPIS 공연시설 식별자',
    name VARCHAR(150) NOT NULL COMMENT '공연시설명',
    address VARCHAR(300) NOT NULL COMMENT '공연시설 주소',
    region VARCHAR(50) NOT NULL COMMENT '공연시설 지역',
    latitude DECIMAL(10,7) NULL COMMENT '위도',
    longitude DECIMAL(10,7) NULL COMMENT '경도',
    phone VARCHAR(30) NULL COMMENT '대표 전화번호',
    homepage_url VARCHAR(500) NULL COMMENT '공식 홈페이지 URL',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_venue_kopis_facility_id (kopis_facility_id),
    KEY idx_venue_region_name (region, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='복수의 홀을 포함하는 공연시설';

CREATE TABLE venue_halls (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공연장 홀 번호',
    venue_id BIGINT NOT NULL COMMENT '소속 공연시설 번호',
    kopis_hall_id VARCHAR(50) NULL COMMENT 'KOPIS 공연장 식별자',
    name VARCHAR(150) NOT NULL COMMENT '공연장 홀 이름',
    seat_capacity INT NOT NULL COMMENT '전체 좌석 수',
    accessible_seat_count INT NULL COMMENT '장애인 좌석 수',
    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_venue_hall_name (venue_id, name),
    UNIQUE KEY uk_venue_hall_kopis_id (kopis_hall_id),
    CONSTRAINT fk_venue_hall_venue
        FOREIGN KEY (venue_id) REFERENCES venues (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공연시설 내부의 실제 공연장 홀';

CREATE TABLE seat_charts (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '좌석도 번호',
    venue_hall_id BIGINT NOT NULL COMMENT '좌석도를 사용하는 공연장 홀 번호',
    name VARCHAR(150) NOT NULL COMMENT '좌석도 이름',
    seatsio_chart_key VARCHAR(100) NULL COMMENT 'Seats.io 차트 식별자',
    version INT NOT NULL COMMENT '좌석도 버전',
    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '현재 사용 여부',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_chart_version (venue_hall_id, version),
    UNIQUE KEY uk_seat_chart_seatsio_key (seatsio_chart_key),
    CONSTRAINT fk_seat_chart_venue_hall
        FOREIGN KEY (venue_hall_id) REFERENCES venue_halls (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공연장 홀별 좌석 배치도';

CREATE TABLE seat_grades (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '좌석 등급 번호',
    seat_chart_id BIGINT NOT NULL COMMENT '소속 좌석도 번호',
    name VARCHAR(30) NOT NULL COMMENT '좌석 등급명',
    display_color VARCHAR(7) NOT NULL COMMENT '화면 표시 색상',
    sort_order INT NOT NULL COMMENT '화면 표시 순서',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_grade_name (seat_chart_id, name),
    CONSTRAINT fk_seat_grade_chart
        FOREIGN KEY (seat_chart_id) REFERENCES seat_charts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='좌석도의 기본 좌석 등급';

CREATE TABLE seats (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '물리 좌석 번호',
    seat_chart_id BIGINT NOT NULL COMMENT '소속 좌석도 번호',
    seat_grade_id BIGINT NOT NULL COMMENT '기본 좌석 등급 번호',
    seatsio_object_key VARCHAR(100) NULL COMMENT 'Seats.io 좌석 객체 식별자',
    object_type VARCHAR(30) NOT NULL DEFAULT 'SEAT' COMMENT '좌석 객체 종류(SEAT, TABLE, BOOTH, GENERAL_ADMISSION)',
    section_name VARCHAR(50) NULL COMMENT '좌석 구역명',
    row_label VARCHAR(20) NULL COMMENT '좌석 열 표기',
    seat_number VARCHAR(20) NULL COMMENT '좌석 번호 표기',
    capacity INT NOT NULL DEFAULT 1 COMMENT '수용 가능 인원',
    accessible_seat TINYINT(1) NOT NULL DEFAULT 0 COMMENT '장애인 이용 가능 좌석 여부',
    blocked_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '기본 판매 제한 여부',
    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '좌석 사용 여부',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_chart_position (
        seat_chart_id,
        section_name,
        row_label,
        seat_number
    ),
    UNIQUE KEY uk_seat_chart_seatsio_object (seat_chart_id, seatsio_object_key),
    CONSTRAINT fk_seat_chart
        FOREIGN KEY (seat_chart_id) REFERENCES seat_charts (id),
    CONSTRAINT fk_seat_grade
        FOREIGN KEY (seat_grade_id) REFERENCES seat_grades (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='좌석도에 배치된 물리 좌석 및 객체';

CREATE TABLE performance_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공연 회차 번호',
    performance_id BIGINT NOT NULL COMMENT '공연 번호',
    venue_hall_id BIGINT NOT NULL COMMENT '공연이 열리는 홀 번호',
    seat_chart_id BIGINT NOT NULL COMMENT '회차에 적용할 좌석도 번호',
    round_number INT NULL COMMENT '공연 회차 순서',
    starts_at DATETIME(6) NOT NULL COMMENT '공연 시작 일시',
    sales_open_at DATETIME(6) NOT NULL COMMENT '예매 시작 일시',
    sales_close_at DATETIME(6) NOT NULL COMMENT '예매 종료 일시',
    cancel_close_at DATETIME(6) NULL COMMENT '예매 취소 마감 일시',
    max_tickets_per_member INT NOT NULL DEFAULT 4 COMMENT '회원당 최대 구매 매수',
    seatsio_event_key VARCHAR(100) NULL COMMENT 'Seats.io 이벤트 식별자',
    status VARCHAR(20) NOT NULL COMMENT '회차 상태(SCHEDULED, OPEN, CLOSED, CANCELLED)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_seatsio_event_key (seatsio_event_key),
    UNIQUE KEY uk_schedule_performance_start (
        performance_id,
        venue_hall_id,
        starts_at
    ),
    KEY idx_schedule_performance_start (performance_id, starts_at),
    CONSTRAINT fk_schedule_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id),
    CONSTRAINT fk_schedule_venue_hall
        FOREIGN KEY (venue_hall_id) REFERENCES venue_halls (id),
    CONSTRAINT fk_schedule_seat_chart
        FOREIGN KEY (seat_chart_id) REFERENCES seat_charts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='예매 가능한 공연 날짜 및 회차';

CREATE TABLE schedule_seats (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '회차 좌석 재고 번호',
    schedule_id BIGINT NOT NULL COMMENT '공연 회차 번호',
    seat_id BIGINT NOT NULL COMMENT '물리 좌석 번호',
    price DECIMAL(12,2) NOT NULL COMMENT '회차별 실제 판매 가격',
    currency VARCHAR(3) NOT NULL DEFAULT 'KRW' COMMENT '통화 코드',
    status VARCHAR(20) NOT NULL COMMENT '좌석 판매 상태(AVAILABLE, HELD, RESERVED, BLOCKED)',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '동시성 제어 버전',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_seat (schedule_id, seat_id),
    KEY idx_schedule_seat_status (schedule_id, status),
    CONSTRAINT fk_schedule_seat_schedule
        FOREIGN KEY (schedule_id) REFERENCES performance_schedules (id),
    CONSTRAINT fk_schedule_seat_seat
        FOREIGN KEY (seat_id) REFERENCES seats (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공연 회차별 좌석 가격 및 판매 재고';

CREATE TABLE seat_holds (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '좌석 선점 번호',
    schedule_id BIGINT NOT NULL COMMENT '공연 회차 번호',
    member_id BIGINT NOT NULL COMMENT '좌석을 선점한 회원 번호',
    hold_token_hash VARCHAR(64) NOT NULL COMMENT '좌석 선점 토큰 해시',
    status VARCHAR(20) NOT NULL COMMENT '선점 상태(ACTIVE, BOOKED, RELEASED, EXPIRED)',
    started_at DATETIME(6) NOT NULL COMMENT '선점 시작 일시',
    expires_at DATETIME(6) NOT NULL COMMENT '선점 만료 일시',
    released_at DATETIME(6) NULL COMMENT '선점 해제 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_hold_token_hash (hold_token_hash),
    KEY idx_seat_hold_expiration (status, expires_at),
    KEY idx_seat_hold_owner (schedule_id, member_id, status),
    CONSTRAINT fk_seat_hold_schedule
        FOREIGN KEY (schedule_id) REFERENCES performance_schedules (id),
    CONSTRAINT fk_seat_hold_member
        FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원의 회차별 임시 좌석 선점';

CREATE TABLE seat_hold_items (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '좌석 선점 항목 번호',
    seat_hold_id BIGINT NOT NULL COMMENT '좌석 선점 번호',
    schedule_seat_id BIGINT NOT NULL COMMENT '선점한 회차 좌석 번호',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_hold_item (seat_hold_id, schedule_seat_id),
    KEY idx_seat_hold_item_schedule_seat (schedule_seat_id),
    CONSTRAINT fk_seat_hold_item_hold
        FOREIGN KEY (seat_hold_id) REFERENCES seat_holds (id),
    CONSTRAINT fk_seat_hold_item_schedule_seat
        FOREIGN KEY (schedule_seat_id) REFERENCES schedule_seats (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='하나의 선점에 포함된 회차 좌석 목록';

CREATE TABLE reservations (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '예약 번호',
    booking_number VARCHAR(30) NOT NULL COMMENT '사용자에게 표시할 예매번호',
    member_id BIGINT NOT NULL COMMENT '예매 회원 번호',
    schedule_id BIGINT NOT NULL COMMENT '예매한 공연 회차 번호',
    seat_hold_id BIGINT NULL COMMENT '예매에 사용한 좌석 선점 번호',
    status VARCHAR(20) NOT NULL COMMENT '예약 상태(PENDING, RESERVED, CANCELLED, EXPIRED)',
    seat_amount DECIMAL(12,2) NOT NULL COMMENT '좌석 금액 합계',
    fee_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '예매 수수료',
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '할인 금액',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '최종 결제 금액',
    expires_at DATETIME(6) NOT NULL COMMENT '결제 대기 만료 일시',
    reserved_at DATETIME(6) NULL COMMENT '예약 확정 일시',
    cancelled_at DATETIME(6) NULL COMMENT '예약 취소 일시',
    cancel_reason VARCHAR(200) NULL COMMENT '예약 취소 사유',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_booking_number (booking_number),
    KEY idx_reservation_member_created (member_id, created_at),
    KEY idx_reservation_status_created (status, created_at),
    CONSTRAINT fk_reservation_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_reservation_schedule
        FOREIGN KEY (schedule_id) REFERENCES performance_schedules (id),
    CONSTRAINT fk_reservation_seat_hold
        FOREIGN KEY (seat_hold_id) REFERENCES seat_holds (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원의 공연 예매 및 주문';

CREATE TABLE reservation_seats (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '예약 좌석 번호',
    reservation_id BIGINT NOT NULL COMMENT '예약 번호',
    schedule_seat_id BIGINT NOT NULL COMMENT '예매한 회차 좌석 번호',
    captured_section_name VARCHAR(50) NOT NULL COMMENT '예매 당시 좌석 구역명',
    captured_row_label VARCHAR(20) NOT NULL COMMENT '예매 당시 좌석 열',
    captured_seat_number VARCHAR(20) NOT NULL COMMENT '예매 당시 좌석 번호',
    captured_grade_name VARCHAR(30) NOT NULL COMMENT '예매 당시 좌석 등급명',
    captured_unit_price DECIMAL(12,2) NOT NULL COMMENT '예매 당시 좌석 단가',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_schedule_seat (reservation_id, schedule_seat_id),
    KEY idx_reservation_seat_schedule_seat (schedule_seat_id),
    CONSTRAINT fk_reservation_seat_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_reservation_seat_schedule_seat
        FOREIGN KEY (schedule_seat_id) REFERENCES schedule_seats (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='예매 시점의 좌석 정보와 가격 스냅샷';

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제 시도 번호',
    reservation_id BIGINT NOT NULL COMMENT '결제 대상 예약 번호',
    provider VARCHAR(30) NOT NULL COMMENT '결제 제공자(MOCK)',
    idempotency_key VARCHAR(100) NOT NULL COMMENT '중복 결제 방지 멱등키',
    amount DECIMAL(12,2) NOT NULL COMMENT '결제 요청 금액',
    status VARCHAR(20) NOT NULL COMMENT '결제 상태(READY, SUCCESS, FAILED, CANCELLED)',
    failure_code VARCHAR(50) NULL COMMENT '결제 실패 코드',
    requested_at DATETIME(6) NOT NULL COMMENT '결제 요청 일시',
    processed_at DATETIME(6) NULL COMMENT '결제 처리 완료 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_provider_idempotency (provider, idempotency_key),
    KEY idx_payment_reservation_status (reservation_id, status),
    CONSTRAINT fk_payment_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='예약별 결제 시도 및 결과';

CREATE TABLE waiting_queue_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '대기열 이력 번호',
    schedule_id BIGINT NOT NULL COMMENT '공연 회차 번호',
    member_id BIGINT NOT NULL COMMENT '대기 회원 번호',
    queue_token_hash VARCHAR(64) NOT NULL COMMENT '대기열 토큰 해시',
    status VARCHAR(20) NOT NULL COMMENT '대기열 상태(WAITING, ENTERED, EXPIRED)',
    joined_at DATETIME(6) NOT NULL COMMENT '대기열 진입 일시',
    entered_at DATETIME(6) NULL COMMENT '좌석 선택 입장 일시',
    expired_at DATETIME(6) NULL COMMENT '대기열 권한 만료 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_waiting_queue_token_hash (queue_token_hash),
    KEY idx_waiting_queue_schedule_status (schedule_id, status),
    KEY idx_waiting_queue_schedule_member_status (schedule_id, member_id, status),
    KEY idx_waiting_queue_schedule_status_joined (schedule_id, status, joined_at, id),
    CONSTRAINT fk_waiting_queue_schedule
        FOREIGN KEY (schedule_id) REFERENCES performance_schedules (id),
    CONSTRAINT fk_waiting_queue_member
        FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Redis 대기열 상태 전이 이력';

CREATE TABLE ai_chat_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI 대화 이력 번호',
    member_id BIGINT NULL COMMENT '질문한 회원 번호',
    request_type VARCHAR(30) NOT NULL COMMENT 'AI 요청 종류',
    question TEXT NOT NULL COMMENT '사용자 질문',
    extracted_condition JSON NULL COMMENT 'AI가 추출한 검색 조건',
    answer TEXT NULL COMMENT 'AI 답변',
    provider VARCHAR(30) NOT NULL COMMENT '사용한 AI 제공자',
    success TINYINT(1) NOT NULL COMMENT 'AI 처리 성공 여부',
    error_code VARCHAR(50) NULL COMMENT 'AI 처리 오류 코드',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '요청 일시',
    PRIMARY KEY (id),
    KEY idx_ai_chat_success_created (success, created_at),
    CONSTRAINT fk_ai_chat_member
        FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 추천 및 FAQ 요청 이력';

-- Optional catalog details. Core performance lookup does not depend on these rows.
CREATE TABLE performance_assets (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공연 이미지 번호',
    performance_id BIGINT NOT NULL COMMENT '공연 번호',
    asset_type VARCHAR(30) NOT NULL COMMENT '이미지 종류(POSTER, INTRO_IMAGE)',
    url VARCHAR(1000) NOT NULL COMMENT '이미지 URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '화면 표시 순서',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    KEY idx_performance_asset_order (performance_id, asset_type, sort_order),
    CONSTRAINT fk_performance_asset_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공연 포스터 및 소개 이미지 (안 써도 되는 테이블)';

CREATE TABLE performance_credits (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공연 관계자 번호',
    performance_id BIGINT NOT NULL COMMENT '공연 번호',
    credit_type VARCHAR(30) NOT NULL COMMENT '관계자 구분(CAST, CREW, PRODUCER, HOST, ORGANIZER)',
    name VARCHAR(150) NOT NULL COMMENT '출연진·제작진·단체 이름',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '화면 표시 순서',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    KEY idx_performance_credit_order (performance_id, credit_type, sort_order),
    CONSTRAINT fk_performance_credit_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공연 출연진·제작진·주최·주관 정보 (안 써도 되는 테이블)';

CREATE TABLE performance_outlets (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '외부 예매처 번호',
    performance_id BIGINT NOT NULL COMMENT '공연 번호',
    outlet_name VARCHAR(100) NOT NULL COMMENT '외부 예매처 이름',
    outlet_url VARCHAR(1000) NOT NULL COMMENT '외부 예매 페이지 URL',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    KEY idx_performance_outlet_performance (performance_id),
    CONSTRAINT fk_performance_outlet_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공연별 외부 예매처 연결 정보 (안 써도 되는 테이블)';

-- Optional performance-level price policy. ScheduleSeat.price remains the authoritative sale price.
CREATE TABLE performance_price_tiers (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공연 가격 등급 번호',
    performance_id BIGINT NOT NULL COMMENT '공연 번호',
    provider_category_key VARCHAR(100) NULL COMMENT 'Seats.io 카테고리 식별자',
    name VARCHAR(50) NOT NULL COMMENT '가격 등급명',
    display_color VARCHAR(7) NULL COMMENT '화면 표시 색상',
    ticket_type VARCHAR(50) NULL COMMENT '티켓 유형(일반, 청소년 등)',
    price DECIMAL(12,2) NOT NULL COMMENT '가격 등급 판매 가격',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_performance_price_tier_name (performance_id, name, ticket_type),
    CONSTRAINT fk_performance_price_tier_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='선택 기능용 공연별 가격 정책 (안 써도 되는 테이블)';

-- Optional refund history. Reservation cancellation does not require a refund row.
CREATE TABLE refunds (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '환불 번호',
    payment_id BIGINT NOT NULL COMMENT '환불 대상 결제 번호',
    amount DECIMAL(12,2) NOT NULL COMMENT '환불 금액',
    status VARCHAR(20) NOT NULL COMMENT '환불 처리 상태',
    reason VARCHAR(200) NULL COMMENT '환불 사유',
    requested_at DATETIME(6) NOT NULL COMMENT '환불 요청 일시',
    processed_at DATETIME(6) NULL COMMENT '환불 완료 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    KEY idx_refund_payment_status (payment_id, status),
    CONSTRAINT fk_refund_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='선택 기능용 결제 환불 이력 (안 써도 되는 테이블)';

-- Optional digital ticket. Booking completion can use booking_number without this row.
CREATE TABLE tickets (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '티켓 번호',
    reservation_seat_id BIGINT NOT NULL COMMENT '티켓이 발급된 예약 좌석 번호',
    ticket_number VARCHAR(30) NOT NULL COMMENT '예약의 booking_number와 동일한 티켓번호',
    status VARCHAR(20) NOT NULL COMMENT '티켓 상태(ISSUED, USED, CANCELLED)',
    qr_token_hash VARCHAR(64) NULL COMMENT 'QR 인증 토큰 해시',
    issued_at DATETIME(6) NOT NULL COMMENT '티켓 발급 일시',
    used_at DATETIME(6) NULL COMMENT '입장 처리 일시',
    cancelled_at DATETIME(6) NULL COMMENT '티켓 취소 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_reservation_seat (reservation_seat_id),
    KEY idx_ticket_number (ticket_number),
    UNIQUE KEY uk_ticket_qr_token_hash (qr_token_hash),
    CONSTRAINT fk_ticket_reservation_seat
        FOREIGN KEY (reservation_seat_id) REFERENCES reservation_seats (id),
    CONSTRAINT fk_ticket_booking_number
        FOREIGN KEY (ticket_number) REFERENCES reservations (booking_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='선택 기능용 모바일 및 QR 티켓 (안 써도 되는 테이블)';

-- Optional raw external API archive. Parsed performance rows remain independently usable.
CREATE TABLE external_source_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '외부 원본 스냅샷 번호',
    source_name VARCHAR(30) NOT NULL COMMENT '외부 데이터 제공처 이름',
    resource_type VARCHAR(30) NOT NULL COMMENT '외부 리소스 종류',
    external_id VARCHAR(100) NOT NULL COMMENT '외부 시스템 리소스 식별자',
    raw_payload JSON NOT NULL COMMENT '외부 API 원본 응답',
    checksum VARCHAR(64) NULL COMMENT '원본 변경 확인용 체크섬',
    sync_status VARCHAR(20) NOT NULL COMMENT '동기화 처리 상태',
    fetched_at DATETIME(6) NOT NULL COMMENT '외부 데이터 수집 일시',
    error_message TEXT NULL COMMENT '동기화 오류 내용',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    PRIMARY KEY (id),
    KEY idx_external_snapshot_resource (
        source_name,
        resource_type,
        external_id,
        fetched_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='KOPIS 등 외부 API 원본 데이터 보관 (안 써도 되는 테이블)';
