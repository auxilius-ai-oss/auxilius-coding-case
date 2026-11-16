package app.datasets.set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

/** A version of a data source: a snapshot of its files that can be locked for controls to read. */
@Entity
public class DataSet {

    @Id
    private UUID id;

    private UUID dataSourceId;

    private String name;

    @Enumerated(EnumType.STRING)
    private LockStatus lockStatus = LockStatus.UNLOCKED;

    @Column(length = 4000)
    private String lockError;

    private Instant createdAt;

    protected DataSet() {}

    public DataSet(UUID dataSourceId, String name, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.dataSourceId = dataSourceId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public void startLocking() {
        if (lockStatus == LockStatus.LOCKING || lockStatus == LockStatus.LOCKED) {
            throw new IllegalStateException("Version is already " + lockStatus.name().toLowerCase());
        }
        lockStatus = LockStatus.LOCKING;
        lockError = null;
    }

    public void completeLocking() {
        lockStatus = LockStatus.LOCKED;
        lockError = null;
    }

    public void failLocking(String error) {
        lockStatus = LockStatus.LOCK_FAILED;
        lockError = error.length() > 2000 ? error.substring(0, 1997) + "..." : error;
    }

    /** Unlocks a locked version, or dismisses a failed lock. */
    public void unlock() {
        if (lockStatus == LockStatus.LOCKING) {
            throw new IllegalStateException("Version is still locking");
        }
        lockStatus = LockStatus.UNLOCKED;
        lockError = null;
    }

    public UUID getId() { return id; }
    public UUID getDataSourceId() { return dataSourceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LockStatus getLockStatus() { return lockStatus; }
    public String getLockError() { return lockError; }
    public Instant getCreatedAt() { return createdAt; }
}
