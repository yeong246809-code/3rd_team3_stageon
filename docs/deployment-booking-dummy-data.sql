-- 배포 화면 확인용 예매·예매좌석·결제 더미 데이터입니다.
-- 기존 활성 일반 회원만 사용하며 대기열과 AI 대화 테이블은 변경하지 않습니다.
-- 예약 완료 좌석만 RESERVED 처리하고 취소 예약의 좌석은 AVAILABLE 상태로 유지합니다.

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_booking_seed_plan (
    booking_number VARCHAR(30) NOT NULL,
    member_email VARCHAR(190) NOT NULL,
    schedule_id BIGINT NOT NULL,
    seat_count INT NOT NULL,
    reservation_status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    receive_method VARCHAR(20) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    PRIMARY KEY (booking_number)
);

INSERT INTO tmp_booking_seed_plan (
    booking_number, member_email, schedule_id, seat_count,
    reservation_status, payment_status, receive_method, requested_at
)
VALUES
    ('STG-SEED-260807-01', 'kceuni0405@gmail.com', 183, 2, 'RESERVED',  'SUCCESS',   'MOBILE', '2026-08-07 11:20:00'),
    ('STG-SEED-260808-01', 'kceuni0405@gmail.com', 160, 1, 'RESERVED',  'SUCCESS',   'ONSITE', '2026-08-08 14:10:00'),
    ('STG-SEED-260809-01', 'kceuni0405@gmail.com', 164, 1, 'CANCELLED', 'CANCELLED', 'MOBILE', '2026-08-09 10:30:00'),

    ('STG-SEED-260807-02', 'user1@stageon.test',    152, 2, 'RESERVED',  'SUCCESS',   'MOBILE', '2026-08-07 13:40:00'),
    ('STG-SEED-260809-02', 'user1@stageon.test',    163, 1, 'RESERVED',  'SUCCESS',   'MOBILE', '2026-08-09 16:15:00'),
    ('STG-SEED-260811-01', 'user1@stageon.test',    173, 2, 'RESERVED',  'SUCCESS',   'ONSITE', '2026-08-11 12:05:00'),

    ('STG-SEED-260808-02', 'user2@stageon.test',    153, 1, 'RESERVED',  'SUCCESS',   'MOBILE', '2026-08-08 18:25:00'),
    ('STG-SEED-260810-01', 'user2@stageon.test',    174, 2, 'CANCELLED', 'CANCELLED', 'MOBILE', '2026-08-10 15:45:00'),
    ('STG-SEED-260812-01', 'user2@stageon.test',    184, 2, 'RESERVED',  'SUCCESS',   'MOBILE', '2026-08-12 09:35:00'),

    ('STG-SEED-260809-03', 'suah.nam@gmail.com',    165, 2, 'RESERVED',  'SUCCESS',   'MOBILE', '2026-08-09 19:10:00'),
    ('STG-SEED-260811-02', 'suah.nam@gmail.com',    175, 1, 'RESERVED',  'SUCCESS',   'ONSITE', '2026-08-11 17:50:00'),
    ('STG-SEED-260812-02', 'suah.nam@gmail.com',    161, 2, 'RESERVED',  'SUCCESS',   'MOBILE', '2026-08-12 20:20:00');

-- 이미 생성된 예매번호는 다시 선택하지 않아 재실행 시 중복 생성을 막습니다.
CREATE TEMPORARY TABLE tmp_new_booking_plan AS
SELECT plan.*
FROM tmp_booking_seed_plan plan
JOIN members member
  ON member.email = plan.member_email
 AND member.role = 'USER'
 AND member.status = 'ACTIVE'
JOIN performance_schedules schedule
  ON schedule.id = plan.schedule_id
WHERE NOT EXISTS (
    SELECT 1
    FROM reservations reservation
    WHERE reservation.booking_number = plan.booking_number
);

-- 각 회차에서 아직 판매 가능한 좌석을 필요한 수만큼 고릅니다.
CREATE TEMPORARY TABLE tmp_booking_seed_seats AS
SELECT selected.*
FROM (
    SELECT
        plan.booking_number,
        plan.member_email,
        plan.schedule_id,
        plan.seat_count,
        plan.reservation_status,
        plan.payment_status,
        plan.receive_method,
        plan.requested_at,
        schedule_seat.id AS schedule_seat_id,
        schedule_seat.price,
        ROW_NUMBER() OVER (
            PARTITION BY plan.booking_number
            ORDER BY grade.sort_order, seat.id
        ) AS seat_order
    FROM tmp_new_booking_plan plan
    JOIN schedule_seats schedule_seat
      ON schedule_seat.schedule_id = plan.schedule_id
     AND schedule_seat.status = 'AVAILABLE'
    JOIN seats seat
      ON seat.id = schedule_seat.seat_id
    JOIN seat_grades grade
      ON grade.id = seat.seat_grade_id
) selected
WHERE selected.seat_order <= selected.seat_count;

-- 필요한 좌석 수를 모두 확보한 예매만 생성합니다.
CREATE TEMPORARY TABLE tmp_valid_booking_plan AS
SELECT
    plan.*,
    SUM(selected.price) AS seat_amount
FROM tmp_new_booking_plan plan
JOIN tmp_booking_seed_seats selected
  ON selected.booking_number = plan.booking_number
GROUP BY
    plan.booking_number, plan.member_email, plan.schedule_id, plan.seat_count,
    plan.reservation_status, plan.payment_status, plan.receive_method, plan.requested_at
HAVING COUNT(*) = plan.seat_count;

INSERT INTO reservations (
    booking_number,
    member_id,
    schedule_id,
    ticket_count,
    seat_hold_id,
    status,
    receive_method,
    seat_amount,
    fee_amount,
    discount_amount,
    total_amount,
    expires_at,
    reserved_at,
    cancelled_at,
    cancel_reason,
    created_at,
    updated_at
)
SELECT
    plan.booking_number,
    member.id,
    plan.schedule_id,
    plan.seat_count,
    NULL,
    plan.reservation_status,
    plan.receive_method,
    plan.seat_amount,
    2000.00,
    0.00,
    plan.seat_amount + 2000.00,
    TIMESTAMPADD(MINUTE, 15, plan.requested_at),
    CASE WHEN plan.reservation_status = 'RESERVED' THEN TIMESTAMPADD(MINUTE, 2, plan.requested_at) ELSE TIMESTAMPADD(MINUTE, 2, plan.requested_at) END,
    CASE WHEN plan.reservation_status = 'CANCELLED' THEN TIMESTAMPADD(DAY, 1, plan.requested_at) ELSE NULL END,
    CASE WHEN plan.reservation_status = 'CANCELLED' THEN '배포 화면 확인용 취소 예매' ELSE NULL END,
    plan.requested_at,
    CASE
        WHEN plan.reservation_status = 'CANCELLED' THEN TIMESTAMPADD(DAY, 1, plan.requested_at)
        ELSE TIMESTAMPADD(MINUTE, 2, plan.requested_at)
    END
FROM tmp_valid_booking_plan plan
JOIN members member ON member.email = plan.member_email;

INSERT INTO reservation_seats (
    reservation_id,
    schedule_seat_id,
    status,
    captured_section_name,
    captured_row_label,
    captured_seat_number,
    captured_grade_name,
    captured_unit_price,
    created_at
)
SELECT
    reservation.id,
    selected.schedule_seat_id,
    CASE WHEN plan.reservation_status = 'CANCELLED' THEN 'CANCELLED' ELSE 'RESERVED' END,
    seat.section_name,
    seat.row_label,
    seat.seat_number,
    grade.name,
    selected.price,
    plan.requested_at
FROM tmp_valid_booking_plan plan
JOIN reservations reservation
  ON reservation.booking_number = plan.booking_number
JOIN tmp_booking_seed_seats selected
  ON selected.booking_number = plan.booking_number
JOIN schedule_seats schedule_seat
  ON schedule_seat.id = selected.schedule_seat_id
JOIN seats seat
  ON seat.id = schedule_seat.seat_id
JOIN seat_grades grade
  ON grade.id = seat.seat_grade_id
WHERE NOT EXISTS (
    SELECT 1
    FROM reservation_seats existing
    WHERE existing.reservation_id = reservation.id
      AND existing.schedule_seat_id = selected.schedule_seat_id
);

INSERT INTO payments (
    reservation_id,
    provider,
    order_id,
    payment_key,
    pay_method,
    amount,
    cancel_amount,
    status,
    failure_code,
    requested_at,
    processed_at,
    created_at,
    updated_at
)
SELECT
    reservation.id,
    'TOSSPAYMENTS',
    CONCAT('SEED-PAY-', plan.booking_number),
    CONCAT('dummy_payment_key_', plan.booking_number),
    'CARD',
    plan.seat_amount + 2000.00,
    CASE WHEN plan.payment_status = 'CANCELLED' THEN plan.seat_amount + 2000.00 ELSE 0.00 END,
    plan.payment_status,
    NULL,
    plan.requested_at,
    CASE
        WHEN plan.payment_status = 'CANCELLED' THEN TIMESTAMPADD(DAY, 1, plan.requested_at)
        ELSE TIMESTAMPADD(MINUTE, 2, plan.requested_at)
    END,
    plan.requested_at,
    CASE
        WHEN plan.payment_status = 'CANCELLED' THEN TIMESTAMPADD(DAY, 1, plan.requested_at)
        ELSE TIMESTAMPADD(MINUTE, 2, plan.requested_at)
    END
FROM tmp_valid_booking_plan plan
JOIN reservations reservation
  ON reservation.booking_number = plan.booking_number
WHERE NOT EXISTS (
    SELECT 1
    FROM payments existing
    WHERE existing.order_id = CONCAT('SEED-PAY-', plan.booking_number)
);

-- 확정 예매에 들어간 좌석만 판매 완료로 변경합니다.
UPDATE schedule_seats schedule_seat
JOIN tmp_booking_seed_seats selected
  ON selected.schedule_seat_id = schedule_seat.id
JOIN tmp_valid_booking_plan plan
  ON plan.booking_number = selected.booking_number
SET schedule_seat.status = 'RESERVED',
    schedule_seat.version = schedule_seat.version + 1
WHERE plan.reservation_status = 'RESERVED'
  AND schedule_seat.status = 'AVAILABLE';

DROP TEMPORARY TABLE tmp_valid_booking_plan;
DROP TEMPORARY TABLE tmp_booking_seed_seats;
DROP TEMPORARY TABLE tmp_new_booking_plan;
DROP TEMPORARY TABLE tmp_booking_seed_plan;

COMMIT;
