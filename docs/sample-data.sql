-- StageOn 화면·조회 API 확인용 최소 데이터입니다.
-- spring.sql.init.mode=never이므로 애플리케이션이 자동 실행하지 않습니다.
-- 공유 DB에 적용하기 전 팀원과 데이터 범위를 확인하세요.

START TRANSACTION;

INSERT INTO venues (name, address, region, total_seat_count)
SELECT '블루스퀘어 신한카드홀', '서울특별시 용산구 이태원로 294', '서울', 3
WHERE NOT EXISTS (
    SELECT 1 FROM venues WHERE name = '블루스퀘어 신한카드홀'
);

SET @venue_id = (
    SELECT id FROM venues WHERE name = '블루스퀘어 신한카드홀' ORDER BY id LIMIT 1
);

INSERT INTO seat_grades (venue_id, name, display_color, sort_order)
SELECT @venue_id, 'VIP', '#E8513D', 1
WHERE NOT EXISTS (
    SELECT 1 FROM seat_grades WHERE venue_id = @venue_id AND name = 'VIP'
);

SET @grade_id = (
    SELECT id FROM seat_grades WHERE venue_id = @venue_id AND name = 'VIP' ORDER BY id LIMIT 1
);

INSERT INTO seats (venue_id, seat_grade_id, section_name, row_label, seat_number, accessible, blocked_default)
SELECT @venue_id, @grade_id, 'A구역', '8열', '10번', FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM seats WHERE venue_id = @venue_id AND section_name = 'A구역' AND row_label = '8열' AND seat_number = '10번'
);
INSERT INTO seats (venue_id, seat_grade_id, section_name, row_label, seat_number, accessible, blocked_default)
SELECT @venue_id, @grade_id, 'A구역', '8열', '11번', FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM seats WHERE venue_id = @venue_id AND section_name = 'A구역' AND row_label = '8열' AND seat_number = '11번'
);
INSERT INTO seats (venue_id, seat_grade_id, section_name, row_label, seat_number, accessible, blocked_default)
SELECT @venue_id, @grade_id, 'A구역', '8열', '12번', TRUE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM seats WHERE venue_id = @venue_id AND section_name = 'A구역' AND row_label = '8열' AND seat_number = '12번'
);

INSERT INTO performances (kopis_id, title, genre, poster_url, start_date, end_date, status, source_type)
VALUES ('STAGEON-SAMPLE-001', '뮤지컬 〈오페라의 유령〉', '뮤지컬', '/images/figma/detail-04.jpg',
        '2026-07-18', '2026-10-11', 'ON_SALE', 'LOCAL')
ON DUPLICATE KEY UPDATE title = VALUES(title), poster_url = VALUES(poster_url);

SET @performance_id = (
    SELECT id FROM performances WHERE kopis_id = 'STAGEON-SAMPLE-001'
);

INSERT INTO performance_schedules
    (performance_id, venue_id, starts_at, sales_open_at, sales_close_at, status)
SELECT @performance_id, @venue_id, '2026-08-15 19:00:00.000000',
       '2026-07-25 14:00:00.000000', '2026-08-15 18:30:00.000000', 'OPEN'
WHERE NOT EXISTS (
    SELECT 1 FROM performance_schedules
    WHERE performance_id = @performance_id AND venue_id = @venue_id
      AND starts_at = '2026-08-15 19:00:00.000000'
);

SET @schedule_id = (
    SELECT id FROM performance_schedules
    WHERE performance_id = @performance_id AND venue_id = @venue_id
    ORDER BY starts_at LIMIT 1
);

INSERT INTO schedule_seats (schedule_id, seat_id, price, status, version)
SELECT @schedule_id, seat.id, 160000.00,
       IF(seat.blocked_default, 'BLOCKED', 'AVAILABLE'), 0
FROM seats seat
WHERE seat.venue_id = @venue_id
  AND NOT EXISTS (
      SELECT 1 FROM schedule_seats schedule_seat
      WHERE schedule_seat.schedule_id = @schedule_id
        AND schedule_seat.seat_id = seat.id
  );

COMMIT;
