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

    @Transactional
    public void delete(Long id) {
        if (!versionRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 버전 이력입니다.");
        }
        versionRepository.deleteById(id);
    }
}