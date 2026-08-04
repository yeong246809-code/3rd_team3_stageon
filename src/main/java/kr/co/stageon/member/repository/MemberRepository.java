package kr.co.stageon.member.repository;

import kr.co.stageon.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 회원 데이터 접근을 담당합니다.
 */
public interface MemberRepository
        extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    // 하이픈과 공백을 제거한 전화번호로 중복 확인
    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM members
                    WHERE REPLACE(REPLACE(phone, '-', ''), ' ', '') = :phone
                    """,
            nativeQuery = true
    )
    long countByNormalizedPhone(@Param("phone") String phone);

    // 이름과 휴대전화 번호가 모두 일치하는 활성 회원 조회
    @Query(
            value = """
                    SELECT *
                    FROM members
                    WHERE name = :name
                      AND REPLACE(REPLACE(phone, '-', ''), ' ', '') = :phone
                      AND status = 'ACTIVE'
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<Member> findActiveMemberByNameAndNormalizedPhone(
            @Param("name") String name,
            @Param("phone") String phone
    );
}