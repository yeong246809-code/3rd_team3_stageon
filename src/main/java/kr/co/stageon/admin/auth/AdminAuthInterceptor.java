package kr.co.stageon.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AdminAuthController.SESSION_KEY_ADMIN) != null) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/admin/login?error=unauthorized");
        return false;
    }
}