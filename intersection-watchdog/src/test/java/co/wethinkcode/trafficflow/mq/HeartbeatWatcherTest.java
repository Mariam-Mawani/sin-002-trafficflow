package co.wethinkcode.trafficflow.mq;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class HeartbeatWatcherTest {

    private static final Duration THRESHOLD = Duration.ofSeconds(16);

    @Test
    void notStaleWhenLastHeartbeatIsWithinTheThreshold() {
        Instant last = Instant.now().minusSeconds(5);
        assertFalse(HeartbeatWatcher.isStale(last, Instant.now(), THRESHOLD));
    }



}
