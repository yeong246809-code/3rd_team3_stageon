package kr.co.stageon.admin.repository;

import kr.co.stageon.admin.domain.AdminActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminActivityLogRepository
        extends JpaRepository<AdminActivityLog, Long>, JpaSpecificationExecutor<AdminActivityLog> {
}