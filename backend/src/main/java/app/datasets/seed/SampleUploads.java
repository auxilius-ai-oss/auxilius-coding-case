package app.datasets.seed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;
import org.springframework.util.FileSystemUtils;

/**
 * Writes the files the README's task uses: next quarter's extracts, and the corrected re-exports finance sends
 * during the close. Run with {@code ./gradlew generateSampleUploads}.
 */
public final class SampleUploads {

    private SampleUploads() {}

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args[0]);
        Quarter q = Seeder.NEXT;
        String folder = q.label().toLowerCase().replace(' ', '-').replaceAll("q(\\d)-(\\d+)", "$2-q$1");
        Path extracts = root.resolve(folder);
        Path corrections = root.resolve(folder + "-corrections");
        FileSystemUtils.deleteRecursively(extracts);
        FileSystemUtils.deleteRecursively(corrections);
        Files.createDirectories(extracts);
        Files.createDirectories(corrections);

        for (Extracts.Extract extract : Extracts.ALL) {
            Extracts.Extract exported = switch (extract.fileName()) {
                case "Customer_master.csv" -> extract.withHeader(3, "Country ");
                case "Travel_expense_claims.csv" -> extract.withHeader(5, "");
                default -> extract;
            };
            UnaryOperator<List<List<String>>> edit = extract.fileName().equals("AP_invoice_lines.csv")
                    ? SampleUploads::plantUnparseableAmounts
                    : UnaryOperator.identity();
            Files.write(extracts.resolve(extract.fileNameFor(q)), Extracts.csvFile(exported, q, extract.rows(), edit));
        }
        Files.write(extracts.resolve(Extracts.costCentreWorkbookName(q)), Extracts.costCentreWorkbook(q, 3));
        Extracts.Extract vendors = Extracts.byName("Vendor_master.csv");
        Files.write(extracts.resolve("vendor_master.xlsx"), Extracts.singleSheetWorkbook(vendors, q, vendors.rows()));
        Extracts.Extract lineItems = Extracts.byName("BSEG_journal_entry_line_items.csv");
        Files.write(extracts.resolve("GL_line_items_full_year_" + q.year() + ".csv"),
                Extracts.csvFile(lineItems, q, 26000, UnaryOperator.identity()));

        for (String name : List.of("Customer_master.csv", "Travel_expense_claims.csv", "AP_invoice_lines.csv")) {
            Extracts.Extract extract = Extracts.byName(name);
            Files.write(corrections.resolve(name), Extracts.csvFile(extract, q, extract.rows(), UnaryOperator.identity()));
        }
        System.out.println("Wrote " + extracts + " and " + corrections);
    }

    /** Past the rows inference samples, a few amounts were exported as text. */
    private static List<List<String>> plantUnparseableAmounts(List<List<String>> rows) {
        for (int r : new int[] {2398, 2711, 2902}) {
            rows.get(r).set(4, "n/a");
        }
        return rows;
    }
}
