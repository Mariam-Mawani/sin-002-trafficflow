package co.wethinkcode.trafficflow.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.rmi.server.LogStream.parseLevel;

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


    /**
     * Connects and starts listening. Best-effort: if the broker isn't reachable
     * right now, this logs a warning and routing-service simply keeps using
     * {@link #DEFAULT_LEVEL} until it's restarted — there's no periodic retry
     * here, since (unlike a producer) a subscriber can't "try again on the next
     * publish" if it never connected in the first place.
     */
    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageConsumer consumer = session.createConsumer(topic);
            consumer.setMessageListener(this::onMessage);
            System.out.println("Connected to broker at " + MqConfig.BROKER_URL
                    + " — listening on " + MqConfig.TOPIC);
        } catch (JMSException error) {
            System.out.println("Could not subscribe to " + MqConfig.TOPIC + ": " + error.getMessage()
                    + ". Falling back to a default congestion level (" + DEFAULT_LEVEL
                    + ") until this service is restarted. Is the broker up at " + MqConfig.BROKER_URL
                    + "? (see common/README.md: docker compose up -d)");
        }
    }

    private void onMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                currentLevel.set(parseLevel(textMessage.getText()));
            }
        } catch (JMSException error) {
            System.out.println("Failed to read a congestion-topic message: " + error.getMessage());
        }
    }

    /** The most recently seen congestion level, or {@link #DEFAULT_LEVEL} if none has arrived yet. */
    public int getCurrentLevel() {
        return currentLevel.get();
    }

    /** Releases the broker connection, if one is open. */
    public void stop() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException ignored) {
            // best-effort cleanup - nothing useful to do if even closing fails
        }
    }


}
