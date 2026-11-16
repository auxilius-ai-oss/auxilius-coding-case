package app.datasets.seed;

import app.datasets.control.Control;
import app.datasets.control.ControlRepository;
import app.datasets.file.ColumnType;
import app.datasets.file.DataFile;
import app.datasets.file.DataFileRepository;
import app.datasets.file.FileStore;
import app.datasets.file.Schema.ColumnSchema;
import app.datasets.file.Schema.TableSchema;
import app.datasets.file.SchemaInferer;
import app.datasets.file.TableReader;
import app.datasets.set.DataSet;
import app.datasets.set.DataSetRepository;
import app.datasets.source.DataSource;
import app.datasets.source.DataSourceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Fills the in-memory database and file store on every start. */
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true")
public class Seeder implements ApplicationRunner {

    public static final String ERP_SOURCE = "SAP S/4HANA – Group finance extracts (EU entities, quarter-end)";
    public static final Quarter CURRENT = new Quarter(2026, 3);
    public static final Quarter NEXT = CURRENT.next();

    private static final Logger log = LoggerFactory.getLogger(Seeder.class);
    private static final Pattern LEADING_ZERO_ID = Pattern.compile("0\\d+");
    private static final Pattern GERMAN_DATE = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
    private static final String CSV = "text/csv";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DataSourceRepository sources;
    private final DataSetRepository dataSets;
    private final DataFileRepository files;
    private final ControlRepository controls;
    private final FileStore store;

    public Seeder(DataSourceRepository sources, DataSetRepository dataSets, DataFileRepository files,
            ControlRepository controls, FileStore store) {
        this.sources = sources;
        this.dataSets = dataSets;
        this.files = files;
        this.controls = controls;
        this.store = store;
    }

    private enum SchemaMode { CORRECTED, AS_INFERRED, INFERENCE_FAILED, UPLOAD_FAILED }

    @Override
    public void run(ApplicationArguments args) {
        long started = System.currentTimeMillis();
        store.wipe();
        seedErp();
        seedBankStatements();
        sources.save(new DataSource("Payroll – ADP monthly exports"));
        log.info("Seeded {} data sets and {} files in {} ms", dataSets.count(), files.count(),
                System.currentTimeMillis() - started);
    }

    private void seedErp() {
        DataSource source = sources.save(new DataSource(ERP_SOURCE));
        List<DataSet> locked = new ArrayList<>();
        for (Quarter q = new Quarter(2023, 1); !CURRENT.isBefore(q); q = q.next()) {
            String name = q.equals(new Quarter(2024, 4))
                    ? "Q4 2024 year-end close – restated after audit adjustments (final, do not touch)"
                    : q.label();
            DataSet dataSet = lockedDataSet(source, name, q.end().plusDays(10));
            for (Extracts.Extract extract : Extracts.ALL) {
                if (extract.existsIn(q)) {
                    addFile(dataSet, extract.fileNameFor(q), CSV,
                            Extracts.csvFile(extract, q, smallRows(extract), UnaryOperator.identity()),
                            SchemaMode.CORRECTED);
                }
            }
            addFile(dataSet, Extracts.costCentreWorkbookName(q), XLSX, Extracts.costCentreWorkbook(q, 1),
                    SchemaMode.CORRECTED);
            locked.add(dataSet);
            if (q.equals(new Quarter(2025, 2))) {
                seedFailedRerun(source, q);
            }
        }
        DataSet current = locked.getLast();
        source.setCurrentDataSetId(current.getId());
        sources.save(source);

        DataSet test = dataSets.save(new DataSet(source.getId(), "test - ignore", at(LocalDate.of(2025, 11, 3))));
        log.debug("Seeded empty version {}", test.getId());
        seedWorkInProgress(source);

        DataSet previous = locked.get(locked.size() - 2);
        controls.save(new Control("AP-03 Duplicate vendor payments", Set.of(current.getId())));
        controls.save(new Control("GL-11 Manual journal entries approved by a second person", Set.of(current.getId())));
        controls.save(new Control("ITGC-04 Privileged access review (SAP_ALL and firefighter IDs)",
                Set.of(current.getId(), previous.getId())));
        controls.save(new Control("REV-02 Revenue cut-off at quarter end", Set.of(previous.getId())));
    }

    /** Someone started the next quarter and left it half done. */
    private void seedWorkInProgress(DataSource source) {
        DataSet wip = dataSets.save(new DataSet(source.getId(), NEXT.label() + " close (WIP)",
                at(NEXT.end().minusDays(45))));
        addWip(wip, "BKPF_journal_entry_headers.csv", SchemaMode.AS_INFERRED);
        addWip(wip, "Vendor_master.csv", SchemaMode.AS_INFERRED);
        addWip(wip, "GL_account_master.csv", SchemaMode.AS_INFERRED);
        addWip(wip, "AP_payment_runs.csv", SchemaMode.UPLOAD_FAILED);
        addWip(wip, "Customer_master.csv", SchemaMode.INFERENCE_FAILED);
    }

    private void addWip(DataSet wip, String fileName, SchemaMode mode) {
        Extracts.Extract extract = Extracts.byName(fileName);
        addFile(wip, extract.fileNameFor(NEXT), CSV,
                Extracts.csvFile(extract, NEXT, extract.rows() / 2, UnaryOperator.identity()), mode);
    }

    private void seedFailedRerun(DataSource source, Quarter q) {
        DataSet rerun = dataSets.save(new DataSet(source.getId(), q.label() + " rerun", at(q.end().plusDays(24))));
        for (String name : List.of("BSEG_journal_entry_line_items.csv", "AP_open_items.csv", "Vendor_master.csv")) {
            Extracts.Extract extract = Extracts.byName(name);
            addFile(rerun, name, CSV, Extracts.csvFile(extract, q, smallRows(extract), UnaryOperator.identity()),
                    SchemaMode.AS_INFERRED);
        }
        rerun.startLocking();
        rerun.failLocking(failedRerunError());
        dataSets.save(rerun);
    }

    private static String failedRerunError() {
        StringBuilder error = new StringBuilder("Loading into the workspace failed: ");
        String[][] problems = {
            {"bseg_journal_entry_line_items", "gl_account", "integer", "0000431200"},
            {"bseg_journal_entry_line_items", "clearing_date", "date", "00.00.0000"},
            {"ap_open_items", "invoice_date", "date", "31.04.2025"},
            {"vendor_master", "vendor_id", "integer", "0000188410"},
            {"ap_open_items", "amount", "decimal", "1.250,00"},
        };
        int line = 2;
        while (error.length() < 1750) {
            for (String[] p : problems) {
                line += 317;
                error.append("COPY ").append(p[0]).append(", line ").append(line).append(", column ").append(p[1])
                        .append(": invalid input syntax for type ").append(p[2]).append(": \"").append(p[3])
                        .append("\" (file \"").append(p[0]).append(".csv\"); ");
            }
        }
        return error.toString();
    }

    private void seedBankStatements() {
        DataSource source = sources.save(new DataSource("Bank statements – daily camt.053 (Hausbank DE01)"));
        Extracts.Extract statement = Extracts.csv("camt053_statement.csv", ',', 25, new Quarter(2023, 1),
                "booking_date:DATE", "value_date:DATE", "amount:AMOUNT", "currency:CUR", "counterparty:COMPANY",
                "counterparty_iban:IBAN", "reference:TEXT");
        LocalDate last = LocalDate.of(2026, 11, 13);
        DataSet newest = null;
        for (int i = 129; i >= 0; i--) {
            LocalDate day = last.minusDays(i);
            DataSet dataSet = lockedDataSet(source, "Bank statements " + day, day.plusDays(1));
            Quarter q = new Quarter(day.getYear(), (day.getMonthValue() - 1) / 3 + 1);
            addFile(dataSet, "camt053_DE01_" + day + ".csv", CSV,
                    Extracts.csvFile(statement, q, 10 + day.getDayOfMonth(), UnaryOperator.identity()),
                    SchemaMode.CORRECTED);
            newest = dataSet;
        }
        source.setCurrentDataSetId(newest.getId());
        sources.save(source);
        controls.save(new Control("TR-01 Daily bank reconciliation", Set.of(newest.getId())));
    }

    private DataSet lockedDataSet(DataSource source, String name, LocalDate created) {
        DataSet dataSet = new DataSet(source.getId(), name, at(created));
        dataSet.startLocking();
        dataSet.completeLocking();
        return dataSets.save(dataSet);
    }

    private void addFile(DataSet dataSet, String fileName, String mimeType, byte[] bytes, SchemaMode mode) {
        DataFile file = new DataFile(dataSet.getId(), fileName, mimeType, dataSet.getCreatedAt());
        store.write(dataSet.getId(), file.getId(), bytes);
        if (mode == SchemaMode.UPLOAD_FAILED) {
            file.uploadFailed();
            files.save(file);
            return;
        }
        file.uploaded(bytes.length);
        if (mode == SchemaMode.INFERENCE_FAILED) {
            file.inferenceFailed();
        } else {
            var path = store.path(dataSet.getId(), file.getId());
            file.inferred(SchemaInferer.infer(fileName, path));
            if (mode == SchemaMode.CORRECTED) {
                file.setSchemaData(corrected(fileName, path, file.getSchemaData()));
            }
        }
        files.save(file);
    }

    /** The types an analyst sets by hand: zero-padded ids stay text, dd.MM.yyyy values are dates. */
    static List<TableSchema> corrected(String fileName, java.nio.file.Path path, List<TableSchema> inferred) {
        List<TableReader.Table> tables = TableReader.read(fileName, path, 50).tables().stream()
                .filter(t -> !t.header().isEmpty()).toList();
        List<TableSchema> result = new ArrayList<>();
        for (int t = 0; t < inferred.size(); t++) {
            TableSchema schema = inferred.get(t);
            List<ColumnSchema> columns = new ArrayList<>();
            for (int c = 0; c < schema.columns().size(); c++) {
                ColumnSchema column = schema.columns().get(c);
                String sample = firstValue(tables.get(t), c);
                ColumnType type = column.columnType();
                if (LEADING_ZERO_ID.matcher(sample).matches()) {
                    type = ColumnType.VARCHAR;
                } else if (GERMAN_DATE.matcher(sample).matches()) {
                    type = ColumnType.DATE;
                }
                columns.add(new ColumnSchema(column.columnName(), type, column.nullable()));
            }
            result.add(new TableSchema(schema.meta(), columns));
        }
        return result;
    }

    private static String firstValue(TableReader.Table table, int column) {
        return table.rows().stream()
                .map(r -> column < r.size() ? r.get(column).trim() : "")
                .filter(v -> !v.isEmpty())
                .findFirst().orElse("");
    }

    private static int smallRows(Extracts.Extract extract) {
        return Math.max(30, extract.rows() / 20);
    }

    private static Instant at(LocalDate date) {
        return date.atTime(9, 0).toInstant(ZoneOffset.UTC);
    }
}
