package kr.co.stageon.setting.repository;

import kr.co.stageon.setting.domain.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
}