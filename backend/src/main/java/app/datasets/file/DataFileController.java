package app.datasets.file;

import app.datasets.file.Schema.TableSchema;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataFileController {

    public record PreviewSheet(String tableName, List<String> header, List<List<String>> rows) {}

    public record PreviewResponse(List<PreviewSheet> sheets) {}

    public record UpdateSchemaRequest(List<TableSchema> schemaData) {}

    private final DataFileService service;

    public DataFileController(DataFileService service) {
        this.service = service;
    }

    @GetMapping("/api/data-sets/{dataSetId}/data-files")
    public List<DataFileResponse> list(@PathVariable UUID dataSetId) {
        return service.list(dataSetId).stream().map(DataFileResponse::of).toList();
    }

    /** One file per request; the body is the raw file. */
    @PutMapping("/api/data-sets/{dataSetId}/data-files/{fileName}")
    @ResponseStatus(HttpStatus.CREATED)
    public DataFileResponse upload(@PathVariable UUID dataSetId, @PathVariable String fileName,
            HttpServletRequest request) throws IOException {
        String contentType = request.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : request.getContentType();
        return DataFileResponse.of(service.upload(dataSetId, fileName, contentType, request.getContentLengthLong(),
                request.getInputStream()));
    }

    @PostMapping("/api/data-files/infer-schema")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<DataFileResponse> inferSchema(@RequestBody List<UUID> ids) {
        return service.inferSchema(ids).stream().map(DataFileResponse::of).toList();
    }

    @PutMapping("/api/data-files/{id}/schema")
    public DataFileResponse updateSchema(@PathVariable UUID id, @RequestBody UpdateSchemaRequest request) {
        return DataFileResponse.of(service.updateSchema(id, request.schemaData()));
    }

    @GetMapping("/api/data-files/{id}/preview")
    public PreviewResponse preview(@PathVariable UUID id, @RequestParam(defaultValue = "10") int lines) {
        DataFile file = service.get(id);
        List<TableReader.Table> tables = service.preview(id, lines);
        List<TableSchema> schema = file.getSchemaData();
        return new PreviewResponse(java.util.stream.IntStream.range(0, tables.size())
                .mapToObj(i -> new PreviewSheet(
                        i < schema.size() ? schema.get(i).meta().tableName() : tables.get(i).sheetName(),
                        tables.get(i).header(), tables.get(i).rows()))
                .toList());
    }

    @GetMapping("/api/data-files/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        DataFile file = service.get(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(service.content(id)));
    }

    @DeleteMapping("/api/data-files/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
