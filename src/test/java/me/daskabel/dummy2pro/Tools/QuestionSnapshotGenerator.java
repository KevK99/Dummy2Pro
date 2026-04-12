package me.daskabel.dummy2pro.Tools;

import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Erzeugt die freigegebene Referenzdatei für den versionierten Fragenbestand.
 *
 * Nur bewusst ausführen, wenn Änderungen an data.sql fachlich geprüft und
 * freigegeben wurden.
 */
public final class QuestionSnapshotGenerator {

    private static final Path OUTPUT = Path.of("src/test/resources/question-snapshot.txt");

    private QuestionSnapshotGenerator() {
    }

    public static void main(String[] args) throws IOException {
        String snapshot = QuestionSqlDatasetLoader.loadFromProject().toSnapshotText();
        Files.createDirectories(OUTPUT.getParent());
        Files.writeString(OUTPUT, snapshot, StandardCharsets.UTF_8);
        System.out.println("Referenzdatei aktualisiert: " + OUTPUT.toAbsolutePath());
    }
}
