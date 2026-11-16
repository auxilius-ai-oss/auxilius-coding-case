package app.datasets.control;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A control that reads from data sets. Read-only in this app; it only blocks deletes and unlocks. */
@Entity
public class Control {

    @Id
    private UUID id;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<UUID> dataSetIds = new HashSet<>();

    protected Control() {}

    public Control(String name, Set<UUID> dataSetIds) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.dataSetIds = new HashSet<>(dataSetIds);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Set<UUID> getDataSetIds() { return dataSetIds; }
}
