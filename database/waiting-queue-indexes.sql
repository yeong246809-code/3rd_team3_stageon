-- 기존 운영 DB에 stageon-schema.sql의 대기열 인덱스를 반영할 때 한 번만 실행합니다.
ALTER TABLE waiting_queue_history
    ADD INDEX idx_waiting_queue_schedule_member_status (schedule_id, member_id, status),
    ADD INDEX idx_waiting_queue_schedule_status_joined (schedule_id, status, joined_at, id);
