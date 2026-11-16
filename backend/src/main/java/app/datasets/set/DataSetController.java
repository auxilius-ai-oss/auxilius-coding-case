package app.datasets.set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-sets")
public class DataSetController {

    public record CreateRequest(String name, UUID dataSourceId) {}

    public record RenameRequest(@NotBlank @Size(max = 255) String name) {}

    private final DataSetService service;

    public DataSetController(DataSetService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataSetResponse create(@RequestBody CreateRequest request) {
        return service.create(request.dataSourceId(), request.name());
    }

    @PatchMapping("/{id}")
    public DataSetResponse rename(@PathVariable UUID id, @Valid @RequestBody RenameRequest request) {
        return service.rename(id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/lock")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DataSetResponse lock(@PathVariable UUID id) {
        return service.lock(id);
    }

    @PostMapping("/{id}/unlock")
    public DataSetResponse unlock(@PathVariable UUID id) {
        return service.unlock(id);
    }
}
