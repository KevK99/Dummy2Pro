package me.daskabel.dummy2pro.integration.support;

import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Liest den Fragenbestand direkt aus data.sql.
 *
 * So bleibt der Test unabhängig von einer laufenden MySQL-Datenbank und
 * prüft trotzdem genau den versionierten Datenbestand im Projekt.
 */
public final class QuestionSqlDatasetLoader {

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "(?is)^INSERT\\s+INTO\\s+`?([a-zA-Z_]+)`?\\s*\\((.*?)\\)\\s*(VALUES|SELECT)\\s+(.*)$"
    );

    private static final Pattern SET_LAST_INSERT_ID_PATTERN = Pattern.compile(
            "(?is)^SET\\s+@([a-zA-Z0-9_]+)\\s*:=\\s*LAST_INSERT_ID\\s*\\(\\)\\s*$"
    );

    private static final Pattern GAP_OPTION_SELECT_PATTERN = Pattern.compile(
            "(?is)^gap_id\\s*,\\s*('(?:''|[^'])*'|[^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^\\s]+)\\s+FROM\\s+gap_field\\s+WHERE\\s+question_id\\s*=\\s*([^\\s]+)\\s+AND\\s+gap_index\\s*=\\s*([^\\s]+)\\s*$"
    );

    private QuestionSqlDatasetLoader() {
    }

    public static QuestionDataset loadFromProject() throws IOException {
        String configuredPath = System.getProperty("question.sql.path", "src/main/resources/data.sql");
        return load(Path.of(configuredPath));
    }

    public static QuestionDataset load(Path sqlFile) throws IOException {
        String rawSql = Files.readString(sqlFile, StandardCharsets.UTF_8);
        return parse(rawSql);
    }

    public static QuestionDataset parse(String rawSql) {
        ParserState state = new ParserState();
        List<String> statements = splitStatements(removeCommentLines(rawSql));

        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.equalsIgnoreCase("START TRANSACTION")
                    || trimmed.equalsIgnoreCase("COMMIT")
                    || trimmed.startsWith("SET FOREIGN_KEY_CHECKS")
                    || trimmed.toUpperCase(Locale.ROOT).startsWith("TRUNCATE TABLE")
                    || trimmed.toUpperCase(Locale.ROOT).startsWith("DROP TABLE")) {
                continue;
            }

            Matcher setMatcher = SET_LAST_INSERT_ID_PATTERN.matcher(trimmed);
            if (setMatcher.matches()) {
                state.variables.put(setMatcher.group(1), state.lastInsertId);
                continue;
            }

            Matcher insertMatcher = INSERT_PATTERN.matcher(trimmed);
            if (!insertMatcher.matches()) {
                continue;
            }

            String table = insertMatcher.group(1).toLowerCase(Locale.ROOT);
            List<String> columns = splitCsv(insertMatcher.group(2));
            String mode = insertMatcher.group(3).toUpperCase(Locale.ROOT);
            String payload = insertMatcher.group(4).trim();

            switch (table) {
                case "question_set" -> parseQuestionSet(columns, mode, payload, state);
                case "question" -> parseQuestion(columns, mode, payload, state);
                case "question_theme" -> parseQuestionTheme(columns, mode, payload, state);
                case "answer_option" -> parseAnswerOption(columns, mode, payload, state);
                case "gap_field" -> parseGapField(columns, mode, payload, state);
                case "gap_option" -> parseGapOption(columns, mode, payload, state);
                default -> {
                    // Für team/theme/users/... hier nicht relevant.
                }
            }
        }

        return new QuestionDataset(new ArrayList<>(state.questions.values()));
    }

    private static void parseQuestionSet(List<String> columns, String mode, String payload, ParserState state) {
        if (!"VALUES".equals(mode)) {
            return;
        }

        for (List<String> tuple : parseValuesTuples(payload)) {
            Map<String, String> row = mapColumns(columns, tuple);
            long questionSetId = readExplicitOrGeneratedId(row.get("question_set_id"), ++state.questionSetIdCounter);
            state.questionSetIdCounter = Math.max(state.questionSetIdCounter, questionSetId);
            state.lastInsertId = questionSetId;
        }
    }

    private static void parseQuestion(List<String> columns, String mode, String payload, ParserState state) {
        if (!"VALUES".equals(mode)) {
            return;
        }

        for (List<String> tuple : parseValuesTuples(payload)) {
            Map<String, String> row = mapColumns(columns, tuple);

            long questionId = readExplicitOrGeneratedId(row.get("question_id"), ++state.questionIdCounter);
            state.questionIdCounter = Math.max(state.questionIdCounter, questionId);
            state.lastInsertId = questionId;

            QuestionRecord record = new QuestionRecord();
            record.questionId = questionId;
            record.questionSetId = asLong(resolveValue(row.get("question_set_id"), state));
            record.questionType = QuestionType.valueOf(asString(resolveValue(row.get("question_type"), state)));
            record.startText = nullableString(resolveValue(row.get("start_text"), state));
            record.imageUrl = nullableString(resolveValue(row.get("image_url"), state));
            record.endText = nullableString(resolveValue(row.get("end_text"), state));
            record.allowsMultiple = asBoolean(resolveValue(row.get("allows_multiple"), state));
            record.points = asInt(resolveValue(row.get("points"), state));
            state.questions.put(questionId, record);
        }
    }

    private static void parseQuestionTheme(List<String> columns, String mode, String payload, ParserState state) {
        if (!"VALUES".equals(mode)) {
            return;
        }

        for (List<String> tuple : parseValuesTuples(payload)) {
            Map<String, String> row = mapColumns(columns, tuple);
            long questionId = asLong(resolveValue(row.get("question_id"), state));
            long themeId = asLong(resolveValue(row.get("theme_id"), state));
            state.getQuestion(questionId).themeIds.add(themeId);
        }
    }

    private static void parseAnswerOption(List<String> columns, String mode, String payload, ParserState state) {
        if (!"VALUES".equals(mode)) {
            return;
        }

        for (List<String> tuple : parseValuesTuples(payload)) {
            Map<String, String> row = mapColumns(columns, tuple);

            long answerId = readExplicitOrGeneratedId(row.get("answer_id"), ++state.answerIdCounter);
            state.answerIdCounter = Math.max(state.answerIdCounter, answerId);
            state.lastInsertId = answerId;

            long questionId = asLong(resolveValue(row.get("question_id"), state));
            AnswerRecord answerRecord = new AnswerRecord();
            answerRecord.answerId = answerId;
            answerRecord.questionId = questionId;
            answerRecord.optionText = nullableString(resolveValue(row.get("option_text"), state));
            answerRecord.correct = asBoolean(resolveValue(row.get("is_correct"), state));
            answerRecord.optionOrder = asInt(resolveValue(row.get("option_order"), state));
            state.getQuestion(questionId).answers.add(answerRecord);
        }
    }

    private static void parseGapField(List<String> columns, String mode, String payload, ParserState state) {
        if (!"VALUES".equals(mode)) {
            return;
        }

        for (List<String> tuple : parseValuesTuples(payload)) {
            Map<String, String> row = mapColumns(columns, tuple);

            long gapId = readExplicitOrGeneratedId(row.get("gap_id"), ++state.gapIdCounter);
            state.gapIdCounter = Math.max(state.gapIdCounter, gapId);
            state.lastInsertId = gapId;

            long questionId = asLong(resolveValue(row.get("question_id"), state));
            GapFieldRecord gapFieldRecord = new GapFieldRecord();
            gapFieldRecord.gapId = gapId;
            gapFieldRecord.questionId = questionId;
            gapFieldRecord.gapIndex = asInt(resolveValue(row.get("gap_index"), state));
            gapFieldRecord.textBefore = nullableString(resolveValue(row.get("text_before"), state));
            gapFieldRecord.textAfter = nullableString(resolveValue(row.get("text_after"), state));
            state.gapFieldsById.put(gapId, gapFieldRecord);
            state.gapFieldsByQuestionAndIndex.put(questionId + "#" + gapFieldRecord.gapIndex, gapFieldRecord);
            state.getQuestion(questionId).gapFields.add(gapFieldRecord);
        }
    }

    private static void parseGapOption(List<String> columns, String mode, String payload, ParserState state) {
        if ("VALUES".equals(mode)) {
            for (List<String> tuple : parseValuesTuples(payload)) {
                Map<String, String> row = mapColumns(columns, tuple);
                long gapId = asLong(resolveValue(row.get("gap_id"), state));
                createGapOption(state, gapId,
                        nullableString(resolveValue(row.get("option_text"), state)),
                        asBoolean(resolveValue(row.get("is_correct"), state)),
                        asInt(resolveValue(row.get("option_order"), state)),
                        row.get("gap_option_id"));
            }
            return;
        }

        if (!"SELECT".equals(mode)) {
            return;
        }

        Matcher matcher = GAP_OPTION_SELECT_PATTERN.matcher(payload);
        if (!matcher.matches()) {
            throw new IllegalStateException("Gap-Option-SELECT konnte nicht gelesen werden: " + payload);
        }

        String optionTextToken = matcher.group(1);
        String isCorrectToken = matcher.group(2);
        String optionOrderToken = matcher.group(3);
        String questionIdToken = matcher.group(4);
        String gapIndexToken = matcher.group(5);

        long questionId = asLong(resolveValue(questionIdToken, state));
        int gapIndex = asInt(resolveValue(gapIndexToken, state));
        GapFieldRecord gapFieldRecord = Optional.ofNullable(state.gapFieldsByQuestionAndIndex.get(questionId + "#" + gapIndex))
                .orElseThrow(() -> new IllegalStateException("Gap-Feld nicht gefunden für Frage " + questionId + " / Index " + gapIndex));

        createGapOption(state, gapFieldRecord.gapId,
                nullableString(resolveValue(optionTextToken, state)),
                asBoolean(resolveValue(isCorrectToken, state)),
                asInt(resolveValue(optionOrderToken, state)),
                null);
    }

    private static void createGapOption(ParserState state,
                                        long gapId,
                                        String optionText,
                                        boolean isCorrect,
                                        int optionOrder,
                                        String explicitGapOptionIdToken) {
        long gapOptionId = readExplicitOrGeneratedId(explicitGapOptionIdToken, ++state.gapOptionIdCounter);
        state.gapOptionIdCounter = Math.max(state.gapOptionIdCounter, gapOptionId);
        state.lastInsertId = gapOptionId;

        GapOptionRecord gapOptionRecord = new GapOptionRecord();
        gapOptionRecord.gapOptionId = gapOptionId;
        gapOptionRecord.gapId = gapId;
        gapOptionRecord.optionText = optionText;
        gapOptionRecord.correct = isCorrect;
        gapOptionRecord.optionOrder = optionOrder;

        GapFieldRecord gapFieldRecord = Optional.ofNullable(state.gapFieldsById.get(gapId))
                .orElseThrow(() -> new IllegalStateException("Gap-Feld mit ID " + gapId + " nicht gefunden."));
        gapFieldRecord.options.add(gapOptionRecord);
    }

    private static long readExplicitOrGeneratedId(String token, long generatedId) {
        if (token == null) {
            return generatedId;
        }
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return generatedId;
        }
        if (trimmed.startsWith("@")) {
            throw new IllegalStateException("Explizite ID darf keine Variable sein: " + token);
        }
        return asLong(trimmed);
    }

    private static Object resolveValue(String token, ParserState state) {
        if (token == null) {
            return null;
        }

        String trimmed = token.trim();
        if (trimmed.equalsIgnoreCase("NULL")) {
            return null;
        }

        if (trimmed.startsWith("@")) {
            String variableName = trimmed.substring(1);
            Long value = state.variables.get(variableName);
            if (value == null) {
                throw new IllegalStateException("Unbekannte SQL-Variable: " + trimmed);
            }
            return value;
        }

        if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
            return unescapeSqlString(trimmed.substring(1, trimmed.length() - 1));
        }

        if (trimmed.matches("-?\\d+")) {
            return Long.parseLong(trimmed);
        }

        return trimmed;
    }

    private static String removeCommentLines(String rawSql) {
        return rawSql.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
    }

    private static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);

            if (ch == '\'') {
                if (inString && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    current.append(ch).append(sql.charAt(i + 1));
                    i++;
                    continue;
                }
                inString = !inString;
                current.append(ch);
                continue;
            }

            if (ch == ';' && !inString) {
                statements.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        if (current.length() > 0) {
            statements.add(current.toString());
        }

        return statements;
    }

    private static List<List<String>> parseValuesTuples(String payload) {
        List<List<String>> tuples = new ArrayList<>();
        int i = 0;
        while (i < payload.length()) {
            char ch = payload.charAt(i);
            if (Character.isWhitespace(ch) || ch == ',') {
                i++;
                continue;
            }
            if (ch != '(') {
                throw new IllegalStateException("VALUES-Tupel erwartet, gefunden: " + payload.substring(i));
            }

            int depth = 0;
            boolean inString = false;
            StringBuilder tuple = new StringBuilder();
            for (; i < payload.length(); i++) {
                char c = payload.charAt(i);
                tuple.append(c);
                if (c == '\'') {
                    if (inString && i + 1 < payload.length() && payload.charAt(i + 1) == '\'') {
                        tuple.append(payload.charAt(i + 1));
                        i++;
                        continue;
                    }
                    inString = !inString;
                } else if (!inString) {
                    if (c == '(') {
                        depth++;
                    } else if (c == ')') {
                        depth--;
                        if (depth == 0) {
                            i++;
                            break;
                        }
                    }
                }
            }
            String tupleText = tuple.toString().trim();
            tuples.add(splitCsv(tupleText.substring(1, tupleText.length() - 1)));
        }
        return tuples;
    }

    private static List<String> splitCsv(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        int depth = 0;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == '\'') {
                current.append(ch);
                if (inString && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                    current.append(text.charAt(i + 1));
                    i++;
                    continue;
                }
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                } else if (ch == ',' && depth == 0) {
                    parts.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }

            current.append(ch);
        }

        parts.add(current.toString().trim());
        return parts;
    }

    private static Map<String, String> mapColumns(List<String> columns, List<String> values) {
        if (columns.size() != values.size()) {
            throw new IllegalStateException("Spaltenanzahl und Werteanzahl passen nicht zusammen: " + columns + " vs. " + values);
        }
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            row.put(normalizeIdentifier(columns.get(i)), values.get(i));
        }
        return row;
    }

    private static String normalizeIdentifier(String identifier) {
        return identifier.replace("`", "").trim().toLowerCase(Locale.ROOT);
    }

    private static String unescapeSqlString(String value) {
        return value.replace("''", "'");
    }

    private static String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String asString(Object value) {
        return Objects.toString(value, null);
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static final class ParserState {
        private final Map<String, Long> variables = new HashMap<>();
        private final Map<Long, QuestionRecord> questions = new LinkedHashMap<>();
        private final Map<Long, GapFieldRecord> gapFieldsById = new HashMap<>();
        private final Map<String, GapFieldRecord> gapFieldsByQuestionAndIndex = new HashMap<>();
        private long questionSetIdCounter;
        private long questionIdCounter;
        private long answerIdCounter;
        private long gapIdCounter;
        private long gapOptionIdCounter;
        private long lastInsertId;

        private QuestionRecord getQuestion(long questionId) {
            QuestionRecord questionRecord = questions.get(questionId);
            if (questionRecord == null) {
                throw new IllegalStateException("Frage mit ID " + questionId + " wurde nicht gefunden.");
            }
            return questionRecord;
        }
    }

    public static final class QuestionDataset {
        private final List<QuestionRecord> questions;

        private QuestionDataset(List<QuestionRecord> questions) {
            this.questions = questions.stream()
                    .sorted(Comparator.comparingLong(q -> q.questionId))
                    .toList();
        }

        public List<QuestionRecord> questions() {
            return questions;
        }

        public String toSnapshotText() {
            StringBuilder snapshot = new StringBuilder();
            for (QuestionRecord question : questions) {
                snapshot.append("QUESTION|")
                        .append(question.questionId).append('|')
                        .append(question.questionSetId).append('|')
                        .append(question.questionType).append('|')
                        .append(question.allowsMultiple).append('|')
                        .append(question.points).append('|')
                        .append(escape(question.startText)).append('|')
                        .append(escape(question.imageUrl)).append('|')
                        .append(escape(question.endText)).append('|')
                        .append(question.themeIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(",")))
                        .append('\n');

                for (AnswerRecord answer : question.sortedAnswers()) {
                    snapshot.append("ANSWER|")
                            .append(question.questionId).append('|')
                            .append(answer.answerId).append('|')
                            .append(answer.optionOrder).append('|')
                            .append(answer.correct).append('|')
                            .append(escape(answer.optionText))
                            .append('\n');
                }

                for (GapFieldRecord gapField : question.sortedGapFields()) {
                    snapshot.append("GAP|")
                            .append(question.questionId).append('|')
                            .append(gapField.gapId).append('|')
                            .append(gapField.gapIndex).append('|')
                            .append(escape(gapField.textBefore)).append('|')
                            .append(escape(gapField.textAfter))
                            .append('\n');

                    for (GapOptionRecord gapOption : gapField.sortedOptions()) {
                        snapshot.append("GAP_OPTION|")
                                .append(question.questionId).append('|')
                                .append(gapField.gapIndex).append('|')
                                .append(gapOption.gapOptionId).append('|')
                                .append(gapOption.optionOrder).append('|')
                                .append(gapOption.correct).append('|')
                                .append(escape(gapOption.optionText))
                                .append('\n');
                    }
                }
            }
            return snapshot.toString();
        }

        private static String escape(String value) {
            if (value == null) {
                return "<NULL>";
            }
            return value
                    .replace("\\", "\\\\")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("|", "\\|");
        }
    }

    public static final class QuestionRecord {
        private long questionId;
        private long questionSetId;
        private QuestionType questionType;
        private String startText;
        private String imageUrl;
        private String endText;
        private boolean allowsMultiple;
        private int points;
        private final Set<Long> themeIds = new LinkedHashSet<>();
        private final List<AnswerRecord> answers = new ArrayList<>();
        private final List<GapFieldRecord> gapFields = new ArrayList<>();

        public long questionId() {
            return questionId;
        }

        public QuestionType questionType() {
            return questionType;
        }

        public boolean allowsMultiple() {
            return allowsMultiple;
        }

        public String startText() {
            return startText;
        }

        public String endText() {
            return endText;
        }

        public int points() {
            return points;
        }

        public List<AnswerRecord> answers() {
            return sortedAnswers();
        }

        public List<GapFieldRecord> gapFields() {
            return sortedGapFields();
        }

        public Question toDomainQuestion() {
            Question question = new Question();
            question.setQuestionId(questionId);
            question.setQuestionType(questionType);
            question.setStartText(startText);
            question.setImageUrl(imageUrl);
            question.setEndText(endText);
            question.setAllowsMultiple(allowsMultiple);
            question.setPoints(points);

            if (questionType == QuestionType.GAP) {
                Set<GapField> domainGapFields = new LinkedHashSet<>();
                for (GapFieldRecord gapFieldRecord : sortedGapFields()) {
                    GapField gapField = new GapField();
                    gapField.setGapId(gapFieldRecord.gapId);
                    gapField.setQuestion(question);
                    gapField.setGapIndex(gapFieldRecord.gapIndex);
                    gapField.setTextBefore(gapFieldRecord.textBefore);
                    gapField.setTextAfter(gapFieldRecord.textAfter);

                    Set<GapOption> domainOptions = new LinkedHashSet<>();
                    for (GapOptionRecord gapOptionRecord : gapFieldRecord.sortedOptions()) {
                        GapOption gapOption = new GapOption();
                        gapOption.setGapOptionId(gapOptionRecord.gapOptionId);
                        gapOption.setGapField(gapField);
                        gapOption.setOptionText(gapOptionRecord.optionText);
                        gapOption.setIsCorrect(gapOptionRecord.correct);
                        gapOption.setOptionOrder(gapOptionRecord.optionOrder);
                        domainOptions.add(gapOption);
                    }

                    gapField.setGapOptions(domainOptions);
                    domainGapFields.add(gapField);
                }
                question.setGapFields(domainGapFields);
            } else {
                List<AnswerOption> domainAnswers = new ArrayList<>();
                for (AnswerRecord answerRecord : sortedAnswers()) {
                    AnswerOption answerOption = new AnswerOption();
                    answerOption.setAnswerId(answerRecord.answerId);
                    answerOption.setQuestion(question);
                    answerOption.setOptionText(answerRecord.optionText);
                    answerOption.setIsCorrect(answerRecord.correct);
                    answerOption.setOptionOrder(answerRecord.optionOrder);
                    domainAnswers.add(answerOption);
                }
                question.setAnswerOptions(domainAnswers);
            }

            return question;
        }

        private List<AnswerRecord> sortedAnswers() {
            return answers.stream()
                    .sorted(Comparator.comparingInt(AnswerRecord::optionOrder).thenComparingLong(AnswerRecord::answerId))
                    .toList();
        }

        private List<GapFieldRecord> sortedGapFields() {
            return gapFields.stream()
                    .sorted(Comparator.comparingInt(GapFieldRecord::gapIndex).thenComparingLong(GapFieldRecord::gapId))
                    .toList();
        }
    }

    public static final class AnswerRecord {
        private long answerId;
        private long questionId;
        private String optionText;
        private boolean correct;
        private int optionOrder;

        public long answerId() {
            return answerId;
        }

        public String optionText() {
            return optionText;
        }

        public boolean correct() {
            return correct;
        }

        public int optionOrder() {
            return optionOrder;
        }
    }

    public static final class GapFieldRecord {
        private long gapId;
        private long questionId;
        private int gapIndex;
        private String textBefore;
        private String textAfter;
        private final List<GapOptionRecord> options = new ArrayList<>();

        public long gapId() {
            return gapId;
        }

        public int gapIndex() {
            return gapIndex;
        }

        public String textBefore() {
            return textBefore;
        }

        public String textAfter() {
            return textAfter;
        }

        public List<GapOptionRecord> options() {
            return sortedOptions();
        }

        private List<GapOptionRecord> sortedOptions() {
            return options.stream()
                    .sorted(Comparator.comparingInt(GapOptionRecord::optionOrder).thenComparingLong(GapOptionRecord::gapOptionId))
                    .toList();
        }
    }

    public static final class GapOptionRecord {
        private long gapOptionId;
        private long gapId;
        private String optionText;
        private boolean correct;
        private int optionOrder;

        public long gapOptionId() {
            return gapOptionId;
        }

        public String optionText() {
            return optionText;
        }

        public boolean correct() {
            return correct;
        }

        public int optionOrder() {
            return optionOrder;
        }
    }
}
