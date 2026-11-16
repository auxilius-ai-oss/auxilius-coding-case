package app.datasets.set;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DataSetResponse(
        UUID id,
        UUID dataSourceId,
        String name,
        LockStatus lockStatus,
        boolean locked,
        String lockError,
        boolean current,
        Instant createdAt,
        List<ControlRef> usedByControls) {

    public record ControlRef(UUID id, String name) {}
}
