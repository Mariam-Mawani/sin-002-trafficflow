package co.wethinkcode.trafficflow.mq;

import javax.jms.Connection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Subscribes to {@link MqConfig#TOPIC} and keeps the most recently seen
 * congestion level in memory, replacing the direct {@code GET /congestion}
 * REST call to congestion-service (stage 2) with an async topic subscription
 * (stage 3) — routing-service reads {@link #getCurrentLevel()} instead of
 * making a network call per request.
 */
public class CongestionTopicSubscriber {

    private static final int DEFAULT_LEVEL = 0;
    private final AtomicInteger currentLevel = new AtomicInteger(DEFAULT_LEVEL);
    private Connection connection;

}
