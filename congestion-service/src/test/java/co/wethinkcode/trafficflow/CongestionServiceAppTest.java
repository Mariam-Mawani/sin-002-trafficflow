package co.wethinkcode.trafficflow;

public class CongestionServiceAppTest {

    @Test
    void acceptsLevelsWithinRange() {
        assertTrue(isValidLevel(0));
        assertTrue(isValidLevel(4));
        assertTrue(isValidLevel(8));
    }


}
