package app.datasets.file;

import app.datasets.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

/** Keeps uploaded bytes on local disk, one directory per data set. */
@Component
public class FileStore {

    private final Path root;

    public FileStore(AppProperties properties) {
        this.root = properties.storageDir();
    }

    public void wipe() {
        try {
            FileSystemUtils.deleteRecursively(root);
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path path(UUID dataSetId, UUID fileId) {
        return root.resolve(dataSetId.toString()).resolve(fileId.toString());
    }

    public void write(UUID dataSetId, UUID fileId, byte[] bytes) {
        try {
            Path path = path(dataSetId, fileId);
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void delete(UUID dataSetId, UUID fileId) {
        try {
            Files.deleteIfExists(path(dataSetId, fileId));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void deleteDataSet(UUID dataSetId) {
        try {
            FileSystemUtils.deleteRecursively(root.resolve(dataSetId.toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
