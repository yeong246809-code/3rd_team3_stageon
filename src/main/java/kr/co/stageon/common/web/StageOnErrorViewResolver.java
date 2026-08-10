package kr.co.stageon.common.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 브라우저 HTML 오류를 StageOn 공통 오류 화면으로 연결합니다. */
@Component
public class StageOnErrorViewResolver implements ErrorViewResolver {

    private static final Logger log = LoggerFactory.getLogger(StageOnErrorViewResolver.class);
    private static final String TRACE_ID_ATTRIBUTE = StageOnErrorViewResolver.class.getName() + ".traceId";
    private static final DateTimeFormatter TRACE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final boolean showFullLog;

    public StageOnErrorViewResolver(
            @Value("${stageon.error.show-full-log:false}") boolean showFullLog
    ) {
        this.showFullLog = showFullLog;
    }

    @Override
    public ModelAndView resolveErrorView(
            HttpServletRequest request,
            HttpStatus status,
            Map<String, Object> model
    ) {
        Throwable exception = findException(request);
        String traceId = resolveTraceId(request);
        String requestPath = resolveRequestPath(request, model);
        LocalDateTime occurredAt = LocalDateTime.now();

        writeServerLog(status, traceId, requestPath, exception);

        Map<String, Object> viewModel = new LinkedHashMap<>(model);
        viewModel.put("status", status.value());
        viewModel.put("errorCode", errorCode(status));
        viewModel.put("errorTitle", errorTitle(status));
        viewModel.put("errorDescription", errorDescription(status));
        viewModel.put("requestPath", requestPath);
        viewModel.put("traceId", traceId);
        viewModel.put("occurredAt", occurredAt.format(DISPLAY_TIME_FORMAT));
        viewModel.put("showFullLog", showFullLog);
        if (showFullLog) {
            viewModel.put("fullLog", buildFullLog(
                    status,
                    traceId,
                    requestPath,
                    occurredAt,
                    exception
            ));
        } else {
            viewModel.remove("trace");
            viewModel.remove("exception");
        }

        return new ModelAndView("user/error", viewModel, status);
    }

    private static Throwable findException(HttpServletRequest request) {
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        return exception instanceof Throwable throwable ? throwable : null;
    }

    private static String resolveTraceId(HttpServletRequest request) {
        Object existingTraceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
        if (existingTraceId instanceof String traceId && !traceId.isBlank()) {
            return traceId;
        }

        String randomSuffix = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        String traceId = "STG-" + LocalDateTime.now().format(TRACE_TIME_FORMAT) + "-" + randomSuffix;
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        return traceId;
    }

    private static String resolveRequestPath(HttpServletRequest request, Map<String, Object> model) {
        Object errorPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorPath instanceof String path && !path.isBlank()) {
            return path;
        }
        Object modelPath = model.get("path");
        return modelPath instanceof String path && !path.isBlank() ? path : request.getRequestURI();
    }

    private static String errorCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "PAGE_NOT_FOUND";
            case FORBIDDEN -> "ACCESS_DENIED";
            case UNAUTHORIZED -> "AUTHENTICATION_REQUIRED";
            case BAD_REQUEST -> "INVALID_REQUEST";
            case INTERNAL_SERVER_ERROR -> "INTERNAL_SERVER_ERROR";
            case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
            default -> status.is4xxClientError() ? "REQUEST_ERROR" : "SERVER_ERROR";
        };
    }

    private static String errorTitle(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "요청하신 페이지를 찾을 수 없어요";
            case FORBIDDEN, UNAUTHORIZED -> "이 페이지에 접근할 수 없어요";
            case INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
                    "잠시 무대가 멈췄어요";
            default -> "요청을 처리할 수 없어요";
        };
    }

    private static String errorDescription(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "주소가 변경됐거나 사용할 수 없는 페이지입니다.";
            case FORBIDDEN, UNAUTHORIZED -> "로그인 상태나 접근 권한을 확인해 주세요.";
            case INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
                    "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
            default -> "문제가 계속되면 아래 추적 번호와 함께 문의해 주세요.";
        };
    }

    private static String buildFullLog(
            HttpStatus status,
            String traceId,
            String requestPath,
            LocalDateTime occurredAt,
            Throwable exception
    ) {
        StringBuilder fullLog = new StringBuilder()
                .append(occurredAt).append(" ERROR StageOn error report\n")
                .append("Status       : ").append(status.value()).append(' ').append(status.name()).append('\n')
                .append("Request path : ").append(requestPath).append('\n')
                .append("Trace ID     : ").append(traceId).append("\n\n");

        if (exception == null) {
            fullLog.append("No exception stack trace was attached to this error dispatch.");
        } else {
            StringWriter stackTrace = new StringWriter();
            exception.printStackTrace(new PrintWriter(stackTrace));
            fullLog.append(stackTrace);
        }
        return maskSensitiveValues(fullLog.toString());
    }

    private static String maskSensitiveValues(String logText) {
        return logText
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [MASKED]")
                .replaceAll(
                        "(?i)(password|passwd|secret|token|authorization|cookie|session(?:id)?)"
                                + "(\\s*[:=]\\s*)[^\\s,;]+",
                        "$1$2[MASKED]"
                );
    }

    private static void writeServerLog(
            HttpStatus status,
            String traceId,
            String requestPath,
            Throwable exception
    ) {
        if (status.is5xxServerError()) {
            log.error("StageOn error traceId={} status={} path={}",
                    traceId, status.value(), requestPath, exception);
            return;
        }
        log.warn("StageOn error traceId={} status={} path={}",
                traceId, status.value(), requestPath);
    }
}
