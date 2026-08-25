package kr.co.stageon.admin.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.stageon.admin.auth.AdminAuthController;
import kr.co.stageon.admin.domain.AdminActivityLog;
import kr.co.stageon.admin.repository.AdminActivityLogRepository;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * @AdminLoggable 이 붙은 메서드가 예외 없이 성공 완료된 경우에만 admin_activity_logs에 기록.
 * 로그인 세션이 없으면(=관리자 컨텍스트 밖에서 호출된 경우) 기록하지 않고 조용히 스킵합니다.
 * 로깅 자체에서 오류가 나더라도 원래 비즈니스 로직에는 영향을 주지 않도록 예외를 삼킵니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminActivityLogAspect {

    private final AdminActivityLogRepository adminActivityLogRepository;
    private final MemberRepository memberRepository;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @AfterReturning(pointcut = "@annotation(adminLoggable)", returning = "result")
    public void logActivity(JoinPoint joinPoint, AdminLoggable adminLoggable, Object result) {
        try {
            Long adminId = resolveCurrentAdminId();
            if (adminId == null) {
                return;
            }
            String adminEmail = memberRepository.findById(adminId)
                    .map(Member::getEmail)
                    .orElse(null);

            StandardEvaluationContext context = buildEvaluationContext(joinPoint, result);

            Long targetId = evalTargetId(adminLoggable.targetId(), context);
            String description = evalDescription(adminLoggable.description(), context);

            AdminActivityLog activityLog = AdminActivityLog.create(
                    adminId, adminEmail, adminLoggable.actionType(), adminLoggable.targetEntity(),
                    targetId, description
            );
            adminActivityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.warn("관리자 활동 로그 기록 실패 (비즈니스 로직에는 영향 없음): {}", e.getMessage());
        }
    }

    private Long resolveCurrentAdminId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object adminId = session.getAttribute(AdminAuthController.SESSION_KEY_ADMIN);
        return (adminId instanceof Long) ? (Long) adminId : null;
    }

    private StandardEvaluationContext buildEvaluationContext(JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        context.setVariable("result", result);
        return context;
    }

    private Long evalTargetId(String expression, StandardEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = parser.parseExpression(expression).getValue(context);
        if (value == null) {
            return null;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private String evalDescription(String expression, StandardEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = parser.parseExpression(expression).getValue(context);
        return value != null ? value.toString() : null;
    }
}