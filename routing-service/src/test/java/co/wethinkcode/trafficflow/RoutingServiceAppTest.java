package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoutingServiceAppTest {

    @Test
    void noCongestionGivesJustTheBaseTime() {
        assertEquals(5.0, estimateMinutes(0));
    }

}
