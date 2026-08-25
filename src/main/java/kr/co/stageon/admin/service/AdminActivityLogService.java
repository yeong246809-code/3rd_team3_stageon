package kr.co.stageon.admin.service;

import jakarta.persistence.criteria.Predicate;
import kr.co.stageon.admin.domain.AdminActivityLog;
import kr.co.stageon.admin.dto.AdminActivityLogRowDto;
import kr.co.stageon.admin.dto.AdminActivityLogSearchDto;
import kr.co.stageon.admin.repository.AdminActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 관리자 계정이 1개뿐인 프로젝트 특성상 관리자 이메일 검색 조건은 제거함(2026-08-25). */
@Service
@RequiredArgsConstructor
public class AdminActivityLogService {

    private final AdminActivityLogRepository adminActivityLogRepository;

    @Transactional(readOnly = true)
    public Page<AdminActivityLogRowDto> search(AdminActivityLogSearchDto searchDto, int page, int size) {
        Specification<AdminActivityLog> spec = buildSpecification(searchDto);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return adminActivityLogRepository.findAll(spec, pageable)
                .map(AdminActivityLogRowDto::from);
    }

    private Specification<AdminActivityLog> buildSpecification(AdminActivityLogSearchDto s) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (s.actionType() != null && !s.actionType().isBlank()) {
                predicates.add(cb.equal(root.get("actionType"), s.actionType()));
            }
            if (s.targetEntity() != null && !s.targetEntity().isBlank()) {
                predicates.add(cb.equal(root.get("targetEntity"), s.targetEntity()));
            }
            if (s.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), s.dateFrom().atStartOfDay()));
            }
            if (s.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), s.dateTo().atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}