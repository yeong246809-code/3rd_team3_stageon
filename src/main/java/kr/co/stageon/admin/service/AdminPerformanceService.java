package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.PerformanceFormDto;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPerformanceService {

    private final PerformanceRepository performanceRepository;

    /** 예전 방식(전체 조회)이 필요한 곳이 있을 수 있어 남겨둡니다. */
    @Transactional(readOnly = true)
    public List<Performance> getList() {
        return performanceRepository.findAll();
    }

    /**
     * 관리자 공연 목록을 페이지 단위로 조회합니다.
     * @param page 0부터 시작하는 페이지 번호
     * @param size 한 페이지에 보여줄 개수
     */
    @Transactional(readOnly = true)
    public Page<Performance> getList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return performanceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public PerformanceFormDto getForm(Long id) {
        Performance p = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다. id=" + id));
        return PerformanceFormDto.from(p);
    }

    /** draft=true면 임시저장(저장 상태 유지, 목록에 "임시저장됨" 표시), false면 정식 저장입니다. */
    @Transactional
    public Long create(PerformanceFormDto dto, boolean draft) {
        Performance p = Performance.create(
                dto.getKopisId(), dto.getTitle(), dto.getGenre(), dto.getPosterUrl(),
                dto.getStartDate(), dto.getEndDate(), dto.getRuntimeMinutes(),
                dto.getAgeText(), dto.getStory(), toStatus(dto.getStatus()), draft
        );
        return performanceRepository.save(p).getId();
    }

    @Transactional
    public void update(Long id, PerformanceFormDto dto, boolean draft) {
        Performance p = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다. id=" + id));
        p.update(
                dto.getKopisId(), dto.getTitle(), dto.getGenre(), dto.getPosterUrl(),
                dto.getStartDate(), dto.getEndDate(), dto.getRuntimeMinutes(),
                dto.getAgeText(), dto.getStory(), toStatus(dto.getStatus()), draft
        );
    }

    private Performance.Status toStatus(String status) {
        return (status == null || status.isBlank())
                ? Performance.Status.UPCOMING
                : Performance.Status.valueOf(status);
    }
}