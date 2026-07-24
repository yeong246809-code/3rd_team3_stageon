package kr.co.stageon.member.repository;

import kr.co.stageon.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 회원 영속성 접근을 담당하는 Spring Data JPA DAO입니다. */
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
}
