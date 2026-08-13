package kr.co.stageon.common.config;

import kr.co.stageon.member.service.MemberUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberUserDetailsService memberUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(memberUserDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authenticationProvider(authenticationProvider())

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/members/email-verification/**",
                                "/api/members/find-id",
                                "/api/members/password-reset/**",
                                "/api/seats/**",
                                "/api/payments/webhook"
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/signup/complete",
                                "/find-id",
                                "/password-reset",
                                "/api/members/check-email",
                                "/api/members/check-phone",
                                "/api/members/find-id",
                                "/api/members/email-verification/**",
                                "/api/members/password-reset/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error",
                                "/api/payments/webhook"
                        ).permitAll()

                        .requestMatchers(
                                "/booking/**",
                                "/api/waiting-queue-history",
                                "/api/waiting-queue/**"
                        ).hasAnyRole("USER", "ADMIN")

                        .requestMatchers("/mypage/**")
                        .hasAnyRole("USER", "ADMIN")

                        .anyRequest().permitAll()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendRedirect(
                                                "/login?required"
                                        )
                        )
                );

        return http.build();
    }
}
