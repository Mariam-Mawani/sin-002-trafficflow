package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

/**
 * Validates intersection/district names — the "source of truth" for the rest
 * of TrafficFlow. On startup it pulls the already-cleaned list from
 * ingestion-service (port 7020) and holds it in memory; every other service
 * asks this one whether an id or district is real.
 */

public class IntersectionServiceApp {

    private static final String INGESTION_SERVICE_URL = "http://localhost:7020/intersections";
    // Services in this exercise are started manually, one terminal at a time, so
    // ingestion-service might not be up yet the instant this one starts. A few retries
    // with a short pause is enough to ride that out without adding real orchestration.
    private static final int MAX_STARTUP_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 2000;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Mirrors the shape ingestion-service publishes at GET /intersections. */
    public record IntersectionRecord(String id, String district, String signalType, Boolean active) {
    }


    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7021);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Validates intersection/district names (source of truth).)
        // Add domain endpoints for intersection-service here.
    }
}

// MQ TODO: publishes a periodic heartbeat to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at
// MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig), consumed by intersection-watchdog.
