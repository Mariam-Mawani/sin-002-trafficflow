package co.wethinkcode.trafficflow.mq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CongestionTopicSubscriberTest {

    @Test
    void parsesTheLevelOutOfTheMessagePayload() {
        assertEquals(0, CongestionTopicSubscriber.parseLevel("{\"level\":0}"));
        assertEquals(8, CongestionTopicSubscriber.parseLevel("{\"level\":8}"));
        assertEquals(3, CongestionTopicSubscriber.parseLevel("{\"level\": 3}"));
    }
}
