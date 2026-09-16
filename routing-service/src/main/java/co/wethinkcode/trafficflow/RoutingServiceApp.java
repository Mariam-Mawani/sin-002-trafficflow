package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.http.HttpClient;

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
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
