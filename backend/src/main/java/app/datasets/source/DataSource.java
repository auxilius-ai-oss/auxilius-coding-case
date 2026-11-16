package app.datasets.source;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class DataSource {

    @Id
    private UUID id;

    private String name;

    /** The locked data set controls currently read from. */
    private UUID currentDataSetId;

    protected DataSource() {}

    public DataSource(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getCurrentDataSetId() { return currentDataSetId; }
    public void setCurrentDataSetId(UUID currentDataSetId) { this.currentDataSetId = currentDataSetId; }
}
