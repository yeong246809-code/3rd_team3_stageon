-- 포트폴리오 화면용 대표 공연장·홀 데이터입니다.
-- 기존 블루스퀘어/신한카드홀은 유지하며 좌석 배치도·좌석·회차는 변경하지 않습니다.

START TRANSACTION;

INSERT INTO venues (
    kopis_facility_id, name, address, region,
    latitude, longitude, phone, homepage_url
)
SELECT NULL, '더 서울라이티움 (갤러리아포레)', '상세 주소 미제공', '서울', NULL, NULL, NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM venues
    WHERE name = '더 서울라이티움 (갤러리아포레)' AND region = '서울'
);

INSERT INTO venues (
    kopis_facility_id, name, address, region,
    latitude, longitude, phone, homepage_url
)
SELECT NULL, '달밤엔씨어터', '상세 주소 미제공', '서울', NULL, NULL, NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM venues
    WHERE name = '달밤엔씨어터' AND region = '서울'
);

INSERT INTO venues (
    kopis_facility_id, name, address, region,
    latitude, longitude, phone, homepage_url
)
SELECT NULL, 'entry55 [사당]', '상세 주소 미제공', '서울', NULL, NULL, NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM venues
    WHERE name = 'entry55 [사당]' AND region = '서울'
);

INSERT INTO venues (
    kopis_facility_id, name, address, region,
    latitude, longitude, phone, homepage_url
)
SELECT NULL, '스테이지엠 (STAGE M) (구.프란츠홀)', '상세 주소 미제공', '대구', NULL, NULL, NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM venues
    WHERE name = '스테이지엠 (STAGE M) (구.프란츠홀)' AND region = '대구'
);

SET @lightium_venue_id = (
    SELECT id FROM venues
    WHERE name = '더 서울라이티움 (갤러리아포레)' AND region = '서울'
    ORDER BY id LIMIT 1
);
SET @dalbam_venue_id = (
    SELECT id FROM venues
    WHERE name = '달밤엔씨어터' AND region = '서울'
    ORDER BY id LIMIT 1
);
SET @entry55_venue_id = (
    SELECT id FROM venues
    WHERE name = 'entry55 [사당]' AND region = '서울'
    ORDER BY id LIMIT 1
);
SET @stagem_venue_id = (
    SELECT id FROM venues
    WHERE name = '스테이지엠 (STAGE M) (구.프란츠홀)' AND region = '대구'
    ORDER BY id LIMIT 1
);

INSERT INTO venue_halls (
    venue_id, kopis_hall_id, name,
    seat_capacity, accessible_seat_count, active
)
SELECT @lightium_venue_id, NULL, 'G층 전용관', 800, 0, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM venue_halls
    WHERE venue_id = @lightium_venue_id AND name = 'G층 전용관'
);

INSERT INTO venue_halls (
    venue_id, kopis_hall_id, name,
    seat_capacity, accessible_seat_count, active
)
SELECT @dalbam_venue_id, NULL, '달밤엔씨어터', 109, 0, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM venue_halls
    WHERE venue_id = @dalbam_venue_id AND name = '달밤엔씨어터'
);

INSERT INTO venue_halls (
    venue_id, kopis_hall_id, name,
    seat_capacity, accessible_seat_count, active
)
SELECT @entry55_venue_id, NULL, 'entry55 [사당]', 88, 0, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM venue_halls
    WHERE venue_id = @entry55_venue_id AND name = 'entry55 [사당]'
);

INSERT INTO venue_halls (
    venue_id, kopis_hall_id, name,
    seat_capacity, accessible_seat_count, active
)
SELECT @stagem_venue_id, NULL, '스테이지 엠 (STAGE M)', 75, 0, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM venue_halls
    WHERE venue_id = @stagem_venue_id AND name = '스테이지 엠 (STAGE M)'
);

SET @lightium_hall_id = (
    SELECT id FROM venue_halls
    WHERE venue_id = @lightium_venue_id AND name = 'G층 전용관'
    ORDER BY id LIMIT 1
);
SET @dalbam_hall_id = (
    SELECT id FROM venue_halls
    WHERE venue_id = @dalbam_venue_id AND name = '달밤엔씨어터'
    ORDER BY id LIMIT 1
);
SET @entry55_hall_id = (
    SELECT id FROM venue_halls
    WHERE venue_id = @entry55_venue_id AND name = 'entry55 [사당]'
    ORDER BY id LIMIT 1
);
SET @stagem_hall_id = (
    SELECT id FROM venue_halls
    WHERE venue_id = @stagem_venue_id AND name = '스테이지 엠 (STAGE M)'
    ORDER BY id LIMIT 1
);

-- 공연장 수를 5개로 제한하기 위해 장르별 대표 홀에 연결합니다.
-- 뮤지컬 중 원본이 달밤엔씨어터인 공연은 달밤엔씨어터를 우선 사용합니다.
UPDATE performances
SET venue_hall_id = CASE
    WHEN genre = '뮤지컬' AND raw_schedule_text LIKE '%달밤엔씨어터%' THEN @dalbam_hall_id
    WHEN genre = '뮤지컬' THEN @lightium_hall_id
    WHEN genre = '연극' THEN @dalbam_hall_id
    WHEN genre = '콘서트' THEN @entry55_hall_id
    WHEN genre = '클래식/무용' THEN @stagem_hall_id
    WHEN genre = '행사' THEN @lightium_hall_id
    ELSE venue_hall_id
END
WHERE story = '엑셀 공연DB를 바탕으로 구성한 포트폴리오 시연용 공연입니다.';

COMMIT;
