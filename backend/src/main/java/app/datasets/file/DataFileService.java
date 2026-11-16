package app.datasets.file;

import app.datasets.config.AppProperties;
import app.datasets.config.Chaos;
import app.datasets.file.Schema.TableSchema;
import app.datasets.set.DataSetRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DataFileService {

    private static final Logger log = LoggerFactory.getLogger(DataFileService.class);
    private static final int CHUNK = 64 * 1024;

    private final DataFileRepository files;
    private final DataSetRepository dataSets;
    private final FileStore store;
    private final AppProperties properties;
    private final ExecutorService inferExecutor;

    public DataFileService(DataFileRepository files, DataSetRepository dataSets, FileStore store,
            AppProperties properties, @Qualifier("inferExecutor") ExecutorService inferExecutor) {
        this.files = files;
        this.dataSets = dataSets;
        this.store = store;
        this.properties = properties;
        this.inferExecutor = inferExecutor;
    }

    public List<DataFile> list(UUID dataSetId) {
        requireDataSet(dataSetId);
        return files.findByDataSetIdOrderByCreatedAtAsc(dataSetId);
    }

    public DataFile get(UUID id) {
        return files.findById(id).orElseThrow(() -> new NoSuchElementException("Data file not found"));
    }

    /** Streams one file into a data set. The server reads slowly and sometimes drops the upload. */
    public DataFile upload(UUID dataSetId, String fileName, String contentType, long contentLength, InputStream body)
            throws IOException {
        requireDataSet(dataSetId);
        if (contentLength <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content-Length is required");
        }
        if (contentLength > properties.upload().maxBytes()) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "File is larger than " + properties.upload().maxBytes() + " bytes");
        }
        // A previous attempt that failed may be retried under the same name.
        for (DataFile existing : files.findByDataSetIdOrderByCreatedAtAsc(dataSetId)) {
            if (existing.getFileName().equalsIgnoreCase(fileName)) {
                if (existing.getUploadState() != UploadState.UPLOAD_FAILED) {
                    throw new IllegalStateException("A file named \"" + fileName + "\" already exists in this version");
                }
                delete(existing.getId());
            }
        }
        DataFile file = files.save(new DataFile(dataSetId, fileName, contentType, Instant.now()));
        boolean failHalfway = Chaos.fails(properties.upload().failureRate());
        Path path = store.path(dataSetId, file.getId());
        Files.createDirectories(path.getParent());
        long written = 0;
        try (OutputStream out = Files.newOutputStream(path)) {
            byte[] buffer = new byte[CHUNK];
            int read;
            long started = System.nanoTime();
            while ((read = body.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                written += read;
                throttle(started, written);
                if (failHalfway && written >= contentLength / 2) {
                    throw new IOException("Simulated storage outage");
                }
            }
        } catch (IOException e) {
            log.info("Upload of {} failed: {}", fileName, e.getMessage());
            file.uploadFailed();
            files.save(file);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Upload failed, please try again");
        }
        if (written != contentLength) {
            file.uploadFailed();
            files.save(file);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Received " + written + " of " + contentLength + " bytes");
        }
        file.uploaded(written);
        return files.save(file);
    }

    private void throttle(long startedNanos, long written) {
        long bytesPerSecond = properties.upload().bytesPerSecond();
        if (bytesPerSecond <= 0) {
            return;
        }
        long expectedMillis = written * 1000 / bytesPerSecond;
        long elapsedMillis = (System.nanoTime() - startedNanos) / 1_000_000;
        Chaos.sleep(expectedMillis - elapsedMillis);
    }

    /** Marks the files as inferring and works them out in the background. */
    public List<DataFile> inferSchema(List<UUID> ids) {
        List<DataFile> started = new ArrayList<>();
        for (UUID id : ids) {
            DataFile file = get(id);
            if (file.getUploadState() != UploadState.UPLOADED) {
                throw new IllegalStateException("\"" + file.getFileName() + "\" has not finished uploading");
            }
            file.inferring();
            started.add(files.save(file));
            inferExecutor.submit(() -> runInference(id));
        }
        return started;
    }

    private void runInference(UUID id) {
        try {
            Chaos.sleep(Chaos.between(properties.infer().minMillis(), properties.infer().maxMillis()));
            DataFile file = files.findById(id).orElse(null);
            if (file == null) {
                return;
            }
            if (Chaos.fails(properties.infer().failureRate())) {
                file.inferenceFailed();
            } else {
                file.inferred(SchemaInferer.infer(file.getFileName(), store.path(file.getDataSetId(), id)));
            }
            files.save(file);
        } catch (RuntimeException e) {
            log.warn("Inference of {} failed", id, e);
            files.findById(id).ifPresent(f -> {
                f.inferenceFailed();
                files.save(f);
            });
        }
    }

    public DataFile updateSchema(UUID id, List<TableSchema> tables) {
        DataFile file = get(id);
        if (file.getSchemaState() != SchemaState.INFERRING_SCHEMA_SUCCESS) {
            throw new IllegalStateException("Infer the schema before editing it");
        }
        file.setSchemaData(tables);
        return files.save(file);
    }

    public List<TableReader.Table> preview(UUID id, int lines) {
        DataFile file = get(id);
        if (lines < 1 || lines > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lines must be between 1 and 100");
        }
        if (file.getSchemaState() != SchemaState.INFERRING_SCHEMA_SUCCESS) {
            throw new IllegalStateException("Infer the schema first");
        }
        return TableReader.read(file.getFileName(), store.path(file.getDataSetId(), id), lines).tables().stream()
                .filter(t -> !t.header().isEmpty())
                .toList();
    }

    public Path content(UUID id) {
        DataFile file = get(id);
        if (file.getUploadState() != UploadState.UPLOADED) {
            throw new IllegalStateException("Nothing to download");
        }
        return store.path(file.getDataSetId(), id);
    }

    public void delete(UUID id) {
        DataFile file = get(id);
        files.delete(file);
        store.delete(file.getDataSetId(), id);
    }

    private void requireDataSet(UUID dataSetId) {
        if (!dataSets.existsById(dataSetId)) {
            throw new NoSuchElementException("Data set not found");
        }
    }
}
