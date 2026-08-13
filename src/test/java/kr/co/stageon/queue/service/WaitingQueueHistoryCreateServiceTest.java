package kr.co.stageon.queue.service;

import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.queue.domain.WaitingQueueHistory;
import kr.co.stageon.queue.repository.WaitingQueueHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingQueueHistoryCreateServiceTest {

    @Mock WaitingQueueHistoryRepository historyRepository;
    @Mock PerformanceScheduleRepository scheduleRepository;
    @Mock MemberRepository memberRepository;
    @Mock RedisWaitingQueueService redisQueueService;

    private final QueueTokenService tokenService = new QueueTokenService();
    private WaitingQueueHistoryCreateService service;
    private PerformanceSchedule schedule;
    private Member member;

    @BeforeEach
    void setUp() {
        service = new WaitingQueueHistoryCreateService(
                historyRepository,
                scheduleRepository,
                memberRepository,
                redisQueueService,
                tokenService
        );

        schedule = mock(PerformanceSchedule.class);
        member = mock(Member.class);
    }

    @Test
    void createsHashedHistoryAndRegistersRedisQueue() {
        Performance performance = mock(Performance.class);
        when(performance.getId()).thenReturn(122L);
        when(schedule.getId()).thenReturn(10L);
        when(schedule.getPerformance()).thenReturn(performance);
        when(member.getId()).thenReturn(7L);
        stubOpenSalesWindow();
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(historyRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisQueueService.register(any(), any(), any())).thenReturn(true);

        WaitingQueueHistoryCreateService.QueueEntryResult result =
                service.create(10L, " USER@example.com ", null);

        ArgumentCaptor<WaitingQueueHistory> captor = ArgumentCaptor.forClass(WaitingQueueHistory.class);
        verify(historyRepository).saveAndFlush(captor.capture());
        WaitingQueueHistory saved = captor.getValue();

        assertThat(saved.getQueueTokenHash()).hasSize(64).doesNotContain(result.queueToken());
        assertThat(saved.getStatus()).isEqualTo(WaitingQueueHistory.Status.WAITING);
        assertThat(result.performanceId()).isEqualTo(122L);
        verify(redisQueueService).register(10L, 7L, result.queueToken());
    }

    @Test
    void rejectsScheduleBeforeSalesOpen() {
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(schedule.getStatus()).thenReturn(PerformanceSchedule.Status.SCHEDULED);
        when(schedule.getSalesOpenAt()).thenReturn(LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> service.create(10L, "user@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아직 예매가 시작되지 않은");
    }

    private void stubOpenSalesWindow() {
        when(schedule.getStatus()).thenReturn(PerformanceSchedule.Status.OPEN);
        when(schedule.getSalesOpenAt()).thenReturn(LocalDateTime.now().minusHours(1));
        when(schedule.getSalesCloseAt()).thenReturn(LocalDateTime.now().plusHours(1));
    }
}
