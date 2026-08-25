package kr.co.stageon.admin.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 어노테이션이 붙은 메서드가 "정상적으로 성공 완료"되면
 * AdminActivityLogAspect가 admin_activity_logs 테이블에 자동으로 기록합니다.
 * (예외 발생 시에는 기록하지 않음 - @AfterReturning 기반)
 *
 * targetId / description은 SpEL로 작성합니다.
 * - 메서드 파라미터 이름을 #이름 으로 참조 가능 (예: #id, #dto)
 * - 메서드 리턴값은 #result 로 참조 가능 (예: create()가 반환한 신규 PK)
 *
 * 사용 예)
 * @AdminLoggable(actionType = "UPDATE", targetEntity = "PERFORMANCE",
 *                targetId = "#id", description = "'공연 수정: ' + #dto.title")
 * public void update(Long id, PerformanceFormDto dto, boolean draft) { ... }
 *
 * @AdminLoggable(actionType = "CREATE", targetEntity = "PERFORMANCE",
 *                targetId = "#result", description = "'공연 등록: ' + #dto.title")
 * public Long create(PerformanceFormDto dto, boolean draft) { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdminLoggable {

    /** CREATE / UPDATE / DELETE / CANCEL 등 */
    String actionType();

    /** PERFORMANCE / MEMBER / RESERVATION / REFUND 등 (대상 도메인) */
    String targetEntity();

    /** 대상 PK를 가리키는 SpEL. 비워두면 target_id는 null로 기록됨 */
    String targetId() default "";

    /** 로그 설명을 만드는 SpEL. 비워두면 description은 null로 기록됨 */
    String description() default "";
}