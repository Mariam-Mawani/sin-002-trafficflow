package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the city-wide congestion level, a single whole number from 0 (clear)
 * to 8 (gridlock). routing-service polls GET /congestion per request in this
 * stage; once stage 3 is in place this would publish to the congestion-topic
 * instead.
 */
public class CongestionServiceApp {

    static final int MIN_LEVEL = 0;
    static final int MAX_LEVEL = 8;
    private static final int STARTING_LEVEL = 3;    // arbitrary "moderate" starting point


    public static void main(String[] args) {

        AtomicInteger congestionLevel = new AtomicInteger(STARTING_LEVEL);
        Javalin app = Javalin.create().start(7022);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Tracks the city-wide Congestion Level (0-8).)
        // Add domain endpoints for congestion-service here.

        // routing-service calls this per-request in stage 2 to factor the current
        // congestion level into its travel-time estimate.
        app.get("/congestion", ctx -> ctx.json(Map.of("level", congestionLevel.get())));

    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
