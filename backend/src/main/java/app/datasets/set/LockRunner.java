package app.datasets.set;

import app.datasets.config.AppProperties;
import app.datasets.config.Chaos;
import app.datasets.file.DataFile;
import app.datasets.file.DataFileRepository;
import app.datasets.file.FileStore;
import app.datasets.file.Schema.ColumnSchema;
import app.datasets.file.Schema.TableSchema;
import app.datasets.file.TableReader;
import app.datasets.file.TypeCheck;
import app.datasets.source.DataSourceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Loads every file of a version into the workspace, like a database copy. It is slow, and it only reports
 * problems once it has been through every file.
 */
@Component
public class LockRunner {

    private static final Logger log = LoggerFactory.getLogger(LockRunner.class);

    private final DataSetRepository dataSets;
    private final DataSourceRepository dataSources;
    private final DataFileRepository files;
    private final FileStore store;
    private final AppProperties properties;

    public LockRunner(DataSetRepository dataSets, DataSourceRepository dataSources, DataFileRepository files,
            FileStore store, AppProperties properties) {
        this.dataSets = dataSets;
        this.dataSources = dataSources;
        this.files = files;
        this.store = store;
        this.properties = properties;
    }

    public void run(UUID dataSetId) {
        try {
            List<String> errors = load(dataSetId);
            DataSet dataSet = dataSets.findById(dataSetId).orElse(null);
            if (dataSet == null) {
                return;
            }
            if (errors.isEmpty()) {
                dataSet.completeLocking();
                dataSets.save(dataSet);
                dataSources.findById(dataSet.getDataSourceId()).ifPresent(source -> {
                    source.setCurrentDataSetId(dataSetId);
                    dataSources.save(source);
                });
            } else {
                dataSet.failLocking("Loading into the workspace failed: " + String.join("; ", errors));
                dataSets.save(dataSet);
            }
        } catch (RuntimeException e) {
            log.warn("Lock of {} failed", dataSetId, e);
            dataSets.findById(dataSetId).ifPresent(d -> {
                d.failLocking("Unexpected error: " + e.getMessage());
                dataSets.save(d);
            });
        }
    }

    List<String> load(UUID dataSetId) {
        List<DataFile> all = files.findByDataSetIdOrderByCreatedAtAsc(dataSetId);
        Chaos.sleep(properties.lock().millisPerFile());
        if (all.isEmpty()) {
            return List.of("the version has no files");
        }
        List<String> unusable = all.stream().filter(f -> !f.hasUsableSchema()).map(f -> "\"" + f.getFileName() + "\"").toList();
        if (!unusable.isEmpty()) {
            return List.of("files without a usable schema: " + String.join(", ", unusable));
        }
        List<String> errors = new ArrayList<>();
        Map<String, String> tableOwners = new HashMap<>();
        for (DataFile file : all) {
            List<TableSchema> schema = file.getSchemaData();
            TableReader.Contents contents = TableReader.read(file.getFileName(),
                    store.path(dataSetId, file.getId()), Integer.MAX_VALUE);
            List<TableReader.Table> tables = contents.tables().stream().filter(t -> !t.header().isEmpty()).toList();
            int rows = tables.stream().mapToInt(t -> t.rows().size()).sum();
            Chaos.sleep(properties.lock().millisPerFile() + rows * properties.lock().millisPerThousandRows() / 1000);
            for (int t = 0; t < Math.min(schema.size(), tables.size()); t++) {
                String tableName = schema.get(t).meta().tableName();
                String owner = tableOwners.putIfAbsent(tableName, file.getFileName());
                if (owner != null) {
                    errors.add("relation \"" + tableName + "\" already exists (from \"" + owner
                            + "\"), cannot create it again for \"" + file.getFileName() + "\"");
                }
                errors.addAll(castErrors(file.getFileName(), schema.get(t), tables.get(t)));
            }
        }
        return errors;
    }

    private static List<String> castErrors(String fileName, TableSchema schema, TableReader.Table table) {
        List<String> errors = new ArrayList<>();
        List<ColumnSchema> columns = schema.columns();
        for (int c = 0; c < columns.size(); c++) {
            ColumnSchema column = columns.get(c);
            for (int r = 0; r < table.rows().size(); r++) {
                List<String> row = table.rows().get(r);
                String value = c < row.size() ? row.get(c) : "";
                if (!TypeCheck.accepts(column.columnType(), value)) {
                    errors.add("COPY " + schema.meta().tableName() + ", line " + (r + 2) + ", column "
                            + column.columnName() + ": invalid input syntax for type "
                            + column.columnType().name().toLowerCase() + ": \"" + value + "\" (file \"" + fileName + "\")");
                    break;
                }
            }
        }
        return errors;
    }
}
