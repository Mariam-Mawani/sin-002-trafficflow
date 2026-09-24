package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.HeartbeatWatcher;
import io.javalin.Javalin;

public class IntersectionWatchdogApp {

    public static void main(String[] args) {
        HeartbeatWatcher heartbeatWatcher = new HeartbeatWatcher();
        heartbeatWatcher.start();
        Runtime.getRuntime().addShutdownHook(new Thread(heartbeatWatcher::stop));
        Javalin app = Javalin.create().start(7024);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Cries for help if the Intersection Service crashes, since routes can no longer be validated.)
        // Mechanism: ActiveMQ Queue heartbeat/dead-letter

        System.out.println("intersection-watchdog ready — watching for missed heartbeats"
                + " from intersection-service and dead-lettered messages.");

    }
}

// MQ TODO: subscribes to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at MqConfig.BROKER_URL
// (see co.wethinkcode.trafficflow.mq.MqConfig) and alerts if a heartbeat from
// intersection-service is missed or a message lands in the dead-letter queue.
