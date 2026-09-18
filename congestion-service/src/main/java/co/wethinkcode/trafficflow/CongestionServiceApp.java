package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.CongestionTopicPublisher;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.Map;
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
        CongestionTopicPublisher topicPublisher = new CongestionTopicPublisher();
        // Best-effort announcement of the starting level - only useful to a consumer
        // that happens to already be connected and listening, since this is a plain
        // pub/sub topic with no message history for late subscribers.
        topicPublisher.publish(congestionLevel.get());
        Runtime.getRuntime().addShutdownHook(new Thread(topicPublisher::stop));
        Javalin app = Javalin.create().start(7022);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Tracks the city-wide Congestion Level (0-8).)
        // Add domain endpoints for congestion-service here.

        // routing-service calls this per-request in stage 2 to factor the current
        // congestion level into its travel-time estimate.
        app.get("/congestion", ctx -> ctx.json(Map.of("level", congestionLevel.get())));


        // Not part of the integration contract, but something has to be able to change
        // the level for there to be anything interesting to poll — this stands in for
        // whatever real sensor feed or traffic model would normally drive it.
        app.post("/congestion", ctx -> {
            CongestionUpdate update = ctx.bodyAsClass(CongestionUpdate.class);
            if (!isValidLevel(update.level())) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "level must be a whole number between "
                        + MIN_LEVEL + " and " + MAX_LEVEL));
                return;
            }
            congestionLevel.set(update.level());
            topicPublisher.publish(update.level);   // tell routing-service (and anyone else listening)
            ctx.json(Map.of("level", congestionLevel.get()));
        });
        System.out.println("congestion-service ready, starting congestion level = " + STARTING_LEVEL);
    }

    /**
     * Pulled out as its own method (rather than inlined in the POST handler) so the
     * validation rule can be unit-tested without spinning up the whole app.
     */
    static boolean isValidLevel(Integer level) {
        return level != null && level >= MIN_LEVEL && level <= MAX_LEVEL;
    }

    /** Request body shape for POST /congestion. */
    public record CongestionUpdate(Integer level) {

    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
