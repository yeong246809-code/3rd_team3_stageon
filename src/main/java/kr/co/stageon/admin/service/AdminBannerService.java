package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.BannerFormDto;
import kr.co.stageon.admin.dto.BannerListItemDto;
import kr.co.stageon.banner.domain.Banner;
import kr.co.stageon.banner.repository.BannerRepository;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import kr.co.stageon.common.file.ObjectStorageService;
import kr.co.stageon.common.file.StorageProperties;
import kr.co.stageon.common.file.StorageTransactionCleanup;
import kr.co.stageon.common.file.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

/** AD "배너 관리" 화면의 목록, 등록, 수정, 삭제, 순서변경, 노출토글을 담당합니다. */
@Service
@RequiredArgsConstructor
public class AdminBannerService {

    /** 버튼 링크 드롭다운의 "사전 정의 페이지" 옵션입니다. 폼의 select value -> 실제 이동 경로. */
    public static final Map<String, String> PREDEFINED_LINKS = Map.ofEntries(
            Map.entry("PERFORMANCES", "/performances"),
            Map.entry("GENRE_MUSICAL", "/performances?genre=뮤지컬"),
            Map.entry("GENRE_PLAY", "/performances?genre=연극"),
            Map.entry("GENRE_CONCERT", "/performances?genre=콘서트"),
            Map.entry("GENRE_CLASSIC", "/performances?genre=클래식/무용"),
            Map.entry("GENRE_EXHIBITION", "/performances?genre=전시/행사"),
            Map.entry("AI_RECOMMEND", "/ai-recommend"),
            Map.entry("CUSTOMER_CENTER", "/support")
    );

    private final BannerRepository bannerRepository;
    private final PerformanceRepository performanceRepository;
    private final ObjectStorageService storageService;
    private final StorageProperties storageProperties;
    private final StorageTransactionCleanup storageCleanup;

    @Transactional(readOnly = true)
    public List<BannerListItemDto> getList() {
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAscIdAsc();
        int last = banners.size() - 1;
        return java.util.stream.IntStream.range(0, banners.size())
                .mapToObj(i -> {
                    Banner b = banners.get(i);
                    return new BannerListItemDto(
                            b.getId(), b.getTitle(), b.getImageUrl(),
                            b.getPeriodStart(), b.getPeriodEnd(),
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
        dto.setPeriodStart(b.getPeriodStart());
        dto.setPeriodEnd(b.getPeriodEnd());
        dto.setBadgeText(b.getBadgeText());
        dto.setButton1Text(b.getButton1Text());
        dto.setButton1LinkType(inferLinkType(b.getButton1Url()));
        dto.setButton1Url(b.getButton1Url());
        dto.setButton2Text(b.getButton2Text());
        dto.setButton2LinkType(inferLinkType(b.getButton2Url()));
        dto.setButton2Url(b.getButton2Url());
        dto.setActive(b.isActive());
        return dto;
    }

    @Transactional
    public void create(BannerFormDto form) {
        StoredFile uploaded = uploadImage(form);
        String imageUrl = uploaded != null ? uploaded.publicUrl() : form.getImageUrl();
        String imageKey = uploaded != null ? uploaded.objectKey() : null;
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("배너 이미지를 등록해주세요.");
        }
        if (uploaded != null) {
            storageCleanup.deleteOnRollback(uploaded.objectKey());
        }
        Performance performance = resolvePerformance(form.getPerformanceId());
        int nextOrder = bannerRepository.findAllByOrderByDisplayOrderAscIdAsc().size();

        Banner banner = Banner.create(
                form.getTitle(), form.getDescription(), imageUrl, imageKey, performance,
                form.getLinkUrl(), form.getPeriodStart(), form.getPeriodEnd(), form.getBadgeText(),
                blankToDefault(form.getButton1Text(), "실시간 예매하기"), resolveButtonUrl(form.getButton1LinkType(), form.getButton1Url()),
                blankToDefault(form.getButton2Text(), "자세히 보기"), resolveButtonUrl(form.getButton2LinkType(), form.getButton2Url()),
                nextOrder, form.isActive()
        );
        bannerRepository.save(banner);
    }

    @Transactional
    public void update(Long id, BannerFormDto form) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너입니다."));

        StoredFile uploaded = uploadImage(form);
        String imageUrl = banner.getImageUrl();
        String imageKey = banner.getImageKey();
        if (uploaded != null) {
            storageCleanup.deleteOnRollback(uploaded.objectKey());
            storageCleanup.deleteAfterCommit(banner.getImageKey());
            imageUrl = uploaded.publicUrl();
            imageKey = uploaded.objectKey();
        }
        Performance performance = resolvePerformance(form.getPerformanceId());

        banner.update(
                form.getTitle(), form.getDescription(), imageUrl, imageKey, performance,
                form.getLinkUrl(), form.getPeriodStart(), form.getPeriodEnd(), form.getBadgeText(),
                blankToDefault(form.getButton1Text(), "실시간 예매하기"), resolveButtonUrl(form.getButton1LinkType(), form.getButton1Url()),
                blankToDefault(form.getButton2Text(), "자세히 보기"), resolveButtonUrl(form.getButton2LinkType(), form.getButton2Url()),
                form.isActive()
        );
    }

    @Transactional
    public void delete(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너입니다."));
        bannerRepository.delete(banner);
        storageCleanup.deleteAfterCommit(banner.getImageKey());
        reorderSequentially();
    }

    @Transactional
    public void toggleActive(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너입니다."));
        banner.toggleActive();
    }

    @Transactional
    public void moveUp(Long id) {
        swapWithNeighbor(id, -1);
    }

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

    /** 버튼 링크 드롭다운 선택값을 실제 URL로 변환합니다. AUTO는 null로 저장해 홈 화면에서 연결 공연 기준 자동 계산되게 합니다. */
    private String resolveButtonUrl(String linkType, String customUrl) {
        if (linkType == null || linkType.isBlank() || "AUTO".equals(linkType)) {
            return null;
        }
        if ("CUSTOM".equals(linkType)) {
            return (customUrl == null || customUrl.isBlank()) ? null : customUrl;
        }
        return PREDEFINED_LINKS.get(linkType);
    }

    /** 저장된 button URL을 보고 수정 폼의 드롭다운 선택값을 역으로 추정합니다. */
    private String inferLinkType(String url) {
        if (url == null || url.isBlank()) {
            return "AUTO";
        }
        return PREDEFINED_LINKS.entrySet().stream()
                .filter(e -> e.getValue().equals(url))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("CUSTOM");
    }

    private StoredFile uploadImage(BannerFormDto form) {
        if (form.getBannerFile() == null || form.getBannerFile().isEmpty()) {
            return null;
        }
        return storageService.storeImage(form.getBannerFile(), storageProperties.getS3().getBannerPrefix());
    }
}
