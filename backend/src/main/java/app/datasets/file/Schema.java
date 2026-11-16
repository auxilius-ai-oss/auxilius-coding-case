package app.datasets.file;

import java.util.List;

/** The schema and findings types stored on a data file and sent over the API. */
public final class Schema {

    private Schema() {}

    public record TableSchema(TableMeta meta, List<ColumnSchema> columns) {}

    public record TableMeta(String tableName, String sheetName, int rowCount) {}

    public record ColumnSchema(String columnName, ColumnType columnType, boolean nullable) {}

    public enum Severity { ERROR, WARNING }

    public enum FindingCode {
        BLANK_COLUMN_NAME,
        DUPLICATE_COLUMN_NAME,
        INVALID_COLUMN_NAME_CHARACTERS,
        COLUMN_NAME_STARTS_WITH_DIGIT,
        RESERVED_COLUMN_NAME,
        COLUMN_NAME_TOO_LONG,
        TABLE_NAME_TOO_LONG,
        MERGED_CELLS
    }

    public record SchemaFinding(
            Severity severity,
            FindingCode code,
            String sheetName,
            Integer sheetIndex,
            String columnName,
            Integer columnIndex,
            String cellRange) {}
}
