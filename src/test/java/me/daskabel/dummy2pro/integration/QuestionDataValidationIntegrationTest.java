package me.daskabel.dummy2pro.integration;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapAnswerEntry;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prüft den vorhandenen Fragenbestand auf technische Konsistenz und darauf,
 * ob sich die Fragen mit der aktuellen Auswertungslogik tatsächlich korrekt
 * verarbeiten lassen.
 *
 * Der Test geht alle Fragen durch und unterscheidet zwischen MC/TF und GAP.
 * Dabei wird nicht nur auf formale Vollständigkeit geprüft, sondern auch,
 * ob eine mit den als richtig markierten Antworten gebaute Anfrage von der
 * Fachlogik wirklich als korrekt bewertet wird.
 */
@SpringBootTest
@ActiveProfiles("test")
class QuestionDataValidationIntegrationTest
{
    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void allQuestionsShouldBeTechnicallyValidAndEvaluatable()
    {
        List<String> errors = new ArrayList<>();
        List<Question> allQuestions = questionRepository.findAll();

        for (Question baseQuestion : allQuestions)
        {
            Long questionId = baseQuestion.getQuestionId();

            if (baseQuestion.getQuestionType() == QuestionType.GAP)
            {
                validateGapQuestion(questionId, errors);
            }
            else
            {
                validateMcTfQuestion(questionId, errors);
            }
        }

        assertTrue(errors.isEmpty(), "\nFehlerhafte Fragen gefunden:\n - " + String.join("\n - ", errors));
    }

    private void validateMcTfQuestion(Long questionId, List<String> errors)
    {
        Question question = questionRepository.findByQuestionIdWithAnswers(questionId).orElseThrow();

        if (question.getAnswerOptions() == null || question.getAnswerOptions().isEmpty())
        {
            errors.add("Frage " + questionId + " hat keine Antwortoptionen.");
            return;
        }

        long correctCount = question.getAnswerOptions().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                .count();

        if (correctCount == 0)
        {
            errors.add("Frage " + questionId + " hat keine richtige Antwort markiert.");
        }

        if (!question.getAllowsMultiple() && correctCount != 1)
        {
            errors.add("Frage " + questionId + " erlaubt keine Mehrfachauswahl, hat aber " + correctCount + " richtige Antworten.");
        }

        if (question.getQuestionType() == QuestionType.TF)
        {
            if (question.getAnswerOptions().size() != 2)
            {
                errors.add("TF-Frage " + questionId + " hat nicht genau 2 Antwortoptionen.");
            }

            if (correctCount != 1)
            {
                errors.add("TF-Frage " + questionId + " hat nicht genau 1 richtige Antwort.");
            }
        }

        AnswerRequest request = new AnswerRequest();
        request.setSelectedAnswerIds(
                question.getAnswerOptions().stream()
                        .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                        .map(AnswerOption::getAnswerId)
                        .toList()
        );

        AnswerResultDto result = RoomService.evaluateMcTf(question, request);

        if (!result.isCorrect())
        {
            errors.add("Frage " + questionId + " wird trotz Auswahl aller als korrekt markierten Antworten nicht als korrekt ausgewertet.");
        }
    }

    private void validateGapQuestion(Long questionId, List<String> errors)
    {
        Question question = questionRepository.findByQuestionIdWithGaps(questionId).orElseThrow();

        if (question.getGapFields() == null || question.getGapFields().isEmpty())
        {
            errors.add("GAP-Frage " + questionId + " hat keine Lücken.");
            return;
        }

        List<GapAnswerEntry> answers = new ArrayList<>();

        for (GapField gapField : question.getGapFields().stream()
                .sorted(Comparator.comparingInt(GapField::getGapIndex))
                .toList())
        {
            if (gapField.getGapOptions() == null || gapField.getGapOptions().isEmpty())
            {
                errors.add("GAP-Frage " + questionId + ", Gap " + gapField.getGapId() + " hat keine Optionen.");
                continue;
            }

            List<GapOption> correctOptions = gapField.getGapOptions().stream()
                    .filter(GapOption::getIsCorrect)
                    .toList();

            if (correctOptions.size() != 1)
            {
                errors.add("GAP-Frage " + questionId + ", Gap " + gapField.getGapId()
                        + " hat " + correctOptions.size() + " richtige Optionen statt genau 1.");
                continue;
            }

            GapAnswerEntry entry = new GapAnswerEntry();
            entry.setGapId(gapField.getGapId());
            entry.setSelectedGapOptionId(correctOptions.get(0).getGapOptionId());
            answers.add(entry);
        }

        AnswerRequest request = new AnswerRequest();
        request.setGapAnswers(answers);

        AnswerResultDto result = RoomService.evaluateGap(question, request);

        if (!result.isCorrect())
        {
            errors.add("GAP-Frage " + questionId + " wird trotz Auswahl aller als korrekt markierten Gap-Optionen nicht als korrekt ausgewertet.");
        }
    }
}
