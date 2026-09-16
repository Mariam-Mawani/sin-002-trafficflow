package co.wethinkcode.trafficflow;

public class CongestionServiceAppTest {

    @Test
    void acceptsLevelsWithinRange() {
        assertTrue(isValidLevel(0));
        assertTrue(isValidLevel(4));
        assertTrue(isValidLevel(8));
    }

    @Test
    void rejectsLevelsOutsideRange() {
        assertFalse(isValidLevel(-1));
        assertFalse(isValidLevel(9));
    }

    @Test
    void rejectsMissingLevel() {
        assertFalse(isValidLevel(null));
    }


}
