package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.VersionWebhookRequestDto;
import kr.co.stageon.admin.service.AdminVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배포 파이프라인(cicd.yml)이 EC2 배포 성공 직후 호출하는 웹훅입니다.
 * 관리자 세션이 아니라 비밀키(X-Deploy-Secret 헤더)로 인증합니다.
 * 경로가 "/admin/**"이 아니므로 AdminAuthInterceptor의 세션 체크 대상이 아닙니다.
 */
@RestController
@RequestMapping("/api/deploy")
@RequiredArgsConstructor
public class VersionWebhookController {

    private final AdminVersionService adminVersionService;

    /** EC2 .env 등에 VERSION_WEBHOOK_SECRET로 설정한 값과 비교합니다. 미설정 시 항상 거부합니다. */
    @Value("${VERSION_WEBHOOK_SECRET:}")
    private String webhookSecret;

    @PostMapping("/version-hook")
    public ResponseEntity<?> registerVersion(@RequestHeader(value = "X-Deploy-Secret", required = false) String secret,
                                             @RequestBody VersionWebhookRequestDto body) {
        if (webhookSecret == null || webhookSecret.isBlank() || !webhookSecret.equals(secret)) {
            return ResponseEntity.status(401).body("unauthorized");
        }
        Long id = adminVersionService.registerFromDeploy(body.title(), body.author(), body.commitMessage());
        return ResponseEntity.ok().body(java.util.Map.of("id", id));
    }
}