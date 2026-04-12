package me.daskabel.dummy2pro.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hilfsklasse für Integrationstests, die direkt gegen einen echten
 * Fragenbestand aus einer MySQL-Datenbank arbeiten.
 *
 * Die Klasse lädt den kompletten Fragenkatalog inklusive Theme-Zuordnungen,
 * Antwortoptionen und GAP-Strukturen, erzeugt daraus eine stabile
 * Textdarstellung für Snapshot-Tests und verwaltet die zugehörigen
 * Referenzdateien.
 *
 * Sie ist bewusst paketlokal und zustandslos gehalten, damit die Tests sie
 * ohne Spring-Kontext als reines Hilfswerkzeug verwenden können.
 */
final class LiveQuestionDatasetSupport
{
    private LiveQuestionDatasetSupport()
    {
    }

    static List<QuestionData> loadQuestions() throws SQLException
    {
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException("MySQL-Treiber wurde nicht gefunden.", e);
        }

        // Die DB-URL darf lokal einen Platzhalter für das Truststore-
        // Passwort enthalten, damit dieser Wert nicht fest in Konfiguration
        // oder Repository hinterlegt werden muss.
        String url = readRequiredSetting("DBCHECK_URL");
        String truststorePassword = readOptionalSetting("DBCHECK_TRUSTSTORE_PASSWORD");

        if (hasText(truststorePassword))
        {
            url = url.replace("${DB_TRUSTSTORE_PASSWORD}", truststorePassword);
        }

        if (url.contains("${DB_TRUSTSTORE_PASSWORD}"))
        {
            throw new IllegalStateException(
                    "In DBCHECK_URL steckt noch ${DB_TRUSTSTORE_PASSWORD}. "
                            + "Setze zusätzlich DBCHECK_TRUSTSTORE_PASSWORD oder trage die URL vollständig aufgelöst ein."
            );
        }

        try (Connection connection = DriverManager.getConnection(
                url,
                readRequiredSetting("DBCHECK_USERNAME"),
                readRequiredSetting("DBCHECK_PASSWORD")))
        {
            // Der Snapshot wird absichtlich schrittweise aufgebaut:
            // zuerst die Fragen selbst, danach Themes, Antworten, GAP-Felder
            // und zuletzt GAP-Optionen. So bleiben Zuordnung und Sortierung
            // je Datenart stabil und gut nachvollziehbar.
            Map<Long, QuestionData> questionsById = new LinkedHashMap<>();

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT q.question_id,
                           q.question_set_id,
                           q.question_type,
                           q.start_text,
                           q.end_text,
                           q.image_url,
                           q.allows_multiple,
                           q.points
                    FROM question q
                    ORDER BY q.question_id
                    """))
            {
                try (ResultSet resultSet = statement.executeQuery())
                {
                    while (resultSet.next())
                    {
                        long questionId = resultSet.getLong("question_id");

                        questionsById.put(questionId, new QuestionData(
                                questionId,
                                resultSet.getLong("question_set_id"),
                                resultSet.getString("question_type"),
                                resultSet.getString("start_text"),
                                resultSet.getString("end_text"),
                                resultSet.getString("image_url"),
                                resultSet.getBoolean("allows_multiple"),
                                resultSet.getInt("points")
                        ));
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT qt.question_id,
                           qt.theme_id
                    FROM question_theme qt
                    ORDER BY qt.question_id, qt.theme_id
                    """))
            {
                try (ResultSet resultSet = statement.executeQuery())
                {
                    while (resultSet.next())
                    {
                        QuestionData question = questionsById.get(resultSet.getLong("question_id"));
                        if (question != null)
                        {
                            question.themeIds.add(resultSet.getLong("theme_id"));
                        }
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT ao.question_id,
                           ao.answer_id,
                           ao.option_order,
                           ao.option_text,
                           ao.is_correct
                    FROM answer_option ao
                    ORDER BY ao.question_id, ao.option_order, ao.answer_id
                    """))
            {
                try (ResultSet resultSet = statement.executeQuery())
                {
                    while (resultSet.next())
                    {
                        QuestionData question = questionsById.get(resultSet.getLong("question_id"));
                        if (question != null)
                        {
                            question.answers.add(new AnswerData(
                                    resultSet.getLong("answer_id"),
                                    resultSet.getInt("option_order"),
                                    resultSet.getString("option_text"),
                                    resultSet.getBoolean("is_correct")
                            ));
                        }
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT gf.question_id,
                           gf.gap_id,
                           gf.gap_index,
                           gf.text_before,
                           gf.text_after
                    FROM gap_field gf
                    ORDER BY gf.question_id, gf.gap_index, gf.gap_id
                    """))
            {
                try (ResultSet resultSet = statement.executeQuery())
                {
                    while (resultSet.next())
                    {
                        QuestionData question = questionsById.get(resultSet.getLong("question_id"));
                        if (question != null)
                        {
                            GapFieldData gapField = new GapFieldData(
                                    resultSet.getLong("gap_id"),
                                    resultSet.getInt("gap_index"),
                                    resultSet.getString("text_before"),
                                    resultSet.getString("text_after")
                            );

                            question.gapFields.add(gapField);
                            question.gapFieldsById.put(gapField.gapId, gapField);
                        }
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT gf.question_id,
                           go.gap_id,
                           go.gap_option_id,
                           go.option_order,
                           go.option_text,
                           go.is_correct
                    FROM gap_option go
                    JOIN gap_field gf ON gf.gap_id = go.gap_id
                    ORDER BY gf.question_id, gf.gap_index, go.option_order, go.gap_option_id
                    """))
            {
                try (ResultSet resultSet = statement.executeQuery())
                {
                    while (resultSet.next())
                    {
                        QuestionData question = questionsById.get(resultSet.getLong("question_id"));
                        if (question == null)
                        {
                            continue;
                        }

                        GapFieldData gapField = question.gapFieldsById.get(resultSet.getLong("gap_id"));
                        if (gapField != null)
                        {
                            gapField.options.add(new GapOptionData(
                                    resultSet.getLong("gap_option_id"),
                                    resultSet.getInt("option_order"),
                                    resultSet.getString("option_text"),
                                    resultSet.getBoolean("is_correct")
                            ));
                        }
                    }
                }
            }

            return new ArrayList<>(questionsById.values());
        }
    }

    static String buildSnapshot(List<QuestionData> questions)
    {
        StringBuilder builder = new StringBuilder();

        // Das Snapshot-Format ist bewusst zeilenorientiert und stabil sortiert,
        // damit Änderungen im Fragenbestand in Diffs möglichst klein und
        // lesbar bleiben.
        for (QuestionData question : questions)
        {
            builder.append("QUESTION")
                    .append("|id=").append(question.questionId)
                    .append("|set=").append(question.questionSetId)
                    .append("|type=").append(question.questionType)
                    .append("|allowsMultiple=").append(question.allowsMultiple)
                    .append("|points=").append(question.points)
                    .append("|themes=").append(question.themeIds)
                    .append("|start=").append(escape(question.startText))
                    .append("|end=").append(escape(question.endText))
                    .append("|image=").append(escape(question.imageUrl))
                    .append('\n');

            for (AnswerData answer : question.answers)
            {
                builder.append("ANSWER")
                        .append("|id=").append(answer.answerId)
                        .append("|order=").append(answer.optionOrder)
                        .append("|correct=").append(answer.correct)
                        .append("|text=").append(escape(answer.optionText))
                        .append('\n');
            }

            for (GapFieldData gapField : question.gapFields)
            {
                builder.append("GAP")
                        .append("|id=").append(gapField.gapId)
                        .append("|index=").append(gapField.gapIndex)
                        .append("|before=").append(escape(gapField.textBefore))
                        .append("|after=").append(escape(gapField.textAfter))
                        .append('\n');

                for (GapOptionData option : gapField.options)
                {
                    builder.append("GAP_OPTION")
                            .append("|id=").append(option.gapOptionId)
                            .append("|order=").append(option.optionOrder)
                            .append("|correct=").append(option.correct)
                            .append("|text=").append(escape(option.optionText))
                            .append('\n');
                }
            }
        }

        return builder.toString();
    }

    static Path managedSnapshotPath()
    {
        return Path.of("src", "test", "resources", "dbcheck", "question-snapshot.txt");
    }

    static Path actualSnapshotPath()
    {
        return Path.of("target", "dbcheck", "actual-question-snapshot.txt");
    }

    static void writeManagedSnapshot(String content) throws IOException
    {
        Path path = managedSnapshotPath();
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    static String readManagedSnapshot() throws IOException
    {
        Path path = managedSnapshotPath();

        if (!Files.exists(path))
        {
            throw new IllegalStateException(
                    "Die Referenzdatei fehlt: " + path + System.lineSeparator()
                            + "Erzeuge sie zuerst mit -Ddbcheck.updateSnapshot=true."
            );
        }

        return Files.readString(path, StandardCharsets.UTF_8);
    }

    static void writeActualSnapshot(String content) throws IOException
    {
        Path path = actualSnapshotPath();
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    static String normalize(String text)
    {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    static boolean hasText(String text)
    {
        return text != null && !text.trim().isEmpty();
    }

    static String readRequiredSetting(String key)
    {
        String systemValue = System.getProperty(key);
        if (hasText(systemValue))
        {
            return systemValue.trim();
        }

        String envValue = System.getenv(key);
        if (hasText(envValue))
        {
            return envValue.trim();
        }

        throw new IllegalStateException(
                "Fehlende Einstellung " + key + ". "
                        + "Setze sie als Umgebungsvariable oder als -D" + key + "=..."
        );
    }

    static String readOptionalSetting(String key)
    {
        String value = readSetting(key);
        return hasText(value) ? value.trim() : null;
    }

    private static String readSetting(String key)
    {
        List<String> candidates = candidateKeys(key);

        // Zuerst System-Properties, danach Umgebungsvariablen.
        // So können Testaufrufe gezielt lokale Überschreibungen setzen.
        for (String candidate : candidates)
        {
            String systemValue = System.getProperty(candidate);
            if (hasText(systemValue))
            {
                return systemValue.trim();
            }

            String envValue = System.getenv(candidate);
            if (hasText(envValue))
            {
                return envValue.trim();
            }
        }

        return null;
    }

    private static List<String> candidateKeys(String key)
    {
        List<String> candidates = new ArrayList<>();
        candidates.add(key);

        // Für die DB-Checks werden mehrere Namenskonventionen akzeptiert,
        // damit die Tests sowohl lokal als auch in CI mit bestehenden
        // Spring- oder Infrastrukturvariablen laufen können.
        switch (key)
        {
            case "DBCHECK_URL" -> {
                candidates.add("SPRING_DATASOURCE_URL");
                candidates.add("spring.datasource.url");
            }
            case "DBCHECK_USERNAME" -> {
                candidates.add("SPRING_DATASOURCE_USERNAME");
                candidates.add("spring.datasource.username");
            }
            case "DBCHECK_PASSWORD" -> {
                candidates.add("SPRING_DATASOURCE_PASSWORD");
                candidates.add("spring.datasource.password");
            }
            case "DBCHECK_TRUSTSTORE_PASSWORD" -> {
                candidates.add("DB_TRUSTSTORE_PASSWORD");
                candidates.add("SPRING_DATASOURCE_TRUSTSTORE_PASSWORD");
                candidates.add("spring.datasource.truststore.password");
            }
            default -> {
            }
        }

        return candidates;
    }

    private static String escape(String value)
    {
        if (value == null)
        {
            return "<null>";
        }

        // Snapshot-Dateien sollen stabil diffbar bleiben.
        // Steuerzeichen und Anführungszeichen werden deshalb explizit maskiert.
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"")
                + "\"";
    }

    /**
     * Kompakte Datenstruktur für eine Frage im Live-Datensatz.
     */
    static final class QuestionData
    {
        final long questionId;
        final long questionSetId;
        final String questionType;
        final String startText;
        final String endText;
        final String imageUrl;
        final boolean allowsMultiple;
        final int points;

        final List<Long> themeIds = new ArrayList<>();
        final List<AnswerData> answers = new ArrayList<>();
        final List<GapFieldData> gapFields = new ArrayList<>();
        final Map<Long, GapFieldData> gapFieldsById = new LinkedHashMap<>();

        QuestionData(long questionId, long questionSetId, String questionType, String startText, String endText,
                     String imageUrl, boolean allowsMultiple, int points)
        {
            this.questionId = questionId;
            this.questionSetId = questionSetId;
            this.questionType = questionType;
            this.startText = startText;
            this.endText = endText;
            this.imageUrl = imageUrl;
            this.allowsMultiple = allowsMultiple;
            this.points = points;
        }
    }

    /**
     * Antwortoption einer MC- oder TF-Frage.
     */
    static final class AnswerData
    {
        final long answerId;
        final int optionOrder;
        final String optionText;
        final boolean correct;

        AnswerData(long answerId, int optionOrder, String optionText, boolean correct)
        {
            this.answerId = answerId;
            this.optionOrder = optionOrder;
            this.optionText = optionText;
            this.correct = correct;
        }
    }

    /**
     * Lückenfeld einer GAP-Frage mit zugehörigen Auswahloptionen.
     */
    static final class GapFieldData
    {
        final long gapId;
        final int gapIndex;
        final String textBefore;
        final String textAfter;
        final List<GapOptionData> options = new ArrayList<>();

        GapFieldData(long gapId, int gapIndex, String textBefore, String textAfter)
        {
            this.gapId = gapId;
            this.gapIndex = gapIndex;
            this.textBefore = textBefore;
            this.textAfter = textAfter;
        }
    }

    /**
     * Einzelne Auswahloption innerhalb eines Lückenfelds.
     */
    static final class GapOptionData
    {
        final long gapOptionId;
        final int optionOrder;
        final String optionText;
        final boolean correct;

        GapOptionData(long gapOptionId, int optionOrder, String optionText, boolean correct)
        {
            this.gapOptionId = gapOptionId;
            this.optionOrder = optionOrder;
            this.optionText = optionText;
            this.correct = correct;
        }
    }
}
