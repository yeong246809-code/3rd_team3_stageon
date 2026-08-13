package kr.co.stageon.queue.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "stageon.queue")
public class WaitingQueueProperties {

    public static final String COOKIE_NAME = "STAGEON_QUEUE_TOKEN";

    private Duration waitingTtl = Duration.ofHours(2);
    private Duration admissionTtl = Duration.ofMinutes(10);
    private int admitBatchSize = 10;
    private boolean cookieSecure = false;
}
