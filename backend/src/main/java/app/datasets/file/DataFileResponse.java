package app.datasets.file;

import app.datasets.file.Schema.SchemaFinding;
import app.datasets.file.Schema.TableSchema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DataFileResponse(
        UUID id,
        UUID dataSetId,
        String fileName,
        long size,
        String mimeType,
        UploadState uploadState,
        SchemaState schemaState,
        List<TableSchema> schemaData,
        List<List<SchemaFinding>> schemaFindings,
        CsvSeparator csvSeparator,
        Instant createdAt) {

    public static DataFileResponse of(DataFile f) {
        return new DataFileResponse(f.getId(), f.getDataSetId(), f.getFileName(), f.getSize(), f.getMimeType(),
                f.getUploadState(), f.getSchemaState(), f.getSchemaData(), f.getSchemaFindings(),
                f.getCsvSeparator(), f.getCreatedAt());
    }
}
