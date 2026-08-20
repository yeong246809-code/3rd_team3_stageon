package kr.co.stageon.common.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageTransactionCleanup {
    private final ObjectStorageService storageService;

    /** DB 저장이 롤백되면 먼저 업로드된 새 객체를 제거합니다. */
    public void deleteOnRollback(String objectKey) {
        if (isBlank(objectKey)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    safelyDelete(objectKey);
                }
            }
        });
    }

    /** DB 변경이 확정된 뒤 교체 전 객체를 제거합니다. */
    public void deleteAfterCommit(String objectKey) {
        if (isBlank(objectKey)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safelyDelete(objectKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safelyDelete(objectKey);
            }
        });
    }

    private void safelyDelete(String objectKey) {
        try {
            storageService.delete(objectKey);
        } catch (RuntimeException e) {
            log.warn("저장소 객체 정리에 실패했습니다. objectKey={}", objectKey, e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
