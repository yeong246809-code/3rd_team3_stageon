package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.BannerFormDto;
import kr.co.stageon.admin.dto.BannerListItemDto;
import kr.co.stageon.banner.domain.Banner;
import kr.co.stageon.banner.repository.BannerRepository;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/** AD "배너 관리" 화면의 목록, 등록, 수정, 삭제, 순서변경, 노출토글을 담당합니다. */
@Service
@RequiredArgsConstructor
public class AdminBannerService {

    /** performance-form.html 포스터 업로드와 동일한 정적 리소스 루트(/uploads/**)를 사용합니다. */
    private static final String UPLOAD_DIR = "uploads/banners";
    private static final String UPLOAD_URL_PREFIX = "/uploads/banners/";

    private final BannerRepository bannerRepository;
    private final PerformanceRepository performanceRepository;

    @Transactional(readOnly = true)
    public List<BannerListItemDto> getList() {
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAscIdAsc();
        int last = banners.size() - 1;
        return java.util.stream.IntStream.range(0, banners.size())
                .mapToObj(i -> {
                    Banner b = banners.get(i);
                    return new BannerListItemDto(
                            b.getId(), b.getTitle(), b.getImageUrl(),
                            b.getPeriodStartText(), b.getPeriodEndText(),
                            b.getDisplayOrder(), b.isActive(),
                            i == 0, i == last,
                            b.getPerformance() != null ? b.getPerformance().getTitle() : null
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BannerFormDto getForm(Long id) {
        Banner b = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너입니다."));
        BannerFormDto dto = new BannerFormDto();
        dto.setId(b.getId());
        dto.setTitle(b.getTitle());
        dto.setDescription(b.getDescription());
        dto.setImageUrl(b.getImageUrl());
        dto.setPerformanceId(b.getPerformance() != null ? b.getPerformance().getId() : null);
        dto.setLinkUrl(b.getLinkUrl());
        dto.setPeriodStartText(b.getPeriodStartText());
        dto.setPeriodEndText(b.getPeriodEndText());
        dto.setBadgeText(b.getBadgeText());
        dto.setButton1Text(b.getButton1Text());
        dto.setButton1Url(b.getButton1Url());
        dto.setButton2Text(b.getButton2Text());
        dto.setButton2Url(b.getButton2Url());
        dto.setActive(b.isActive());
        return dto;
    }

    @Transactional
    public void create(BannerFormDto form) {
        String imageUrl = resolveImageUrl(form);
        Performance performance = resolvePerformance(form.getPerformanceId());
        int nextOrder = bannerRepository.findAllByOrderByDisplayOrderAscIdAsc().size();

        Banner banner = Banner.create(
                form.getTitle(), form.getDescription(), imageUrl, performance,
                form.getLinkUrl(), form.getPeriodStartText(), form.getPeriodEndText(), form.getBadgeText(),
                blankToDefault(form.getButton1Text(), "실시간 예매하기"), form.getButton1Url(),
                blankToDefault(form.getButton2Text(), "자세히 보기"), form.getButton2Url(),
                nextOrder, form.isActive()
        );
        bannerRepository.save(banner);
    }

    @Transactional
    public void update(Long id, BannerFormDto form) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너입니다."));

        String imageUrl = resolveImageUrl(form);
        Performance performance = resolvePerformance(form.getPerformanceId());

        banner.update(
                form.getTitle(), form.getDescription(), imageUrl, performance,
                form.getLinkUrl(), form.getPeriodStartText(), form.getPeriodEndText(), form.getBadgeText(),
                blankToDefault(form.getButton1Text(), "실시간 예매하기"), form.getButton1Url(),
                blankToDefault(form.getButton2Text(), "자세히 보기"), form.getButton2Url(),
                form.isActive()
        );
    }

    @Transactional
    public void delete(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 배너입니다.");
        }
        bannerRepository.deleteById(id);
        reorderSequentially();
    }

    @Transactional
    public void toggleActive(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너입니다."));
        banner.toggleActive();
    }

    /** 순서를 한 칸 위(더 앞 슬라이드)로 옮깁니다. */
    @Transactional
    public void moveUp(Long id) {
        swapWithNeighbor(id, -1);
    }

    /** 순서를 한 칸 아래(더 뒤 슬라이드)로 옮깁니다. */
    @Transactional
    public void moveDown(Long id) {
        swapWithNeighbor(id, 1);
    }

    private void swapWithNeighbor(Long id, int direction) {
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAscIdAsc();
        int index = -1;
        for (int i = 0; i < banners.size(); i++) {
            if (banners.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        int targetIndex = index + direction;
        if (index < 0 || targetIndex < 0 || targetIndex >= banners.size()) {
            return;
        }
        Banner current = banners.get(index);
        Banner target = banners.get(targetIndex);
        int currentOrder = current.getDisplayOrder();
        current.changeOrder(target.getDisplayOrder());
        target.changeOrder(currentOrder);
    }

    private void reorderSequentially() {
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAscIdAsc();
        for (int i = 0; i < banners.size(); i++) {
            banners.get(i).changeOrder(i);
        }
    }

    private Performance resolvePerformance(Long performanceId) {
        if (performanceId == null) {
            return null;
        }
        return performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다."));
    }

    private String blankToDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /** performance-form.html 포스터 업로드와 동일한 방식으로 파일을 저장합니다. 새 파일이 없으면 기존 URL을 유지합니다. */
    private String resolveImageUrl(BannerFormDto form) {
        MultipartFile file = form.getBannerFile();
        if (file == null || file.isEmpty()) {
            if (form.getImageUrl() == null || form.getImageUrl().isBlank()) {
                throw new IllegalArgumentException("배너 이미지를 등록해주세요.");
            }
            return form.getImageUrl();
        }

        try {
            Path uploadPath = Path.of(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf('.'));
            }
            String savedName = UUID.randomUUID() + extension;

            Path target = uploadPath.resolve(savedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return UPLOAD_URL_PREFIX + savedName;
        } catch (IOException e) {
            throw new UncheckedIOException("배너 이미지 업로드에 실패했습니다.", e);
        }
    }
}