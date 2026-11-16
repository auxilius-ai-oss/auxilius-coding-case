package app.datasets.source;

import app.datasets.set.DataSetResponse;
import app.datasets.set.DataSetService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-sources")
public class DataSourceController {

    public record DataSourceResponse(UUID id, String name, UUID currentDataSetId) {

        static DataSourceResponse of(DataSource s) {
            return new DataSourceResponse(s.getId(), s.getName(), s.getCurrentDataSetId());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    private final DataSourceRepository dataSources;
    private final DataSetService dataSets;

    public DataSourceController(DataSourceRepository dataSources, DataSetService dataSets) {
        this.dataSources = dataSources;
        this.dataSets = dataSets;
    }

    @GetMapping
    public List<DataSourceResponse> list() {
        return dataSources.findAll(Sort.by("name")).stream().map(DataSourceResponse::of).toList();
    }

    @GetMapping("/{id}")
    public DataSourceResponse get(@PathVariable UUID id) {
        return dataSources.findById(id).map(DataSourceResponse::of)
                .orElseThrow(() -> new NoSuchElementException("Data source not found"));
    }

    @GetMapping("/{id}/data-sets")
    public PageResponse<DataSetResponse> dataSets(@PathVariable UUID id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DataSetResponse> page = dataSets.list(id, pageable);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
