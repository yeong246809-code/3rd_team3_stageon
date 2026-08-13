package kr.co.stageon.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 로그인 사용자와 관리자 계정을 나타냅니다.
 * 비밀번호 원문은 저장하지 않고 암호화된 값만 저장합니다.
 */
@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    public enum Role {
        USER,
        ADMIN
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        WITHDRAWN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 30)
    private String phone;

    // 성별
    @Column(length = 10)
    private String gender;

    // 생년월일
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    // 관리자 메모 (AD10에서 추가)
    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;

    // 일반 회원 생성
    public static Member createUser(
            String email,
            String passwordHash,
            String name,
            String phone,
            String gender,
            LocalDate birthDate
    ) {
        Member member = new Member();

        member.email = email;
        member.passwordHash = passwordHash;
        member.name = name;
        member.phone = phone;
        member.gender = gender;
        member.birthDate = birthDate;
        member.role = Role.USER;
        member.status = Status.ACTIVE;

        return member;
    }

    // 암호화된 새 비밀번호로 변경
    public void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
    }

    /**
     * 마이페이지에서 일반 회원정보를 수정합니다.
     *
     * 이메일은 로그인 아이디로 사용되기 때문에
     * 여기서는 변경하지 않습니다.
     */
    public void updateProfile(
            String name,
            String phone,
            String gender,
            LocalDate birthDate
    ) {

        // 이름 수정
        this.name = name;

        // 휴대전화 번호 수정
        this.phone = phone;

        // 성별 수정
        this.gender = gender;

        // 생년월일 수정
        this.birthDate = birthDate;
    }

    // 관리자 권한 변경 (AD10: 관리자 회원 관리 화면)
    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    // 관리자 메모 수정 (AD10: 관리자 회원 관리 화면)
    public void updateAdminMemo(String memo) {
        this.adminMemo = memo;
    }
}