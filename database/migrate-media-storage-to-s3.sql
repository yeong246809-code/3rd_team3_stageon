-- 운영 DB에 한 번 실행하는 S3 미디어 저장소 마이그레이션입니다.
-- 기존 /uploads/... URL은 그대로 남겨 두며, 새로 업로드되는 파일부터 object key가 기록됩니다.

CREATE TABLE IF NOT EXISTS banners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500) NULL,
    image_url VARCHAR(500) NOT NULL,
    image_key VARCHAR(500) NULL,
    performance_id BIGINT NULL,
    link_url VARCHAR(500) NULL,
    period_start DATE NULL,
    period_end DATE NULL,
    badge_text VARCHAR(50) NULL,
    button1_text VARCHAR(30) NOT NULL,
    button1_url VARCHAR(500) NULL,
    button2_text VARCHAR(30) NOT NULL,
    button2_url VARCHAR(500) NULL,
    display_order INT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_banner_display (is_active, display_order),
    CONSTRAINT fk_banner_performance FOREIGN KEY (performance_id) REFERENCES performances (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS site_settings (
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @add_poster_key = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'performances' AND column_name = 'poster_key'),
    'SELECT 1',
    'ALTER TABLE performances ADD COLUMN poster_key VARCHAR(500) NULL AFTER poster_url'
);
PREPARE statement FROM @add_poster_key;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @add_image_key = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'banners' AND column_name = 'image_key'),
    'SELECT 1',
    'ALTER TABLE banners ADD COLUMN image_key VARCHAR(500) NULL AFTER image_url'
);
PREPARE statement FROM @add_image_key;
EXECUTE statement;
DEALLOCATE PREPARE statement;
