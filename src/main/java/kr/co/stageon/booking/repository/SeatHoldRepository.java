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

    /** 대시보드 - 아직 유효기간이 남은 선점 좌석(ACTIVE) 수 */
    long countByStatusAndExpiresAtAfter(SeatHold.Status status, LocalDateTime now);

    //특정 회차에 유저가 선점/예매 완료한 좌석의 총개수를 반환
    int countByMemberIdAndScheduleIdAndStatusIn(Long memberId, Long scheduleId, List<SeatHold.Status> statuses);
}