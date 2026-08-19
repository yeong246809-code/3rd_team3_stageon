package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.VersionDetailDto;
import kr.co.stageon.admin.dto.VersionFormDto;
import kr.co.stageon.admin.dto.VersionListItemDto;
import kr.co.stageon.version.domain.Version;
import kr.co.stageon.version.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** "버전 관리(체인지로그)" 화면의 목록, 상세, 등록, 수정, 삭제를 담당합니다. */
@Service
@RequiredArgsConstructor
public class AdminVersionService {

    private final VersionRepository versionRepository;

    @Transactional(readOnly = true)
    public List<VersionListItemDto> getList() {
        return versionRepository.findAllByOrderByReleasedAtDescIdDesc().stream()
                .map(v -> new VersionListItemDto(
                        v.getId(), v.getVersion(), v.getCategory(), v.getTitle(), v.getAuthor(), v.getReleasedAt()))
                .toList();
    }

    /** 상세 모달용 JSON입니다. */
    @Transactional(readOnly = true)
    public VersionDetailDto getDetail(Long id) {
        Version v = versionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 버전 이력입니다."));
        return new VersionDetailDto(
                v.getId(), v.getVersion(), v.getCategory(), v.getTitle(), v.getDescription(), v.getAuthor(), v.getReleasedAt());
    }

    @Transactional(readOnly = true)
    public VersionFormDto getForm(Long id) {
        Version v = versionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 버전 이력입니다."));
        VersionFormDto dto = new VersionFormDto();
        dto.setId(v.getId());
        dto.setVersion(v.getVersion());
        dto.setCategory(v.getCategory().name());
        dto.setTitle(v.getTitle());
        dto.setDescription(v.getDescription());
        dto.setAuthor(v.getAuthor());
        dto.setReleasedAt(v.getReleasedAt());
        return dto;
    }

    @Transactional
    public void create(VersionFormDto form) {
        Version version = Version.create(
                form.getVersion(),
                Version.Category.valueOf(form.getCategory()),
                form.getTitle(),
                form.getDescription(),
                form.getAuthor(),
                form.getReleasedAt()
        );
        versionRepository.save(version);
    }

    @Transactional
    public void update(Long id, VersionFormDto form) {
        Version v = versionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 버전 이력입니다."));
        v.update(
                form.getVersion(),
                Version.Category.valueOf(form.getCategory()),
                form.getTitle(),
                form.getDescription(),
                form.getAuthor(),
                form.getReleasedAt()
        );
    }

    /**
     * 배포 웹훅(VersionWebhookController)에서 호출됩니다. 버전명은 "build-N" 형식으로 자동 채번하며,
     * category는 커밋 메시지 키워드로 추정합니다. 자동 생성이라 부정확할 수 있어 관리자 화면에서 수정 가능합니다.
     */
    @Transactional
    public Long registerFromDeploy(String title, String author, String commitMessage) {
        long nextSeq = versionRepository.count() + 1;
        String version = "build-" + nextSeq;
        String resolvedTitle = (title == null || title.isBlank())
                ? (commitMessage == null || commitMessage.isBlank() ? "배포 완료" : commitMessage)
                : title;
        String resolvedAuthor = (author == null || author.isBlank()) ? "system" : author;

        Version v = Version.create(
                version,
                inferCategory(commitMessage),
                resolvedTitle,
                commitMessage,
                resolvedAuthor,
                java.time.LocalDate.now()
        );
        versionRepository.save(v);
        return v.getId();
    }

    private Version.Category inferCategory(String text) {
        if (text == null) return Version.Category.FEATURE;
        String t = text.toLowerCase();
        if (t.contains("fix") || t.contains("수정") || t.contains("버그") || t.contains("오류")) {
            return Version.Category.BUGFIX;
        }
        if (t.contains("개선") || t.contains("리팩터") || t.contains("리팩토링") || t.contains("변경")) {
            return Version.Category.IMPROVEMENT;
        }
        return Version.Category.FEATURE;
    }

    @Transactional
    public void delete(Long id) {
        if (!versionRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 버전 이력입니다.");
        }
        versionRepository.deleteById(id);
    }
}