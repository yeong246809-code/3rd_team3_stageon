package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 회원의 회차별 좌석 선점 묶음 DAO입니다. */
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {
    Optional<SeatHold> findByHoldTokenHash(String holdTokenHash);
    Optional<SeatHold> findFirstByScheduleIdAndMemberIdAndStatusOrderByStartedAtDesc(
            Long scheduleId,
            Long memberId,
            SeatHold.Status status
    );
    List<SeatHold> findByStatusAndExpiresAtBefore(SeatHold.Status status, LocalDateTime expiresAt);
}
