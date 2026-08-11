package kr.co.stageon.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

/**
 * 전역 에러 진입점입니다("/error").
 * 요청 경로가 "/admin"으로 시작하면 관리자 전용 에러 화면(admin/error, 로봇 캐릭터)을 보여주고,
 * 그 외(일반 사용자) 경로는 Spring Boot 기본 에러 처리(BasicErrorController)를 그대로 따릅니다.
 * 404(핸들러 없음), 500(예외 발생) 등 서블릿 컨테이너가 "/error"로 포워딩하는 모든 상태코드를 처리합니다.
 */
@Controller
@RequestMapping("${spring.web.error.path:${error.path:/error}}")
public class AdminErrorController extends BasicErrorController {

    public AdminErrorController(ErrorAttributes errorAttributes,
                                WebProperties webProperties,
                                List<ErrorViewResolver> errorViewResolvers) {
        super(errorAttributes, webProperties.getError(), errorViewResolvers);
    }

    @Override
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        String originalPath = getOriginalRequestUri(request);

        if (originalPath != null && originalPath.startsWith("/admin")) {
            HttpStatus status = getStatus(request);
            Map<String, Object> body = getErrorAttributes(request, ErrorAttributeOptions.defaults());

            response.setStatus(status.value());

            ModelAndView mv = new ModelAndView("admin/error");
            mv.addObject("status", status.value());
            mv.addObject("errorTitle", resolveTitle(status));
            mv.addObject("errorMessage", resolveMessage(status));
            mv.addObject("path", body.getOrDefault("path", originalPath));
            return mv;
        }

        return super.errorHtml(request, response);
    }

    private String getOriginalRequestUri(HttpServletRequest request) {
        Object uri = request.getAttribute("jakarta.servlet.error.request_uri");
        return uri != null ? uri.toString() : request.getRequestURI();
    }

    private String resolveTitle(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "이 공연은 무대에 없어요";
            case FORBIDDEN -> "관람이 제한된 구역이에요";
            case UNAUTHORIZED -> "입장 전에 확인이 필요해요";
            default -> "무대에 문제가 생겼어요";
        };
    }

    private String resolveMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "요청하신 페이지를 찾을 수 없습니다. 주소를 다시 확인해주세요.";
            case FORBIDDEN -> "이 페이지에 접근할 권한이 없습니다.";
            case UNAUTHORIZED -> "로그인이 필요한 페이지입니다.";
            default -> "잠시 후 다시 시도해주세요. 문제가 계속되면 관리자에게 문의해주세요.";
        };
    }
}