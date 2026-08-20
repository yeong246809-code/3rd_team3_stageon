package kr.co.stageon.version.web;

import kr.co.stageon.admin.dto.VersionListItemDto;
import kr.co.stageon.admin.service.AdminVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면(admin 포함) 렌더링 시 최신 배포 버전을 모델에 "latestVersion"으로 실어줍니다.
 * site-footer.html 프래그먼트가 이 값을 읽어 하단에 표시합니다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class FooterVersionAdvice {

    private final AdminVersionService adminVersionService;

    @ModelAttribute("latestVersion")
    public VersionListItemDto latestVersion() {
        return adminVersionService.getLatest();
    }
}