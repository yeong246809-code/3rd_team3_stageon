package kr.co.stageon.queue.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class WaitingQueuePropertiesTest {

    @Test
    void productionDefaultAdmissionTtlIsTenMinutes() {
        WaitingQueueProperties properties = new WaitingQueueProperties();

        assertThat(properties.getAdmissionTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.getWaitingTtl()).isEqualTo(Duration.ofHours(2));
    }
}
