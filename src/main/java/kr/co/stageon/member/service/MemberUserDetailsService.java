package kr.co.stageon.member.service;

import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String normalizedEmail =
                email == null ? "" : email.trim().toLowerCase();

        Member member = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "이메일 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        if (member.getStatus() != Member.Status.ACTIVE) {
            throw new UsernameNotFoundException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        return User.builder()
                .username(member.getEmail())
                .password(member.getPasswordHash())
                .authorities(List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + member.getRole().name()
                        )
                ))
                .build();
    }
}