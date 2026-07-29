package kr.co.stageon.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** kr.co.stageon.admin.web 패키지의 모든 컨트롤러에 현재 요청 경로를 자동으로 심어줍니다. */
@ControllerAdvice(basePackages = "kr.co.stageon.admin.web")
public class AdminModelAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}