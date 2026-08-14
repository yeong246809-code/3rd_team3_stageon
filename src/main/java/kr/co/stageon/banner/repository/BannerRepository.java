package kr.co.stageon.banner.repository;

import kr.co.stageon.banner.domain.Banner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 배너 관리(관리자) 및 홈 화면 노출(회원)을 위한 DAO입니다. */
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderByDisplayOrderAscIdAsc();

    List<Banner> findByActiveTrueOrderByDisplayOrderAscIdAsc(Pageable pageable);
}