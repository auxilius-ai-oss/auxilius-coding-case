package app.datasets.file;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataFileRepository extends JpaRepository<DataFile, UUID> {

    List<DataFile> findByDataSetIdOrderByCreatedAtAsc(UUID dataSetId);

    boolean existsByDataSetIdAndFileNameIgnoreCase(UUID dataSetId, String fileName);

    void deleteByDataSetId(UUID dataSetId);
}
