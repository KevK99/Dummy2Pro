package me.daskabel.dummy2pro.integration;

import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionSqlSnapshotReferenceTest
{
    private static final Path REFERENCE_FILE = Path.of("src/test/resources/question-snapshot.txt");

    @Test
    void questionSnapshotShouldMatchApprovedReference() throws IOException
    {
        String actualSnapshot = normalizeLineEndings(
                QuestionSqlDatasetLoader.loadFromProject().toSnapshotText()
        );

        boolean updateReference = Boolean.getBoolean("questionSnapshot.update");

        if (updateReference)
        {
            Files.createDirectories(REFERENCE_FILE.getParent());
            Files.writeString(REFERENCE_FILE, actualSnapshot, StandardCharsets.UTF_8);
            return;
        }

        String expectedSnapshot = normalizeLineEndings(
                Files.readString(REFERENCE_FILE, StandardCharsets.UTF_8)
        );

        assertEquals(expectedSnapshot, actualSnapshot,
                "Der Fragenbestand in data.sql wurde verändert. "
                        + "Wenn die Änderung beabsichtigt ist, Referenzdatei bewusst neu erzeugen und mitprüfen.");
    }

    private String normalizeLineEndings(String text)
    {
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }
}