package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Provides estimated travel times based on congestion and intersection.
 * Talks to intersection-service and congestion-service directly over REST
 * (stage 2) — no queueing/decoupling yet, that's stage 3.
 */
public class RoutingServiceApp {

    private static final String INTERSECTION_SERVICE_BASE_URL = "http://localhost:7021";
    private static final String CONGESTION_SERVICE_URL = "http://localhost:7022/congestion";
    // Simplified travel-time model: a fixed base time for any direct hop between two
    // intersections, stretched out by however congested the city currently is. There's
    // no real road-distance data in this exercise, so this is illustrative rather than
    // realistic — the point is wiring the services together, not the physics.
    static final double BASE_MINUTES = 5.0;
    static final double MINUTES_PER_CONGESTION_LEVEL = 1.5;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();


    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7023);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Provides estimated travel times based on congestion and intersection.)
        // Add domain endpoints for routing-service here.

        app.get("/routes/estimate", ctx -> {
            String from = ctx.queryParam("from");
            String to = ctx.queryParam("to");

            if (from == null || from.isBlank() || to == null || to.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(Map.of("error", "both 'from' and 'to' query params are required"));
                return;
            }

            // Validate both endpoints against intersection-service before doing any
            // "work" — no point estimating a travel time for a route that doesn't exist.
            if (!intersectionExists(from)) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Unknown 'from' intersection: " + from));
                return;
            }
            if (!intersectionExists(to)) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Unknown 'to' intersection: " + to));
                return;
            }

            int congestionLevel = fetchCongestionLevel();
            double estimatedMinutes = estimateMinutes(congestionLevel);

            ctx.json(Map.of(
                    "from", from,
                    "to", to,
                    "congestionLevel", congestionLevel,
                    "estimatedMinutes", estimatedMinutes
            ));
        });

        System.out.println("routing-service ready.");
    }

    /**
     * The actual travel-time formula, pulled out as its own method so it can be
     * unit-tested without any HTTP calls at all.
     */
    static double estimateMinutes(int congestionLevel) {
        return BASE_MINUTES + (congestionLevel * MINUTES_PER_CONGESTION_LEVEL);
    }

    /**
     * Asks intersection-service whether an id is real. We only care about the status
     * code here (200 = known, 404 = unknown), so the response body is discarded.
     */
    private static boolean intersectionExists(String id) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(INTERSECTION_SERVICE_BASE_URL + "/intersections/" + id))
                .GET().timeout(Duration.ofSeconds(5)).build();
        try {
            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException error) {
            throw new IllegalStateException("Could not reach intersection-service at "
                    + INTERSECTION_SERVICE_BASE_URL, error);
        }
    }

}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
