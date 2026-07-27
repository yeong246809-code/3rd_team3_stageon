-- StageOn 화면·조회 API 확인용 최소 데이터입니다.
-- 애플리케이션이 자동 실행하지 않으며, database/stageon-schema.sql 적용 후 수동 실행합니다.

START TRANSACTION;

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
)
ON DUPLICATE KEY UPDATE
    display_color = VALUES(display_color),
    sort_order = VALUES(sort_order);

SET @grade_id = (
    SELECT id
    FROM seat_grades
    WHERE seat_chart_id = @seat_chart_id
      AND name = 'VIP'
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
        @grade_id,
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
        @grade_id,
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
        @grade_id,
        'A-8-12',
        'SEAT',
        'A구역',
        '8열',
        '12번',
        1,
        TRUE,
        FALSE,
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
    160000.00,
    'KRW',
    IF(seat.blocked_default, 'BLOCKED', 'AVAILABLE'),
    0
FROM seats seat
WHERE seat.seat_chart_id = @seat_chart_id
ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    currency = VALUES(currency),
    status = VALUES(status);

COMMIT;
