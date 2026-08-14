-- 기존 tickets 테이블의 ticket_number를 reservations.booking_number와 동일하게
-- 사용하고 외래키로 연결합니다.
-- 한 예매에 좌석별 티켓이 여러 장 존재할 수 있으므로 ticket_number는 UNIQUE가 아닙니다.

ALTER TABLE tickets
    DROP INDEX uk_ticket_number;

UPDATE tickets t
JOIN reservation_seats rs ON rs.id = t.reservation_seat_id
JOIN reservations r ON r.id = rs.reservation_id
SET t.ticket_number = r.booking_number;

ALTER TABLE tickets
    MODIFY COLUMN ticket_number VARCHAR(30) NOT NULL
        COMMENT '예약의 booking_number와 동일한 티켓번호',
    ADD KEY idx_ticket_number (ticket_number),
    ADD CONSTRAINT fk_ticket_booking_number
        FOREIGN KEY (ticket_number) REFERENCES reservations (booking_number);
