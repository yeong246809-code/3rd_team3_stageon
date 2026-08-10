-- 5개 대표 홀에 좌석도와 회차별 좌석 재고를 구성합니다.
-- 홀마다 VIP/R/S 3등급, 등급마다 4좌석(총 12좌석)을 사용합니다.
-- Seats.io 차트·이벤트를 실제로 생성하지 않았으므로 외부 식별자는 NULL로 둡니다.

START TRANSACTION;

-- 좌석도가 없는 홀에 기본 좌석도 1개를 생성합니다.
INSERT INTO seat_charts (
    venue_hall_id, name, seatsio_chart_key, version, active
)
SELECT
    hall.id,
    CONCAT(hall.name, ' 기본 좌석도'),
    NULL,
    1,
    TRUE
FROM venue_halls hall
WHERE NOT EXISTS (
    SELECT 1
    FROM seat_charts chart
    WHERE chart.venue_hall_id = hall.id
      AND chart.active = TRUE
);

-- 모든 활성 좌석도에 VIP/R/S 등급을 보장합니다.
INSERT INTO seat_grades (
    seat_chart_id, name, display_color, sort_order
)
SELECT chart.id, grade.name, grade.display_color, grade.sort_order
FROM seat_charts chart
CROSS JOIN (
    SELECT 'VIP' AS name, '#E8513D' AS display_color, 1 AS sort_order
    UNION ALL SELECT 'R', '#F2A93B', 2
    UNION ALL SELECT 'S', '#4C8BF5', 3
) grade
WHERE chart.active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM seat_grades existing
      WHERE existing.seat_chart_id = chart.id
        AND existing.name = grade.name
  );

-- 좌석이 전혀 없는 신규 좌석도에는 등급별 4좌석을 생성합니다.
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
SELECT
    chart.id,
    grade.id,
    NULL,
    'SEAT',
    CONCAT(grade.name, '구역'),
    CASE grade.name WHEN 'VIP' THEN 'A열' WHEN 'R' THEN 'B열' ELSE 'C열' END,
    CONCAT(numbers.number, '번'),
    1,
    FALSE,
    FALSE,
    TRUE
FROM seat_charts chart
JOIN seat_grades grade ON grade.seat_chart_id = chart.id
CROSS JOIN (
    SELECT 1 AS number
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
) numbers
WHERE chart.active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM seats existing
      WHERE existing.seat_chart_id = chart.id
  );

-- 기존 블루스퀘어 좌석도는 각 등급의 네 번째 좌석만 추가합니다.
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
SELECT
    chart.id,
    grade.id,
    NULL,
    'SEAT',
    CASE grade.name WHEN 'VIP' THEN 'A구역' WHEN 'R' THEN 'B구역' ELSE 'C구역' END,
    CASE grade.name WHEN 'VIP' THEN '8열' WHEN 'R' THEN '5열' ELSE '2열' END,
    CASE grade.name WHEN 'VIP' THEN '13번' ELSE '4번' END,
    1,
    FALSE,
    FALSE,
    TRUE
FROM seat_charts chart
JOIN venue_halls hall ON hall.id = chart.venue_hall_id
JOIN venues venue ON venue.id = hall.venue_id
JOIN seat_grades grade ON grade.seat_chart_id = chart.id
WHERE venue.name = '블루스퀘어'
  AND hall.name = '신한카드홀'
  AND grade.name IN ('VIP', 'R', 'S')
  AND NOT EXISTS (
      SELECT 1
      FROM seats existing
      WHERE existing.seat_chart_id = chart.id
        AND existing.seat_grade_id = grade.id
        AND existing.row_label = CASE grade.name WHEN 'VIP' THEN '8열' WHEN 'R' THEN '5열' ELSE '2열' END
        AND existing.seat_number = CASE grade.name WHEN 'VIP' THEN '13번' ELSE '4번' END
  );

-- 공연별 가격 등급을 생성합니다. 기존 가격 등급은 유지합니다.
INSERT INTO performance_price_tiers (
    performance_id,
    provider_category_key,
    name,
    display_color,
    ticket_type,
    price
)
SELECT
    performance.id,
    NULL,
    grade.name,
    grade.display_color,
    NULL,
    CASE grade.name
        WHEN 'VIP' THEN ROUND(performance.base_price * 1.5, -3)
        WHEN 'R' THEN ROUND(performance.base_price * 1.2, -3)
        ELSE performance.base_price
    END
FROM performances performance
CROSS JOIN (
    SELECT 'VIP' AS name, '#E8513D' AS display_color
    UNION ALL SELECT 'R', '#F2A93B'
    UNION ALL SELECT 'S', '#4C8BF5'
) grade
WHERE performance.base_price IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM performance_price_tiers existing
      WHERE existing.performance_id = performance.id
        AND existing.name = grade.name
  );

-- 신규 공연마다 1회차를 배정합니다. 장르별로 날짜를 나눠 홀 충돌을 피합니다.
CREATE TEMPORARY TABLE tmp_showcase_schedule_plan AS
SELECT
    ranked.id AS performance_id,
    ranked.venue_hall_id,
    chart.id AS seat_chart_id,
    CASE ranked.genre
        WHEN '뮤지컬' THEN TIMESTAMPADD(DAY, ranked.genre_rank - 1, '2026-08-20 19:00:00')
        WHEN '연극' THEN TIMESTAMPADD(DAY, ranked.genre_rank - 1, '2026-09-01 19:30:00')
        WHEN '콘서트' THEN TIMESTAMPADD(DAY, ranked.genre_rank - 1, '2026-09-15 19:00:00')
        WHEN '클래식/무용' THEN TIMESTAMPADD(DAY, ranked.genre_rank - 1, '2026-10-01 19:00:00')
        WHEN '행사' THEN TIMESTAMP(ranked.start_date, '14:00:00')
        ELSE TIMESTAMPADD(DAY, ranked.genre_rank - 1, '2026-11-01 19:00:00')
    END AS starts_at,
    ranked.genre_rank
FROM (
    SELECT
        performance.*,
        ROW_NUMBER() OVER (
            PARTITION BY performance.genre
            ORDER BY performance.id
        ) AS genre_rank
    FROM performances performance
    WHERE performance.story = '엑셀 공연DB를 바탕으로 구성한 포트폴리오 시연용 공연입니다.'
) ranked
JOIN seat_charts chart
  ON chart.venue_hall_id = ranked.venue_hall_id
 AND chart.active = TRUE;

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
SELECT
    plan.performance_id,
    plan.venue_hall_id,
    plan.seat_chart_id,
    1,
    plan.starts_at,
    TIMESTAMPADD(DAY, plan.genre_rank, CURRENT_TIMESTAMP(6)),
    TIMESTAMPADD(HOUR, -1, plan.starts_at),
    TIMESTAMPADD(DAY, -1, plan.starts_at),
    4,
    NULL,
    'SCHEDULED'
FROM tmp_showcase_schedule_plan plan
WHERE NOT EXISTS (
    SELECT 1
    FROM performance_schedules existing
    WHERE existing.performance_id = plan.performance_id
);

DROP TEMPORARY TABLE tmp_showcase_schedule_plan;

-- 각 회차에 좌석도의 12좌석을 연결하고 등급 가격을 적용합니다.
INSERT INTO schedule_seats (
    schedule_id,
    seat_id,
    price,
    currency,
    status,
    version
)
SELECT
    schedule.id,
    seat.id,
    price_tier.price,
    'KRW',
    CASE WHEN seat.blocked_default = TRUE THEN 'BLOCKED' ELSE 'AVAILABLE' END,
    0
FROM performance_schedules schedule
JOIN seats seat
  ON seat.seat_chart_id = schedule.seat_chart_id
 AND seat.active = TRUE
JOIN seat_grades grade
  ON grade.id = seat.seat_grade_id
JOIN performance_price_tiers price_tier
  ON price_tier.performance_id = schedule.performance_id
 AND price_tier.name = grade.name
WHERE NOT EXISTS (
    SELECT 1
    FROM schedule_seats existing
    WHERE existing.schedule_id = schedule.id
      AND existing.seat_id = seat.id
);

COMMIT;
