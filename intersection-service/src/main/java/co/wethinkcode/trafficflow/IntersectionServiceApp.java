package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

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

        List<IntersectionRecord> records = fetchIntersectionsFromIngestionService();
        Map<String, IntersectionRecord> byId = indexById(records);
        TreeSet<String> districts = collectDistricts(records);

        Javalin app = Javalin.create().start(7021);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Validates intersection/district names (source of truth).)
        // Add domain endpoints for intersection-service here.

        // Not in the integration contract, but a natural fit for a "source of truth"
        // service — handy for debugging and for any client that wants everything at once.
        app.get("/intersections", ctx -> ctx.json(byId.values()));

        // routing-service calls this to validate a route's endpoints before estimating
        // travel time: 200 + the record if the id is known, 404 otherwise.
        app.get("/intersections/{id}", ctx -> {
            String id = ctx.pathParam("id").trim().toUpperCase(Locale.ROOT);
            IntersectionRecord record = byId.get(id);
            if (record == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Unknown intersection id: " + id));
                return;
            }
            ctx.json(record);
        });

        // The service's job description covers district names too, so it gets the same
        // kind of existence check, even though only /intersections/{id} is in the
        // integration contract right now.
        app.get("/districts/{name}", ctx -> {
            String name = ctx.pathParam("name").trim();
            if (!districts.contains(name)) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Unknown district: " + name));
                return;
            }
            ctx.json(Map.of("district", name, "valid", true));
        });

        System.out.println("intersection-service ready with " + byId.size()
                + " intersections across " + districts.size() + " districts.");
    }


}

// MQ TODO: publishes a periodic heartbeat to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at
// MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig), consumed by intersection-watchdog.
