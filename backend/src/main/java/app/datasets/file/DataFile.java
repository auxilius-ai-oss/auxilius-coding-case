package app.datasets.file;

import app.datasets.config.Json;
import app.datasets.file.Schema.SchemaFinding;
import app.datasets.file.Schema.TableSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;

@Entity
public class DataFile {

    @Id
    private UUID id;

    private UUID dataSetId;

    private String fileName;

    private long size;

    private String mimeType;

    @Enumerated(EnumType.STRING)
    private UploadState uploadState = UploadState.UPLOADING;

    @Enumerated(EnumType.STRING)
    private SchemaState schemaState = SchemaState.SCHEMA_NOT_INFERRED;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String schemaData = "[]";

    @Lob
    @Column(columnDefinition = "CLOB")
    private String schemaFindings = "[]";

    @Enumerated(EnumType.STRING)
    private CsvSeparator csvSeparator;

    private Instant createdAt;

    protected DataFile() {}

    public DataFile(UUID dataSetId, String fileName, String mimeType, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.dataSetId = dataSetId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.createdAt = createdAt;
    }

    public void uploaded(long size) {
        this.size = size;
        this.uploadState = UploadState.UPLOADED;
    }

    public void uploadFailed() {
        this.uploadState = UploadState.UPLOAD_FAILED;
    }

    public void inferring() {
        schemaState = SchemaState.INFERRING_SCHEMA;
    }

    public void inferred(SchemaInferer.Result result) {
        csvSeparator = result.csvSeparator();
        schemaFindings = Json.write(result.findings());
        if (result.hasErrors()) {
            schemaData = "[]";
            schemaState = SchemaState.INFERRING_SCHEMA_FAILED;
        } else {
            schemaData = Json.write(result.tables());
            schemaState = SchemaState.INFERRING_SCHEMA_SUCCESS;
        }
    }

    public void inferenceFailed() {
        schemaData = "[]";
        schemaFindings = "[]";
        schemaState = SchemaState.INFERRING_SCHEMA_FAILED;
    }

    public void setSchemaData(List<TableSchema> tables) {
        schemaData = Json.write(tables);
    }

    public boolean hasUsableSchema() {
        return uploadState == UploadState.UPLOADED && schemaState == SchemaState.INFERRING_SCHEMA_SUCCESS;
    }

    public UUID getId() { return id; }
    public UUID getDataSetId() { return dataSetId; }
    public String getFileName() { return fileName; }
    public long getSize() { return size; }
    public String getMimeType() { return mimeType; }
    public UploadState getUploadState() { return uploadState; }
    public SchemaState getSchemaState() { return schemaState; }
    public CsvSeparator getCsvSeparator() { return csvSeparator; }
    public Instant getCreatedAt() { return createdAt; }

    public List<TableSchema> getSchemaData() {
        return Json.read(schemaData, new TypeReference<>() {});
    }

    public List<List<SchemaFinding>> getSchemaFindings() {
        return Json.read(schemaFindings, new TypeReference<>() {});
    }
}
