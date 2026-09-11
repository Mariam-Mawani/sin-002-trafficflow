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

}
