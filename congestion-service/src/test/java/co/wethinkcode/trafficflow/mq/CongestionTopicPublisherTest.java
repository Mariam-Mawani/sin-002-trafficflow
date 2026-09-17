package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.CongestionTopicPublisher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CongestionTopicPublisherTest {

    @Test
    void payloadContainsTheLevel() {
        assertEquals("{\"level\":0}", CongestionTopicPublisher.buildPayload(0));
        assertEquals("{\"level\":8}", CongestionTopicPublisher.buildPayload(8));
    }
}
