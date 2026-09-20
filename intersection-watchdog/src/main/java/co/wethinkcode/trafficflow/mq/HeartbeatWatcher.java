package co.wethinkcode.trafficflow.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/** Notices when intersection-service goes down */
public class HeartbeatWatcher {

    /** ActiveMQ's default dead-letter queue name for a broker with no custom policy. */
    private static final String DEAD_LETTER_QUEUE = "ActiveMQ.DLQ";
    // intersection-service's HeartbeatPublisher sends one every 5s. A single late
    // beat isn't worth alerting on (a slow GC pause, a moment of broker lag), so the
    // threshold is a few intervals rather than one, to only fire on a real outage.
    private static final Duration MISSED_HEARTBEAT_THRESHOLD = Duration.ofSeconds(16);
    private static final long CHECK_INTERVAL_SECONDS = 5;
    private static final long RECONNECT_INTERVAL_SECONDS = 10;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "intersection-watchdog");
        thread.setDaemon(true); // don't stop the JVM from exiting just for this
        return thread;
    });

    private final AtomicReference<Instant> lastHeartbeatAt = new AtomicReference<>(null);
    private volatile boolean alertActive = false; // avoid re-alerting on every single check tick
    private Connection connection;


    /** Connects (retrying if the broker isn't up yet) and starts watching. */
    public void start() {
        scheduler.scheduleAtFixedRate(this::ensureConnected, 0, RECONNECT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(
                this::checkForMissedHeartbeat, CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Stops watching and releases the broker connection, if one is open. */
    public void stop() {
        scheduler.shutdownNow();
        closeConnection();
    }

    private void ensureConnected() {
        if (connection != null) {
            return; // already connected from a previous attempt
        }
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue heartbeatQueue = session.createQueue(MqConfig.HEARTBEAT_QUEUE);
            MessageConsumer heartbeatConsumer = session.createConsumer(heartbeatQueue);
            heartbeatConsumer.setMessageListener(this::onHeartbeat);

            Queue deadLetterQueue = session.createQueue(DEAD_LETTER_QUEUE);
            MessageConsumer deadLetterConsumer = session.createConsumer(deadLetterQueue);
            deadLetterConsumer.setMessageListener(this::onDeadLetter);

            System.out.println("Connected to broker at " + MqConfig.BROKER_URL
                    + " — watching " + MqConfig.HEARTBEAT_QUEUE + " and " + DEAD_LETTER_QUEUE);
        } catch (JMSException error) {
            System.out.println("Could not connect to broker at " + MqConfig.BROKER_URL + ": " + error.getMessage()
                    + ". Will retry in " + RECONNECT_INTERVAL_SECONDS
                    + "s. (see common/README.md: docker compose up -d)");
            closeConnection();
        }
    }

    private void onHeartbeat(Message message) {
        Instant now = Instant.now();
        boolean wasDown = alertActive;
        lastHeartbeatAt.set(now);
        if (wasDown) {
            alertActive = false;
            System.out.println("[RECOVERED] Heartbeat received again from intersection-service at "
                    + now + " — it's back up.");
        }
    }

    private void onDeadLetter(Message message) {
        System.out.println("[ALERT] A message landed in the dead-letter queue (" + DEAD_LETTER_QUEUE
                + ") — a message from intersection-service may have failed delivery. Check the broker's"
                + " web console at http://localhost:8161 for details.");
    }

    private void checkForMissedHeartbeat() {
        Instant last = lastHeartbeatAt.get();
        if (isStale(last, Instant.now(), MISSED_HEARTBEAT_THRESHOLD) && !alertActive) {
            alertActive = true;
            System.out.println("[ALERT] No heartbeat from intersection-service in over "
                    + MISSED_HEARTBEAT_THRESHOLD.toSeconds() + "s (last seen " + last
                    + ") — it may be down. Routes can no longer be validated.");
        }
    }

    private void closeConnection() {
        try {
            if (connection != null) {
                connection.close(); // closing the connection also closes its session/consumers
            }
        } catch (JMSException ignored) {
            // best-effort cleanup - nothing useful to do if even closing fails
        } finally {
            connection = null;
        }
    }



}
