package kr.co.stageon.admin.dto;

/** GitHub Actions 배포 완료 후 자동 버전 등록 웹훅 요청 바디입니다. */
public record VersionWebhookRequestDto(
        String title,
        String author,
        String commitMessage
) {
}