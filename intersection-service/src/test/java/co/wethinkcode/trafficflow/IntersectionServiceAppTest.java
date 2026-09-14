package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests exercise the pure indexing logic directly, without starting a real
 * server or calling a live ingestion-service — that's covered by manual curl
 * verification per the README, since spinning up multiple interdependent services
 * inside a unit test is out of scope for this stage.
 */

public class IntersectionServiceAppTest {

    @Test
    void indexesRecordsByUppercaseId() {
        List<IntersectionRecord> records = List.of(
                new IntersectionRecord("int-1001", "Downtown", "4-way", true),
                new IntersectionRecord("INT-1002", "Midtown", "pedestrian", false)
        );

        Map<String, IntersectionRecord> byId = indexById(records);

        assertEquals(2, byId.size());
        assertEquals("Downtown", byId.get("INT-1001").district());
        assertEquals("Midtown", byId.get("INT-1002").district());
    }

    @Test
    void skipsRecordsWithNoId() {
        List<IntersectionRecord> records = List.of(
                new IntersectionRecord(null, "Downtown", "4-way", true),
                new IntersectionRecord("INT-1002", "Midtown", "pedestrian", false)
        );

        Map<String, IntersectionRecord> byId = indexById(records);

        assertEquals(1, byId.size());
        assertTrue(byId.containsKey("INT-1002"));
    }



}
