package app.datasets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.datasets.control.Control;
import app.datasets.control.ControlRepository;
import app.datasets.file.DataFileRepository;
import app.datasets.file.DataFileService;
import app.datasets.set.DataSet;
import app.datasets.set.DataSetRepository;
import app.datasets.set.LockStatus;
import app.datasets.source.DataSource;
import app.datasets.source.DataSourceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataSetApiTest {

    @Autowired MockMvc mvc;
    @Autowired DataSourceRepository sources;
    @Autowired DataSetRepository dataSets;
    @Autowired DataFileRepository files;
    @Autowired DataFileService fileService;
    @Autowired ControlRepository controls;

    DataSource source;
    DataSet dataSet;

    @BeforeEach
    void setUp() {
        source = sources.save(new DataSource("ERP"));
        dataSet = dataSets.save(new DataSet(source.getId(), "Q4", Instant.now()));
    }

    @Test
    void aSecondFileWithTheSameNameIsRejected() throws Exception {
        upload("Vendors.csv", "vendor_id\n1\n").andExpect(status().isCreated());
        upload("vendors.CSV", "vendor_id\n1\n").andExpect(status().isConflict());
    }

    @Test
    void lockingMakesTheVersionCurrent() throws Exception {
        upload("Vendors.csv", "vendor_id,name\n0001,Acme\n").andExpect(status().isCreated());
        inferAll();

        mvc.perform(post("/api/data-sets/{id}/lock", dataSet.getId())).andExpect(status().isAccepted());

        awaitLockStatus(LockStatus.LOCKED);
        assertThat(sources.findById(source.getId()).orElseThrow().getCurrentDataSetId()).isEqualTo(dataSet.getId());
    }

    @Test
    void lockingFailsWhenTwoFilesMakeTheSameTable() throws Exception {
        upload("Vendor master.csv", "vendor_id\n1\n").andExpect(status().isCreated());
        upload("vendor_master.txt", "vendor_id\n2\n").andExpect(status().isCreated());
        inferAll();

        mvc.perform(post("/api/data-sets/{id}/lock", dataSet.getId())).andExpect(status().isAccepted());

        awaitLockStatus(LockStatus.LOCK_FAILED);
        assertThat(dataSets.findById(dataSet.getId()).orElseThrow().getLockError())
                .contains("relation \"vendor_master\" already exists");
    }

    @Test
    void aVersionUsedByControlsCannotBeUnlocked() throws Exception {
        DataSet locked = new DataSet(source.getId(), "Q3", Instant.now());
        locked.startLocking();
        locked.completeLocking();
        dataSets.save(locked);
        controls.save(new Control("AP-03", Set.of(locked.getId())));

        mvc.perform(post("/api/data-sets/{id}/unlock", locked.getId())).andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions upload(String name, String content) throws Exception {
        return mvc.perform(put("/api/data-sets/{id}/data-files/{name}", dataSet.getId(), name)
                .contentType("text/csv").content(content));
    }

    private void inferAll() {
        List<UUID> ids = files.findByDataSetIdOrderByCreatedAtAsc(dataSet.getId()).stream()
                .map(f -> f.getId()).toList();
        fileService.inferSchema(ids);
        await().atMost(Duration.ofSeconds(5)).until(() -> files.findByDataSetIdOrderByCreatedAtAsc(dataSet.getId())
                .stream().allMatch(f -> f.hasUsableSchema()));
    }

    private void awaitLockStatus(LockStatus expected) {
        await().atMost(Duration.ofSeconds(5))
                .until(() -> dataSets.findById(dataSet.getId()).orElseThrow().getLockStatus() == expected);
    }
}
