package co.wethinkcode.trafficflow.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

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

    private void ensureConnected() throws JMSException {
        if (connection != null) {
            return; // already connected from a previous publish
        }
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        connection = factory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(MqConfig.TOPIC);
        producer = session.createProducer(topic);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT); // only the latest level matters
        System.out.println("Connected to broker at " + MqConfig.BROKER_URL
                + " — publishing to " + MqConfig.TOPIC);
    }

    private void closeConnection() {
        try {
            if (connection != null) {
                connection.close(); // closing the connection also closes its session/producer
            }
        } catch (JMSException ignored) {
            // best-effort cleanup - nothing useful to do if even closing fails
        } finally {
            connection = null;
            session = null;
            producer = null;
        }
    }

    /** Releases the broker connection, if one is open. */
    public void stop() {
        closeConnection();
    }


}
