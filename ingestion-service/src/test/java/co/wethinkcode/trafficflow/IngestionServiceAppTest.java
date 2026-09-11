package co.wethinkcode.trafficflow;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IngestionServiceAppTest {

    private List<IntersectionRecord> clean(String csv) throws IOException, CsvValidationException {

        return cleanCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private IntersectionRecord findById(List<IntersectionRecord> records, String id) {
        return records.stream()
                .filter(r -> r.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("No record with id " + id));
    }

    @Test
    void trimsPaddingAndCollapsesDoubleSpacesInFields() throws Exception {
        String csv = "intersection_id,District ,signal_type,active_flag\n"
                + "INT-1001, Downtown ,4-way,Y\n";

        List<IntersectionRecord> records = clean(csv);

        assertEquals(1, records.size());
        assertEquals("Downtown", records.get(0).district());
    }

    @Test
    void normalizesCasingOfIdDistrictAndSignalType() throws Exception {
        String csv = "intersection_id,district,signal_type,active_flag\n"
                + "int-1002,MIDTOWN,PEDESTRIAN,yes\n";

        IntersectionRecord record = clean(csv).get(0);

        assertEquals("INT-1002", record.id());
        assertEquals("Midtown", record.district());
        assertEquals("pedestrian", record.signalType());
    }

    @Test
    void collapsesDuplicateRecordsForTheSameIntersectionUnderDifferentIdCasing() throws Exception {
        String csv = "intersection_id,district,signal_type,active_flag\n"
                + "INT-1005,Downtown,Roundabout,true\n"
                + "int-1005,downtown ,ROUNDABOUT,TRUE\n";

        List<IntersectionRecord> records = clean(csv);

        assertEquals(1, records.size());
        assertEquals("INT-1005", records.get(0).id());
    }

    @Test
    void mergesFieldsFromDuplicatesInsteadOfOverwritingWithBlanks() throws Exception {
        // Second row is missing the district; the first row's value should win rather
        // than being lost.
        String csv = "intersection_id,district,signal_type,active_flag\n"
                + "INT-1020,Eastside,4-way,Y\n"
                + "int-1020,,4-way,Y\n";

        List<IntersectionRecord> records = clean(csv);

        assertEquals(1, records.size());
        assertEquals("Eastside", records.get(0).district());
    }

    @Test
    void treatsMissingAndPlaceholderValuesAsExplicitNullNotDropped() throws Exception {
        String csv = "intersection_id,district,signal_type,active_flag\n"
                + "INT-1007,Eastside,,1\n"
                + "INT-1013,Westside,unknown,N/A\n"
                + "INT-1015,,4-way,Y\n";

        List<IntersectionRecord> records = clean(csv);

        assertEquals(3, records.size(), "placeholder/missing values must not cause rows to be dropped");

        IntersectionRecord int1007 = findById(records, "INT-1007");
        assertNull(int1007.signalType());

        IntersectionRecord int1013 = findById(records, "INT-1013");
        assertNull(int1013.signalType());
        assertNull(int1013.active());

        IntersectionRecord int1015 = findById(records, "INT-1015");
        assertNull(int1015.district());
    }

    @Test
    void normalizesBooleanFlagRepresentations() throws Exception {
        String csv = "intersection_id,district,signal_type,active_flag\n"
                + "INT-1,Downtown,4-way,Y\n"
                + "INT-2,Downtown,4-way,yes\n"
                + "INT-3,Downtown,4-way,true\n"
                + "INT-4,Downtown,4-way,1\n"
                + "INT-5,Downtown,4-way,N\n"
                + "INT-6,Downtown,4-way,no\n"
                + "INT-7,Downtown,4-way,FALSE\n"
                + "INT-8,Downtown,4-way,0\n";

        List<IntersectionRecord> records = clean(csv);

        for (String truthyId : List.of("INT-1", "INT-2", "INT-3", "INT-4")) {
            assertTrue(findById(records, truthyId).active(), truthyId + " should be active");
        }
        for (String falsyId : List.of("INT-5", "INT-6", "INT-7", "INT-8")) {
            assertFalse(findById(records, falsyId).active(), falsyId + " should be inactive");
        }
    }

    @Test
    void skipsRowsWithNoUsableIdInsteadOfCorruptingTheMap() throws Exception {
        String csv = "intersection_id,district,signal_type,active_flag\n"
                + ",Downtown,4-way,Y\n"
                + "N/A,Midtown,4-way,Y\n"
                + "INT-1030,Downtown,4-way,Y\n";

        List<IntersectionRecord> records = clean(csv);

        assertEquals(1, records.size());
        assertEquals("INT-1030", records.get(0).id());
    }






}
