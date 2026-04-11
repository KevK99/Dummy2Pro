package me.daskabel.dummy2pro.integration;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapAnswerEntry;
import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader;
import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader.AnswerRecord;
import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader.GapFieldRecord;
import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader.GapOptionRecord;
import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader.QuestionDataset;
import me.daskabel.dummy2pro.integration.support.QuestionSqlDatasetLoader.QuestionRecord;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.service.RoomService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionSqlDatasetEvaluationTest {

    @Test
    void allQuestionsFromDataSqlShouldStayStructurallyValidAndCorrectlyEvaluatable() throws IOException {
        QuestionDataset dataset = QuestionSqlDatasetLoader.loadFromProject();
        List<String> errors = new ArrayList<>();

        for (QuestionRecord questionRecord : dataset.questions()) {
            validateQuestion(questionRecord, errors);
        }

        assertTrue(errors.isEmpty(), "\nFehler im versionierten Fragenbestand:\n - " + String.join("\n - ", errors));
    }

    private void validateQuestion(QuestionRecord questionRecord, List<String> errors) {
        Question question = questionRecord.toDomainQuestion();

        if (questionRecord.startText() == null && questionRecord.endText() == null) {
            errors.add("Frage " + questionRecord.questionId() + " hat weder start_text noch end_text.");
        }

        if (questionRecord.points() <= 0) {
            errors.add("Frage " + questionRecord.questionId() + " hat keine sinnvolle Punktzahl.");
        }

        if (questionRecord.questionType() == QuestionType.GAP) {
            validateGapQuestion(questionRecord, question, errors);
            return;
        }

        validateMcTfQuestion(questionRecord, question, errors);
    }

    private void validateMcTfQuestion(QuestionRecord questionRecord, Question question, List<String> errors) {
        List<AnswerRecord> answers = questionRecord.answers();
        if (answers.isEmpty()) {
            errors.add("Frage " + questionRecord.questionId() + " hat keine Antwortoptionen.");
            return;
        }

        checkDuplicateOptionOrders(
                answers.stream().map(AnswerRecord::optionOrder).collect(Collectors.toList()),
                "Frage " + questionRecord.questionId() + " hat doppelte option_order-Werte.",
                errors
        );

        long correctCount = answers.stream().filter(AnswerRecord::correct).count();
        if (correctCount == 0) {
            errors.add("Frage " + questionRecord.questionId() + " hat keine richtige Antwort.");
        }

        if (!questionRecord.allowsMultiple() && correctCount != 1) {
            errors.add("Frage " + questionRecord.questionId()
                    + " erlaubt keine Mehrfachauswahl, hat aber " + correctCount + " richtige Antworten.");
        }

        if (questionRecord.questionType() == QuestionType.TF) {
            if (answers.size() != 2) {
                errors.add("TF-Frage " + questionRecord.questionId() + " hat nicht genau 2 Antwortoptionen.");
            }
            if (correctCount != 1) {
                errors.add("TF-Frage " + questionRecord.questionId() + " hat nicht genau 1 richtige Antwort.");
            }
        }

        List<Long> correctIds = answers.stream()
                .filter(AnswerRecord::correct)
                .map(AnswerRecord::answerId)
                .toList();

        AnswerRequest correctRequest = new AnswerRequest();
        correctRequest.setSelectedAnswerIds(correctIds);
        AnswerResultDto correctResult = RoomService.evaluateMcTf(question, correctRequest);
        if (!correctResult.isCorrect()) {
            errors.add("Frage " + questionRecord.questionId()
                    + " wird trotz Auswahl aller richtigen Antworten nicht als korrekt gewertet.");
        }

        if (questionRecord.allowsMultiple()) {
            for (AnswerRecord wrongAnswer : answers.stream().filter(answer -> !answer.correct()).toList()) {
                Set<Long> selectedIds = new LinkedHashSet<>(correctIds);
                selectedIds.add(wrongAnswer.answerId());
                AnswerRequest wrongRequest = new AnswerRequest();
                wrongRequest.setSelectedAnswerIds(new ArrayList<>(selectedIds));
                AnswerResultDto wrongResult = RoomService.evaluateMcTf(question, wrongRequest);
                if (wrongResult.isCorrect()) {
                    errors.add("Frage " + questionRecord.questionId()
                            + " akzeptiert zusätzlich eine falsche Antwort als korrekt.");
                }
            }
        } else {
            for (AnswerRecord answer : answers) {
                AnswerRequest singleRequest = new AnswerRequest();
                singleRequest.setSelectedAnswerIds(List.of(answer.answerId()));
                AnswerResultDto singleResult = RoomService.evaluateMcTf(question, singleRequest);
                if (answer.correct() != singleResult.isCorrect()) {
                    errors.add("Frage " + questionRecord.questionId()
                            + " wertet Einzelantwort '" + answer.optionText() + "' falsch aus.");
                }
            }
        }
    }

    private void validateGapQuestion(QuestionRecord questionRecord, Question question, List<String> errors) {
        List<GapFieldRecord> gapFields = questionRecord.gapFields();
        if (gapFields.isEmpty()) {
            errors.add("GAP-Frage " + questionRecord.questionId() + " hat keine Lücken.");
            return;
        }

        checkDuplicateOptionOrders(
                gapFields.stream().map(GapFieldRecord::gapIndex).collect(Collectors.toList()),
                "GAP-Frage " + questionRecord.questionId() + " hat doppelte gap_index-Werte.",
                errors
        );

        List<GapAnswerEntry> correctEntries = new ArrayList<>();
        for (GapFieldRecord gapField : gapFields) {
            List<GapOptionRecord> options = gapField.options();
            if (options.isEmpty()) {
                errors.add("GAP-Frage " + questionRecord.questionId() + ", Lücke " + gapField.gapIndex() + " hat keine Optionen.");
                continue;
            }

            checkDuplicateOptionOrders(
                    options.stream().map(GapOptionRecord::optionOrder).collect(Collectors.toList()),
                    "GAP-Frage " + questionRecord.questionId() + ", Lücke " + gapField.gapIndex() + " hat doppelte option_order-Werte.",
                    errors
            );

            List<GapOptionRecord> correctOptions = options.stream().filter(GapOptionRecord::correct).toList();
            if (correctOptions.size() != 1) {
                errors.add("GAP-Frage " + questionRecord.questionId() + ", Lücke " + gapField.gapIndex()
                        + " hat " + correctOptions.size() + " richtige Antworten statt genau 1.");
                continue;
            }

            GapAnswerEntry entry = new GapAnswerEntry();
            entry.setGapId(gapField.gapId());
            entry.setSelectedGapOptionId(correctOptions.get(0).gapOptionId());
            correctEntries.add(entry);
        }

        AnswerRequest correctRequest = new AnswerRequest();
        correctRequest.setGapAnswers(correctEntries);
        AnswerResultDto correctResult = RoomService.evaluateGap(question, correctRequest);
        if (!correctResult.isCorrect()) {
            errors.add("GAP-Frage " + questionRecord.questionId()
                    + " wird trotz korrekter Gap-Antworten nicht als korrekt gewertet.");
        }

        for (GapFieldRecord gapField : gapFields) {
            GapOptionRecord correctOption = gapField.options().stream().filter(GapOptionRecord::correct).findFirst().orElse(null);
            if (correctOption == null) {
                continue;
            }

            for (GapOptionRecord wrongOption : gapField.options().stream().filter(option -> !option.correct()).toList()) {
                List<GapAnswerEntry> manipulatedEntries = new ArrayList<>();
                for (GapFieldRecord currentGap : gapFields) {
                    GapAnswerEntry entry = new GapAnswerEntry();
                    entry.setGapId(currentGap.gapId());
                    if (currentGap.gapId() == gapField.gapId()) {
                        entry.setSelectedGapOptionId(wrongOption.gapOptionId());
                    } else {
                        long selectedId = currentGap.options().stream().filter(GapOptionRecord::correct)
                                .findFirst()
                                .map(GapOptionRecord::gapOptionId)
                                .orElse(-1L);
                        entry.setSelectedGapOptionId(selectedId);
                    }
                    manipulatedEntries.add(entry);
                }

                AnswerRequest wrongRequest = new AnswerRequest();
                wrongRequest.setGapAnswers(manipulatedEntries);
                AnswerResultDto wrongResult = RoomService.evaluateGap(question, wrongRequest);
                if (wrongResult.isCorrect()) {
                    errors.add("GAP-Frage " + questionRecord.questionId()
                            + " akzeptiert in Lücke " + gapField.gapIndex() + " die falsche Option '"
                            + wrongOption.optionText() + "' als korrekt.");
                }
            }
        }
    }

    private void checkDuplicateOptionOrders(List<Integer> values, String message, List<String> errors) {
        Set<Integer> uniques = new HashSet<>(values);
        if (uniques.size() != values.size()) {
            errors.add(message);
        }
    }
}
