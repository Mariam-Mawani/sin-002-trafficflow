package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
