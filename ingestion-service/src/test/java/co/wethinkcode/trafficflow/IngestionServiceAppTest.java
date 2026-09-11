package co.wethinkcode.trafficflow;

import com.opencsv.exceptions.CsvValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class IngestionServiceAppTest {

    private List<IntersectionRecord> clean(String csv) throws IOException, CsvValidationException {

        return cleanCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private IntersectionRecord findById(List<IntersectionRecord> records, String id) {
        return records.stream()
                .filter(r -> r.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No record with id " + id));
    }

}
