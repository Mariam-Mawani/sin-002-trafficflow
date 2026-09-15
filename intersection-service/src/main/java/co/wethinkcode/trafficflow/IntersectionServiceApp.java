package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

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

        System.out.println("intersection-service ready with " + byId.size() + " intersections across "
                + districts.size() + " districts.");
    }

    /**
     * Fetches the cleaned intersection list from ingestion-service, retrying a few
     * times before giving up so a slightly-out-of-order manual startup doesn't crash
     * this service immediately.
     */
    private static List<IntersectionRecord> fetchIntersectionsFromIngestionService() throws InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(INGESTION_SERVICE_URL)).GET().
                timeout(Duration.ofSeconds(5)).build();

        Exception lastError = null;
        for (int attempt =1; attempt <= MAX_STARTUP_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IOException("ingestion-service returned HTTP " + response.statusCode());
                }
                CollectionType listType = JSON.getTypeFactory()
                        .constructCollectionType(List.class, IntersectionRecord.class);
                return JSON.readValue(response.body(), listType);
            } catch (IOException error) {
                lastError = error;
                System.out.println("Attempt " + attempt + "/" + MAX_STARTUP_ATTEMPTS
                        + " to reach ingestion-service failed: " + error.getMessage());

                if (attempt < MAX_STARTUP_ATTEMPTS) {
                    Thread.sleep(RETRY_DELAY_MS);
                }

            }
        }
        throw new IllegalStateException(
                "Could not load intersections from ingestion-service (" + INGESTION_SERVICE_URL + ") after "
                        + MAX_STARTUP_ATTEMPTS + " attempts. Make sure ingestion-service"
                        + " is running on port 7020 first.",
                lastError);
    }

    /**
     * Builds the id -> record lookup used by /intersections/{id}. Pulled out as its
     * own method (rather than inlined in main) so it can be unit-tested without a
     * live ingestion-service.
     */
    static Map<String, IntersectionRecord> indexById(List<IntersectionRecord> records) {
        Map<String, IntersectionRecord> byId = new LinkedHashMap<>();
        for (IntersectionRecord record : records) {
            if (record.id() != null) {
                byId.put(record.id().toUpperCase(Locale.ROOT), record);
            }
        }
        return byId;
    }

}

// MQ TODO: publishes a periodic heartbeat to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at
// MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig), consumed by intersection-watchdog.
