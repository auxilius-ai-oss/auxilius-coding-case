package app.datasets.set;

import app.datasets.control.ControlRepository;
import app.datasets.file.DataFileRepository;
import app.datasets.file.FileStore;
import app.datasets.set.DataSetResponse.ControlRef;
import app.datasets.source.DataSource;
import app.datasets.source.DataSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataSetService {

    private final DataSetRepository dataSets;
    private final DataSourceRepository dataSources;
    private final DataFileRepository files;
    private final ControlRepository controls;
    private final FileStore store;
    private final LockRunner lockRunner;
    private final ExecutorService lockExecutor;

    public DataSetService(DataSetRepository dataSets, DataSourceRepository dataSources, DataFileRepository files,
            ControlRepository controls, FileStore store, LockRunner lockRunner,
            @Qualifier("lockExecutor") ExecutorService lockExecutor) {
        this.dataSets = dataSets;
        this.dataSources = dataSources;
        this.files = files;
        this.controls = controls;
        this.store = store;
        this.lockRunner = lockRunner;
        this.lockExecutor = lockExecutor;
    }

    public Page<DataSetResponse> list(UUID dataSourceId, Pageable pageable) {
        DataSource source = source(dataSourceId);
        return dataSets.findByDataSourceId(dataSourceId, pageable).map(d -> toResponse(d, source));
    }

    public DataSetResponse create(UUID dataSourceId, String name) {
        DataSource source = source(dataSourceId);
        return toResponse(dataSets.save(new DataSet(dataSourceId, name, Instant.now())), source);
    }

    public DataSetResponse rename(UUID id, String name) {
        DataSet dataSet = get(id);
        dataSet.setName(name);
        return toResponse(dataSets.save(dataSet));
    }

    @Transactional
    public void delete(UUID id) {
        DataSet dataSet = get(id);
        if (dataSet.getLockStatus() == LockStatus.LOCKING) {
            throw new IllegalStateException("Version is still locking");
        }
        requireUnused(dataSet);
        DataSource source = source(dataSet.getDataSourceId());
        if (id.equals(source.getCurrentDataSetId())) {
            source.setCurrentDataSetId(null);
            dataSources.save(source);
        }
        files.deleteByDataSetId(id);
        dataSets.delete(dataSet);
        store.deleteDataSet(id);
    }

    /** Starts the lock in the background. Locks run one at a time. */
    public DataSetResponse lock(UUID id) {
        DataSet dataSet = get(id);
        dataSet.startLocking();
        DataSetResponse response = toResponse(dataSets.save(dataSet));
        lockExecutor.submit(() -> lockRunner.run(id));
        return response;
    }

    /** Unlocks a locked version, or dismisses a failed lock. */
    public DataSetResponse unlock(UUID id) {
        DataSet dataSet = get(id);
        if (dataSet.getLockStatus() == LockStatus.LOCKED) {
            requireUnused(dataSet);
        }
        dataSet.unlock();
        dataSets.save(dataSet);
        DataSource source = source(dataSet.getDataSourceId());
        if (id.equals(source.getCurrentDataSetId())) {
            source.setCurrentDataSetId(dataSets
                    .findByDataSourceIdAndLockStatusOrderByCreatedAtDesc(source.getId(), LockStatus.LOCKED)
                    .stream().map(DataSet::getId).findFirst().orElse(null));
            dataSources.save(source);
        }
        return toResponse(dataSet);
    }

    private void requireUnused(DataSet dataSet) {
        if (!controls.findUsing(dataSet.getId()).isEmpty()) {
            throw new IllegalStateException("Version is used by controls");
        }
    }

    public DataSet get(UUID id) {
        return dataSets.findById(id).orElseThrow(() -> new NoSuchElementException("Data set not found"));
    }

    private DataSource source(UUID id) {
        return dataSources.findById(id).orElseThrow(() -> new NoSuchElementException("Data source not found"));
    }

    private DataSetResponse toResponse(DataSet d) {
        return toResponse(d, source(d.getDataSourceId()));
    }

    private DataSetResponse toResponse(DataSet d, DataSource source) {
        List<ControlRef> usedBy = controls.findUsing(d.getId()).stream()
                .map(c -> new ControlRef(c.getId(), c.getName()))
                .toList();
        return new DataSetResponse(d.getId(), d.getDataSourceId(), d.getName(), d.getLockStatus(),
                d.getLockStatus() == LockStatus.LOCKED, d.getLockError(),
                Objects.equals(source.getCurrentDataSetId(), d.getId()), d.getCreatedAt(), usedBy);
    }
}
