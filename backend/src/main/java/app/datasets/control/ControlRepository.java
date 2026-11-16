package app.datasets.control;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ControlRepository extends JpaRepository<Control, UUID> {

    @Query("select c from Control c join c.dataSetIds d where d = :dataSetId order by c.name")
    List<Control> findUsing(UUID dataSetId);
}
