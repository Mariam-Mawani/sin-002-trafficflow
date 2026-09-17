package co.wethinkcode.trafficflow;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;

/*
 * Publishes the current congestion level to {@link MqConfig#TOPIC} whenever it
 * changes, so routing-service (and anyone else listening) finds out without
 * having to poll {@code GET /congestion} on this service directly.
 */
public class CongestionTopicPublisher {

    private Connection connection;
    private Session session;
    private MessageProducer producer;


}
