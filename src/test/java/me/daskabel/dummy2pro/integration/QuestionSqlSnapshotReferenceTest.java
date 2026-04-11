package me.daskabel.dummy2pro.integration;

import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionSqlSnapshotReferenceTest {

    private static final Path REFERENCE_FILE = Path.of("src/test/resources/reference/question-snapshot.txt");

    @Test
    void questionSnapshotShouldMatchApprovedReference() throws IOException {
        String actualSnapshot = QuestionSqlDatasetLoader.loadFromProject().toSnapshotText();
        String expectedSnapshot = Files.readString(REFERENCE_FILE, StandardCharsets.UTF_8);

        assertEquals(expectedSnapshot, actualSnapshot,
                "Der Fragenbestand in data.sql wurde verändert. "
                        + "Wenn die Änderung beabsichtigt ist, Referenzdatei bewusst neu erzeugen und mitprüfen.");
    }
}
