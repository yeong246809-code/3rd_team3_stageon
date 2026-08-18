package kr.co.stageon.version.repository;

import kr.co.stageon.version.domain.Version;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionRepository extends JpaRepository<Version, Long> {

    /** 목록 조회: 배포일 최신순, 동일 배포일이면 최근 등록순입니다. */
    List<Version> findAllByOrderByReleasedAtDescIdDesc();
}