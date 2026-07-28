-- StageOn 화면·조회 API 확인용 최소 데이터입니다.
-- 애플리케이션이 자동 실행하지 않으며, database/stageon-schema.sql 적용 후 수동 실행합니다.

START TRANSACTION;

-- 테스트 계정입니다. 모든 계정의 비밀번호 원문은 1234이며 BCrypt 해시만 저장합니다.
-- 관리자 로그인 아이디: admin / 비밀번호: 1234
INSERT INTO members (
    email,
    password_hash,
    name,
    phone,
    role,
    status
)
VALUES
    (
        'admin',
        '$2a$10$80ZrdVMC8Bosicpj5K0B3.THTEFxTuBlo57NgOrkM4BJC7H5.2VRO',
        'StageOn 관리자',
        '010-0000-0000',
        'ADMIN',
        'ACTIVE'
    ),
    (
        'user1@stageon.test',
        '$2a$10$80ZrdVMC8Bosicpj5K0B3.THTEFxTuBlo57NgOrkM4BJC7H5.2VRO',
        '테스트 회원1',
        '010-1111-1111',
        'USER',
        'ACTIVE'
    ),
    (
        'user2@stageon.test',
        '$2a$10$80ZrdVMC8Bosicpj5K0B3.THTEFxTuBlo57NgOrkM4BJC7H5.2VRO',
        '테스트 회원2',
        '010-2222-2222',
        'USER',
        'ACTIVE'
    )
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    phone = VALUES(phone),
    role = VALUES(role),
    status = VALUES(status);

SET @test_user_id = (
    SELECT id
    FROM members
    WHERE email = 'user1@stageon.test'
);

SET @queue_user_id = (
    SELECT id
    FROM members
    WHERE email = 'user2@stageon.test'
);

INSERT INTO venues (
    kopis_facility_id,
    name,
    address,
    region
)
VALUES (
    NULL,
    '블루스퀘어',
    '서울특별시 용산구 이태원로 294',
    '서울'
)
ON DUPLICATE KEY UPDATE
    address = VALUES(address),
    region = VALUES(region);

SET @venue_id = (
    SELECT id
    FROM venues
    WHERE name = '블루스퀘어'
    ORDER BY id
    LIMIT 1
);

INSERT INTO venue_halls (
    venue_id,
    kopis_hall_id,
    name,
    seat_capacity,
    accessible_seat_count,
    active
)
VALUES (
    @venue_id,
    NULL,
    '신한카드홀',
    1766,
    3,
    TRUE
)
ON DUPLICATE KEY UPDATE
    seat_capacity = VALUES(seat_capacity),
    accessible_seat_count = VALUES(accessible_seat_count),
    active = VALUES(active);

SET @venue_hall_id = (
    SELECT id
    FROM venue_halls
    WHERE venue_id = @venue_id
      AND name = '신한카드홀'
    ORDER BY id
    LIMIT 1
);

INSERT INTO seat_charts (
    venue_hall_id,
    name,
    seatsio_chart_key,
    version,
    active
)
VALUES (
    @venue_hall_id,
    '신한카드홀 기본 좌석도',
    NULL,
    1,
    TRUE
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    active = VALUES(active);

SET @seat_chart_id = (
    SELECT id
    FROM seat_charts
    WHERE venue_hall_id = @venue_hall_id
      AND version = 1
    ORDER BY id
    LIMIT 1
);

INSERT INTO seat_grades (
    seat_chart_id,
    name,
    display_color,
    sort_order
)
VALUES (
    @seat_chart_id,
    'VIP',
    '#E8513D',
    1
),
(
    @seat_chart_id,
    'R',
    '#F2A93B',
    2
),
(
    @seat_chart_id,
    'S',
    '#4C8BF5',
    3
)
ON DUPLICATE KEY UPDATE
    display_color = VALUES(display_color),
    sort_order = VALUES(sort_order);

SET @vip_grade_id = (
    SELECT id
    FROM seat_grades
    WHERE seat_chart_id = @seat_chart_id
      AND name = 'VIP'
    ORDER BY id
    LIMIT 1
);

SET @r_grade_id = (
    SELECT id
    FROM seat_grades
    WHERE seat_chart_id = @seat_chart_id
      AND name = 'R'
    ORDER BY id
    LIMIT 1
);

SET @s_grade_id = (
    SELECT id
    FROM seat_grades
    WHERE seat_chart_id = @seat_chart_id
      AND name = 'S'
    ORDER BY id
    LIMIT 1
);

INSERT INTO seats (
    seat_chart_id,
    seat_grade_id,
    seatsio_object_key,
    object_type,
    section_name,
    row_label,
    seat_number,
    capacity,
    accessible_seat,
    blocked_default,
    active
)
VALUES
    (
        @seat_chart_id,
        @vip_grade_id,
        'A-8-10',
        'SEAT',
        'A구역',
        '8열',
        '10번',
        1,
        FALSE,
        FALSE,
        TRUE
    ),
    (
        @seat_chart_id,
        @vip_grade_id,
        'A-8-11',
        'SEAT',
        'A구역',
        '8열',
        '11번',
        1,
        FALSE,
        FALSE,
        TRUE
    ),
    (
        @seat_chart_id,
        @vip_grade_id,
        'A-8-12',
        'SEAT',
        'A구역',
        '8열',
        '12번',
        1,
        TRUE,
        FALSE,
        TRUE
    ),
    (
        @seat_chart_id,
        @r_grade_id,
        'B-5-1',
        'SEAT',
        'B구역',
        '5열',
        '1번',
        1,
        FALSE,
        FALSE,
        TRUE
    ),
    (
        @seat_chart_id,
        @r_grade_id,
        'B-5-2',
        'SEAT',
        'B구역',
        '5열',
        '2번',
        1,
        FALSE,
        FALSE,
        TRUE
    ),
    (
        @seat_chart_id,
        @r_grade_id,
        'B-5-3',
        'SEAT',
        'B구역',
        '5열',
        '3번',
        1,
        FALSE,
        FALSE,
        TRUE
    ),
    (
        @seat_chart_id,
        @s_grade_id,
        'C-2-1',
        'SEAT',
        'C구역',
        '2열',
        '1번',
        1,
        FALSE,
        FALSE,
        TRUE
    ),
    (
        @seat_chart_id,
        @s_grade_id,
        'C-2-2',
        'SEAT',
        'C구역',
        '2열',
        '2번',
        1,
        FALSE,
        FALSE,
        TRUE
    ),
    (
        @seat_chart_id,
        @s_grade_id,
        'C-2-3',
        'SEAT',
        'C구역',
        '2열',
        '3번',
        1,
        FALSE,
        TRUE,
        TRUE
    )
ON DUPLICATE KEY UPDATE
    seat_grade_id = VALUES(seat_grade_id),
    accessible_seat = VALUES(accessible_seat),
    blocked_default = VALUES(blocked_default),
    active = VALUES(active);

INSERT INTO performances (
    kopis_id,
    title,
    genre,
    poster_url,
    start_date,
    end_date,
    runtime_minutes,
    age_text,
    story,
    raw_price_text,
    raw_schedule_text,
    status
)
VALUES (
    'STAGEON-SAMPLE-001',
    '뮤지컬 〈오페라의 유령〉',
    '뮤지컬',
    '/images/figma/detail-04.jpg',
    '2026-07-18',
    '2026-10-11',
    150,
    '만 13세 이상',
    'StageOn 예매 흐름 확인을 위한 샘플 공연입니다.',
    'VIP석 160,000원',
    '금요일 19:00',
    'ON_SALE'
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    poster_url = VALUES(poster_url),
    runtime_minutes = VALUES(runtime_minutes),
    age_text = VALUES(age_text),
    story = VALUES(story),
    raw_price_text = VALUES(raw_price_text),
    raw_schedule_text = VALUES(raw_schedule_text),
    status = VALUES(status);

SET @performance_id = (
    SELECT id
    FROM performances
    WHERE kopis_id = 'STAGEON-SAMPLE-001'
);

INSERT INTO performance_schedules (
    performance_id,
    venue_hall_id,
    seat_chart_id,
    round_number,
    starts_at,
    sales_open_at,
    sales_close_at,
    cancel_close_at,
    max_tickets_per_member,
    seatsio_event_key,
    status
)
VALUES (
    @performance_id,
    @venue_hall_id,
    @seat_chart_id,
    1,
    '2026-08-15 19:00:00.000000',
    '2026-07-25 14:00:00.000000',
    '2026-08-15 18:30:00.000000',
    '2026-08-14 17:00:00.000000',
    4,
    NULL,
    'OPEN'
)
ON DUPLICATE KEY UPDATE
    sales_open_at = VALUES(sales_open_at),
    sales_close_at = VALUES(sales_close_at),
    cancel_close_at = VALUES(cancel_close_at),
    max_tickets_per_member = VALUES(max_tickets_per_member),
    status = VALUES(status);

SET @schedule_id = (
    SELECT id
    FROM performance_schedules
    WHERE performance_id = @performance_id
      AND venue_hall_id = @venue_hall_id
      AND starts_at = '2026-08-15 19:00:00.000000'
    ORDER BY id
    LIMIT 1
);

INSERT INTO schedule_seats (
    schedule_id,
    seat_id,
    price,
    currency,
    status,
    version
)
SELECT
    @schedule_id,
    seat.id,
    CASE grade.name
        WHEN 'VIP' THEN 160000.00
        WHEN 'R' THEN 130000.00
        ELSE 90000.00
    END,
    'KRW',
    IF(seat.blocked_default, 'BLOCKED', 'AVAILABLE'),
    0
FROM seats seat
JOIN seat_grades grade
  ON grade.id = seat.seat_grade_id
WHERE seat.seat_chart_id = @seat_chart_id
ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    currency = VALUES(currency),
    status = VALUES(status);

INSERT INTO performance_price_tiers (
    performance_id,
    provider_category_key,
    name,
    display_color,
    ticket_type,
    price
)
VALUES
    (@performance_id, NULL, 'VIP', '#E8513D', '일반', 160000.00),
    (@performance_id, NULL, 'R', '#F2A93B', '일반', 130000.00),
    (@performance_id, NULL, 'S', '#4C8BF5', '일반', 90000.00)
ON DUPLICATE KEY UPDATE
    display_color = VALUES(display_color),
    price = VALUES(price);

SET @reserved_schedule_seat_id = (
    SELECT schedule_seat.id
    FROM schedule_seats schedule_seat
    JOIN seats seat
      ON seat.id = schedule_seat.seat_id
    WHERE schedule_seat.schedule_id = @schedule_id
      AND seat.seatsio_object_key = 'A-8-10'
    LIMIT 1
);

INSERT INTO reservations (
    booking_number,
    member_id,
    schedule_id,
    seat_hold_id,
    status,
    seat_amount,
    fee_amount,
    discount_amount,
    total_amount,
    expires_at,
    reserved_at
)
VALUES (
    'STAGEON-TEST-0001',
    @test_user_id,
    @schedule_id,
    NULL,
    'RESERVED',
    160000.00,
    2000.00,
    0.00,
    162000.00,
    '2026-07-28 15:10:00.000000',
    '2026-07-28 15:05:00.000000'
)
ON DUPLICATE KEY UPDATE
    member_id = VALUES(member_id),
    schedule_id = VALUES(schedule_id),
    status = VALUES(status),
    seat_amount = VALUES(seat_amount),
    fee_amount = VALUES(fee_amount),
    discount_amount = VALUES(discount_amount),
    total_amount = VALUES(total_amount),
    expires_at = VALUES(expires_at),
    reserved_at = VALUES(reserved_at);

SET @reservation_id = (
    SELECT id
    FROM reservations
    WHERE booking_number = 'STAGEON-TEST-0001'
);

INSERT INTO reservation_seats (
    reservation_id,
    schedule_seat_id,
    captured_section_name,
    captured_row_label,
    captured_seat_number,
    captured_grade_name,
    captured_unit_price
)
SELECT
    @reservation_id,
    schedule_seat.id,
    seat.section_name,
    seat.row_label,
    seat.seat_number,
    grade.name,
    schedule_seat.price
FROM schedule_seats schedule_seat
JOIN seats seat
  ON seat.id = schedule_seat.seat_id
JOIN seat_grades grade
  ON grade.id = seat.seat_grade_id
WHERE schedule_seat.id = @reserved_schedule_seat_id
ON DUPLICATE KEY UPDATE
    captured_section_name = VALUES(captured_section_name),
    captured_row_label = VALUES(captured_row_label),
    captured_seat_number = VALUES(captured_seat_number),
    captured_grade_name = VALUES(captured_grade_name),
    captured_unit_price = VALUES(captured_unit_price);

INSERT INTO payments (
    reservation_id,
    provider,
    idempotency_key,
    amount,
    status,
    failure_code,
    requested_at,
    processed_at
)
VALUES (
    @reservation_id,
    'MOCK',
    'STAGEON-TEST-PAYMENT-0001',
    162000.00,
    'SUCCESS',
    NULL,
    '2026-07-28 15:04:00.000000',
    '2026-07-28 15:05:00.000000'
)
ON DUPLICATE KEY UPDATE
    reservation_id = VALUES(reservation_id),
    amount = VALUES(amount),
    status = VALUES(status),
    failure_code = VALUES(failure_code),
    requested_at = VALUES(requested_at),
    processed_at = VALUES(processed_at);

UPDATE schedule_seats
SET status = 'RESERVED'
WHERE id = @reserved_schedule_seat_id;

INSERT INTO waiting_queue_history (
    schedule_id,
    member_id,
    queue_token_hash,
    status,
    joined_at,
    entered_at,
    expired_at
)
VALUES (
    @schedule_id,
    @queue_user_id,
    SHA2('STAGEON-TEST-QUEUE-0001', 256),
    'ENTERED',
    '2026-07-28 14:55:00.000000',
    '2026-07-28 15:00:00.000000',
    NULL
)
ON DUPLICATE KEY UPDATE
    schedule_id = VALUES(schedule_id),
    member_id = VALUES(member_id),
    status = VALUES(status),
    joined_at = VALUES(joined_at),
    entered_at = VALUES(entered_at),
    expired_at = VALUES(expired_at);

COMMIT;
