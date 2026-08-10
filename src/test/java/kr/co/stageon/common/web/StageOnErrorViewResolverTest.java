package kr.co.stageon.common.web;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StageOnErrorViewResolverTest {

    @Test
    void resolvesBrowserErrorsToUserErrorViewWithoutProductionStackTrace() {
        var resolver = new StageOnErrorViewResolver(false);
        var request = errorRequest("/performances/9999", null);

        var result = resolver.resolveErrorView(request, HttpStatus.NOT_FOUND, Map.of());

        assertThat(result.getViewName()).isEqualTo("user/error");
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getModel()).containsEntry("errorCode", "PAGE_NOT_FOUND");
        assertThat(result.getModel()).containsEntry("requestPath", "/performances/9999");
        assertThat(result.getModel()).containsEntry("showFullLog", false);
        assertThat(result.getModel()).doesNotContainKeys("fullLog", "trace", "exception");
    }

    @Test
    void includesMaskedStackTraceOnlyWhenTestLogIsEnabled() {
        var resolver = new StageOnErrorViewResolver(true);
        var exception = new IllegalStateException("token=secret-token password=secret-password");
        var request = errorRequest("/test/error", exception);

        var result = resolver.resolveErrorView(request, HttpStatus.INTERNAL_SERVER_ERROR, Map.of());
        String fullLog = (String) result.getModel().get("fullLog");

        assertThat(result.getModel()).containsEntry("showFullLog", true);
        assertThat(fullLog)
                .contains("IllegalStateException")
                .contains("token=[MASKED]")
                .contains("password=[MASKED]")
                .doesNotContain("secret-token", "secret-password");
    }

    private MockHttpServletRequest errorRequest(String path, Throwable exception) {
        var request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path);
        if (exception != null) {
            request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception);
        }
        return request;
    }
}
