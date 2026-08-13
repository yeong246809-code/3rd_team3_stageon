package kr.co.stageon.member.repository;

import kr.co.stageon.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // AD10 관리자 회원 목록: 이름/이메일 검색 + 권한/상태 필터 + 페이지네이션
    @Query("""
            SELECT m FROM Member m
            WHERE (:role IS NULL OR m.role = :role)
              AND (:status IS NULL OR m.status = :status)
              AND (:keyword IS NULL OR m.name LIKE CONCAT('%', :keyword, '%')
                   OR m.email LIKE CONCAT('%', :keyword, '%'))
            ORDER BY m.id DESC
            """)
    Page<Member> search(
            @Param("role") Member.Role role,
            @Param("status") Member.Status status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}