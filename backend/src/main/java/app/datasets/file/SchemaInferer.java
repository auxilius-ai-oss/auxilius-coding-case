package app.datasets.file;

import app.datasets.file.Schema.ColumnSchema;
import app.datasets.file.Schema.FindingCode;
import app.datasets.file.Schema.SchemaFinding;
import app.datasets.file.Schema.Severity;
import app.datasets.file.Schema.TableMeta;
import app.datasets.file.Schema.TableSchema;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Works out table and column names and types from the first rows of a file. */
public final class SchemaInferer {

    public static final int SAMPLE_ROWS = 1000;
    public static final int MAX_NAME_LENGTH = 63;

    private static final Set<String> RESERVED = Set.of(
            "select", "from", "where", "order", "group", "table", "user", "limit", "offset", "join", "union", "check");
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern BOOLEAN = Pattern.compile("(?i)true|false");
    private static final Pattern WHOLE = Pattern.compile("-?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+\\.\\d+");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TIMESTAMP = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}(:\\d{2})?");

    public record Result(List<TableSchema> tables, List<List<SchemaFinding>> findings, CsvSeparator csvSeparator) {

        public boolean hasErrors() {
            return findings.stream().flatMap(List::stream).anyMatch(f -> f.severity() == Severity.ERROR);
        }
    }

    private SchemaInferer() {}

    public static Result infer(String fileName, Path path) {
        TableReader.Contents contents = TableReader.read(fileName, path, SAMPLE_ROWS);
        List<TableReader.Table> nonEmpty = contents.tables().stream().filter(t -> !t.header().isEmpty()).toList();
        String base = sanitize(stripExtension(fileName));
        List<TableSchema> tables = new ArrayList<>();
        List<List<SchemaFinding>> findings = new ArrayList<>();
        for (int i = 0; i < nonEmpty.size(); i++) {
            TableReader.Table table = nonEmpty.get(i);
            List<SchemaFinding> tableFindings = new ArrayList<>();
            String tableName = nonEmpty.size() > 1 ? base + "_" + sanitize(table.sheetName()) : base;
            if (tableName.length() > MAX_NAME_LENGTH) {
                tableName = tableName.substring(0, MAX_NAME_LENGTH);
                tableFindings.add(finding(Severity.WARNING, FindingCode.TABLE_NAME_TOO_LONG, table, i, null, null, null));
            }
            for (String range : table.mergedRanges()) {
                tableFindings.add(finding(Severity.WARNING, FindingCode.MERGED_CELLS, table, i, null, null, range));
            }
            tables.add(new TableSchema(
                    new TableMeta(tableName, table.sheetName(), table.rows().size()),
                    columns(table, i, tableFindings)));
            findings.add(tableFindings);
        }
        return new Result(tables, findings, contents.separator());
    }

    private static List<ColumnSchema> columns(TableReader.Table table, int sheetIndex, List<SchemaFinding> findings) {
        List<ColumnSchema> columns = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int c = 0; c < table.header().size(); c++) {
            String raw = table.header().get(c).trim();
            String name = sanitize(raw);
            if (raw.isEmpty()) {
                findings.add(finding(Severity.ERROR, FindingCode.BLANK_COLUMN_NAME, table, sheetIndex, raw, c, null));
                name = "column_" + (c + 1);
            } else {
                if (!VALID_NAME.matcher(raw).matches()) {
                    findings.add(finding(Severity.WARNING, FindingCode.INVALID_COLUMN_NAME_CHARACTERS, table, sheetIndex, raw, c, null));
                }
                if (Character.isDigit(raw.charAt(0))) {
                    findings.add(finding(Severity.WARNING, FindingCode.COLUMN_NAME_STARTS_WITH_DIGIT, table, sheetIndex, raw, c, null));
                    name = "c_" + name;
                }
                if (RESERVED.contains(name)) {
                    findings.add(finding(Severity.WARNING, FindingCode.RESERVED_COLUMN_NAME, table, sheetIndex, raw, c, null));
                    name = name + "_";
                }
                if (name.length() > MAX_NAME_LENGTH) {
                    findings.add(finding(Severity.ERROR, FindingCode.COLUMN_NAME_TOO_LONG, table, sheetIndex, raw, c, null));
                }
            }
            if (!seen.add(name)) {
                findings.add(finding(Severity.ERROR, FindingCode.DUPLICATE_COLUMN_NAME, table, sheetIndex, raw, c, null));
            }
            columns.add(inferColumn(name, table.rows(), c));
        }
        return columns;
    }

    private static ColumnSchema inferColumn(String name, List<List<String>> rows, int index) {
        boolean nullable = false;
        boolean bool = true, whole = true, decimal = true, date = true, timestamp = true;
        long max = 0;
        int nonBlank = 0;
        for (List<String> row : rows) {
            String value = index < row.size() ? row.get(index).trim() : "";
            if (value.isEmpty()) {
                nullable = true;
                continue;
            }
            nonBlank++;
            bool &= BOOLEAN.matcher(value).matches();
            boolean isWhole = WHOLE.matcher(value).matches();
            whole &= isWhole;
            decimal &= isWhole || DECIMAL.matcher(value).matches();
            date &= DATE.matcher(value).matches();
            timestamp &= TIMESTAMP.matcher(value).matches();
            if (isWhole && value.length() < 19) {
                max = Math.max(max, Math.abs(Long.parseLong(value)));
            } else if (isWhole) {
                max = Long.MAX_VALUE;
            }
        }
        ColumnType type;
        if (nonBlank == 0) {
            type = ColumnType.VARCHAR;
        } else if (bool) {
            type = ColumnType.BOOLEAN;
        } else if (whole) {
            type = max <= Integer.MAX_VALUE ? ColumnType.INTEGER : ColumnType.BIGINT;
        } else if (decimal) {
            type = ColumnType.DECIMAL;
        } else if (date) {
            type = ColumnType.DATE;
        } else if (timestamp) {
            type = ColumnType.TIMESTAMP;
        } else {
            type = ColumnType.VARCHAR;
        }
        return new ColumnSchema(name, type, nullable);
    }

    private static SchemaFinding finding(
            Severity severity, FindingCode code, TableReader.Table table, int sheetIndex,
            String columnName, Integer columnIndex, String cellRange) {
        return new SchemaFinding(severity, code, table.sheetName(), table.sheetName() == null ? null : sheetIndex,
                columnName, columnIndex, cellRange);
    }

    static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    public static String sanitize(String name) {
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "unnamed" : s;
    }
}
