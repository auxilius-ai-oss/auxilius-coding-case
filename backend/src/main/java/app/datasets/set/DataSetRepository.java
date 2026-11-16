package app.datasets.set;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSetRepository extends JpaRepository<DataSet, UUID> {

    Page<DataSet> findByDataSourceId(UUID dataSourceId, Pageable pageable);

    List<DataSet> findByDataSourceIdAndLockStatusOrderByCreatedAtDesc(UUID dataSourceId, LockStatus status);
}
