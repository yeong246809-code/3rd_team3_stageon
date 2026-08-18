package kr.co.stageon.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사이트 전역 설정을 key-value로 저장합니다(예: 배너 슬라이드 전환 간격). */
@Getter
@Entity
@Table(name = "site_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteSetting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String value;

    public static SiteSetting create(String key, String value) {
        SiteSetting s = new SiteSetting();
        s.key = key;
        s.value = value;
        return s;
    }

    public void changeValue(String value) {
        this.value = value;
    }
}