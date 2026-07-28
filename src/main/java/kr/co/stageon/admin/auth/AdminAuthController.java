package kr.co.stageon.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    public static final String SESSION_KEY_ADMIN = "loginAdmin";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthController(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public String login(@RequestParam(value = "email", required = false) String email,
                        @RequestParam(value = "password", required = false) String password,
                        HttpServletRequest request) {

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return "redirect:/admin/login?error=empty";
        }

        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        boolean invalid = memberOpt.isEmpty()
                || memberOpt.get().getRole() != Member.Role.ADMIN
                || !passwordEncoder.matches(password, memberOpt.get().getPasswordHash());

        if (invalid) {
            return "redirect:/admin/login?error=invalid";
        }

        Member member = memberOpt.get();
        if (member.getStatus() != Member.Status.ACTIVE) {
            return "redirect:/admin/login?error=inactive";
        }

        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_KEY_ADMIN, member.getId());
        session.setAttribute(SESSION_KEY_ADMIN + ":name", member.getName());

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/admin/login";
    }
}