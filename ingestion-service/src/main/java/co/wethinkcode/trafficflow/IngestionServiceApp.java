package co.wethinkcode.trafficflow;

import com.opencsv.exceptions.CsvValidationException;
import io.javalin.Javalin;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class IngestionServiceApp {

    private static final String CSV_RESOURCE = "/intersections-legacy.csv";
    /** Values that mean "no real value was supplied", regardless of case. */
    private static final Set<String> PLACEHOLDER_VALUES = Set.of("", "n/a", "na", "tbd", "unknown", "-", "nan");
    private static final Set<String> TRUE_VALUES = Set.of("y", "yes", "true", "1");
    private static final Set<String> FALSE_VALUES = Set.of("n", "no", "false", "0");


    public record IntersectionRecord(String id, String district, String signalType, Boolean active) {

        /**
         * Returns a copy of this record with any {@code null} fields filled
         * in from {@code other}, preferring this record's own values where
         * both are present. Used when merging duplicate rows for the same
         * real-world intersection.
         */
        IntersectionRecord mergeWith(IntersectionRecord other) {
            return new IntersectionRecord(
                    id,
                    district != null ? district : other.district,
                    signalType != null ? signalType : other.signalType,
                    active != null ? active : other.active
            );
        }

    }

    public static void main(String[] args) {
        List<IntersectionRecord> cleanedRecords = loadCleanedRecords();
        Javalin app = Javalin.create().start(7020);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/intersections-legacy.csv (intersections, districts, signal types data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        // Cleaned intersection records for downstream services (see integration
        // contract in the root README: ingestion-service -> intersection-service).
        app.get("/intersections", ctx -> ctx.json(cleanedRecords));
        System.out.println("Loaded " + cleanedRecords.size() + " cleaned intersection records.");
    }

    /**
     * Loads and cleans {@code intersections-legacy.csv} once at startup. If
     * the file can't be read, fails fast rather than serving an empty/partial
     * dataset silently.
     */
    static List<IntersectionRecord> loadCleanedRecords() {
        try (InputStream csvStream = IngestionServiceApp.class.getResourceAsStream(CSV_RESOURCE)) {
            if (csvStream == null) {
                throw new IllegalStateException("Could not find " + CSV_RESOURCE + " on the classpath");
            }
            return cleanCsv(csvStream);
        } catch (IOException | CsvValidationException e) {
            throw new IllegalStateException("Failed to load and clean " + CSV_RESOURCE, e);
        }
    }

}
