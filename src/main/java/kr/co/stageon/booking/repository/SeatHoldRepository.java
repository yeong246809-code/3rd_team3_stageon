package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    //List<SeatHold> findByStatusAndExpiresAtBefore(SeatHold.Status status, LocalDateTime expiresAt);

    /** 대시보드 - 아직 유효기간이 남은 선점 좌석(ACTIVE) 수 */
    long countByStatusAndExpiresAtAfter(SeatHold.Status status, LocalDateTime now);

    //특정 회차에 유저가 선점/예매 완료한 좌석의 총개수를 반환
    int countByMemberIdAndScheduleIdAndStatusIn(Long memberId, Long scheduleId, List<SeatHold.Status> statuses);

    // ✅ List로 변경
    Optional<SeatHold> findFirstByMemberIdAndScheduleIdAndStatusOrderByStartedAtDesc(Long memberId, Long scheduleId, SeatHold.Status status);

    // 만료 시간이 현재 시간보다 이전이고, 상태가 ACTIVE인 선점 내역 조회
    List<SeatHold> findByStatusAndExpiresAtBefore(SeatHold.Status status, LocalDateTime now);

    /** AD08 좌석 재고 현황 - 특정 회차의 활성 선점 목록을 만료 임박 순으로 조회합니다(회원 정보 fetch join). */
    @Query("SELECT sh FROM SeatHold sh " +
            "JOIN FETCH sh.member m " +
            "WHERE sh.schedule.id = :scheduleId AND sh.status = :status " +
            "ORDER BY sh.expiresAt ASC")
    List<SeatHold> findActiveHoldsByScheduleId(@Param("scheduleId") Long scheduleId,
                                               @Param("status") SeatHold.Status status);
}