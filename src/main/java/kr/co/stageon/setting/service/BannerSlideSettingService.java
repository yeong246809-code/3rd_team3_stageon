package kr.co.stageon.setting.service;

import kr.co.stageon.setting.domain.SiteSetting;
import kr.co.stageon.setting.repository.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 배너 슬라이드 자동 전환 간격 등 배너 관련 전역 설정을 관리합니다. */
@Service
@RequiredArgsConstructor
public class BannerSlideSettingService {

    public static final String SLIDE_INTERVAL_KEY = "banner_slide_interval_seconds";
    private static final int DEFAULT_SLIDE_INTERVAL_SECONDS = 3;
    private static final int MIN_SECONDS = 1;
    private static final int MAX_SECONDS = 30;

    private final SiteSettingRepository siteSettingRepository;

    @Transactional(readOnly = true)
    public int getSlideIntervalSeconds() {
        return siteSettingRepository.findById(SLIDE_INTERVAL_KEY)
                .map(SiteSetting::getValue)
                .map(Integer::parseInt)
                .orElse(DEFAULT_SLIDE_INTERVAL_SECONDS);
    }

    @Transactional
    public void updateSlideIntervalSeconds(int seconds) {
        if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
            throw new IllegalArgumentException("전환 간격은 " + MIN_SECONDS + "~" + MAX_SECONDS + "초 사이로 입력해주세요.");
        }
        SiteSetting setting = siteSettingRepository.findById(SLIDE_INTERVAL_KEY)
                .orElseGet(() -> SiteSetting.create(SLIDE_INTERVAL_KEY, String.valueOf(DEFAULT_SLIDE_INTERVAL_SECONDS)));
        setting.changeValue(String.valueOf(seconds));
        siteSettingRepository.save(setting);
    }
}