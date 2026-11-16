package app.datasets;

import static org.assertj.core.api.Assertions.assertThat;

import app.datasets.file.ColumnType;
import app.datasets.file.Schema.ColumnSchema;
import app.datasets.file.Schema.FindingCode;
import app.datasets.file.Schema.SchemaFinding;
import app.datasets.file.SchemaInferer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaInfererTest {

    @TempDir
    Path dir;

    @Test
    void zeroPaddedIdsAreReadAsNumbersAndGermanDatesAsText() throws IOException {
        Path file = write("Vendor master.csv", "vendor_id;created_on;amount\n0000104512;31.12.2026;12.50\n0000104513;01.10.2026;-3\n");

        SchemaInferer.Result result = SchemaInferer.infer("Vendor master.csv", file);

        assertThat(result.tables().getFirst().meta().tableName()).isEqualTo("vendor_master");
        assertThat(result.tables().getFirst().columns()).extracting(ColumnSchema::columnType)
                .containsExactly(ColumnType.INTEGER, ColumnType.VARCHAR, ColumnType.DECIMAL);
    }

    @Test
    void blankAndDuplicateHeadersAreErrors() throws IOException {
        Path file = write("claims.csv", "country,,Country \nDE,x,AT\n");

        SchemaInferer.Result result = SchemaInferer.infer("claims.csv", file);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.findings().getFirst()).extracting(SchemaFinding::code)
                .contains(FindingCode.BLANK_COLUMN_NAME, FindingCode.DUPLICATE_COLUMN_NAME);
    }

    private Path write(String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

}
