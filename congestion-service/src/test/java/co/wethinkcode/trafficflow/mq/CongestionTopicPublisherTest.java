package co.wethinkcode.trafficflow.mq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CongestionTopicPublisherTest {

    @Test
    void payloadContainsTheLevel() {
        assertEquals("{\"level\":0}", CongestionTopicPublisher.buildPayload(0));
        assertEquals("{\"level\":8}", CongestionTopicPublisher.buildPayload(8));
    }
}
