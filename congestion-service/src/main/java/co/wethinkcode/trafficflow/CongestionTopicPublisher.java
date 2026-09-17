package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;

import javax.jms.*;

/*
 * Publishes the current congestion level to {@link MqConfig#TOPIC} whenever it
 * changes, so routing-service (and anyone else listening) finds out without
 * having to poll {@code GET /congestion} on this service directly.
 */
public class CongestionTopicPublisher {

    private Connection connection;
    private Session session;
    private MessageProducer producer;


    /** Publishes {@code level} to the topic. Safe to call from multiple threads. */
    public synchronized void publish(int level) {
        try {
            ensureConnected();
            TextMessage message = session.createTextMessage(buildPayload(level));
            producer.send(message);
        } catch (JMSException e) {
            System.out.println("Could not publish congestion level " + level + " to " + MqConfig.TOPIC
                    + ": " + e.getMessage() + ". Is the broker up at " + MqConfig.BROKER_URL
                    + "? (see common/README.md: docker compose up -d)");
            // Drop anything half-open so the next publish reconnects cleanly rather than
            // reusing a connection/session that may itself be the reason this failed.
            closeConnection();
        }
    }

}
