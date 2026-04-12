package me.daskabel.dummy2pro.integration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionLiveSemanticTest
{
    @Test
    void liveDatabaseQuestionsMustBeSemanticallyConsistent() throws Exception
    {
        List<LiveQuestionDatasetSupport.QuestionData> questions = LiveQuestionDatasetSupport.loadQuestions();
        List<String> errors = new ArrayList<>();

        for (LiveQuestionDatasetSupport.QuestionData question : questions)
        {
            validateQuestion(question, errors);
        }

        assertTrue(
                errors.isEmpty(),
                "Fehler im Live-Fragenbestand:" + System.lineSeparator()
                        + String.join(System.lineSeparator(), errors)
        );
    }

    private void validateQuestion(LiveQuestionDatasetSupport.QuestionData question, List<String> errors)
    {
        if (question.points <= 0)
        {
            errors.add(prefix(question) + "points muss > 0 sein.");
        }

        if (question.themeIds.isEmpty())
        {
            errors.add(prefix(question) + "hat kein Theme.");
        }

        boolean hasQuestionText =
                LiveQuestionDatasetSupport.hasText(question.startText)
                        || LiveQuestionDatasetSupport.hasText(question.endText)
                        || question.gapFields.stream().anyMatch(gap ->
                        LiveQuestionDatasetSupport.hasText(gap.textBefore)
                                || LiveQuestionDatasetSupport.hasText(gap.textAfter));

        if (!hasQuestionText)
        {
            errors.add(prefix(question) + "hat keinen sichtbaren Fragetext.");
        }

        String type = question.questionType == null ? "" : question.questionType.trim().toUpperCase();

        switch (type)
        {
            case "MC" -> validateMc(question, errors);
            case "TF" -> validateTf(question, errors);
            case "GAP" -> validateGap(question, errors);
            default -> errors.add(prefix(question) + "hat unbekannten question_type: " + question.questionType);
        }
    }

    private void validateMc(LiveQuestionDatasetSupport.QuestionData question, List<String> errors)
    {
        if (!question.gapFields.isEmpty())
        {
            errors.add(prefix(question) + "MC-Frage darf keine Gap-Felder haben.");
        }

        validateAnswerOptions(question, errors);

        long correctCount = question.answers.stream().filter(answer -> answer.correct).count();

        if (correctCount == 0)
        {
            errors.add(prefix(question) + "MC-Frage hat keine richtige Antwort.");
        }
    }

    private void validateTf(LiveQuestionDatasetSupport.QuestionData question, List<String> errors)
    {
        if (!question.gapFields.isEmpty())
        {
            errors.add(prefix(question) + "TF-Frage darf keine Gap-Felder haben.");
        }

        validateAnswerOptions(question, errors);

        if (question.answers.size() != 2)
        {
            errors.add(prefix(question) + "TF-Frage muss genau 2 Antwortoptionen haben.");
        }

        long correctCount = question.answers.stream().filter(answer -> answer.correct).count();
        if (correctCount != 1)
        {
            errors.add(prefix(question) + "TF-Frage muss genau 1 richtige Antwort haben.");
        }

        if (question.allowsMultiple)
        {
            errors.add(prefix(question) + "TF-Frage sollte allows_multiple=false haben.");
        }
    }

    private void validateGap(LiveQuestionDatasetSupport.QuestionData question, List<String> errors)
    {
        if (!question.answers.isEmpty())
        {
            errors.add(prefix(question) + "GAP-Frage darf keine normalen answer_option-Einträge haben.");
        }

        if (question.gapFields.isEmpty())
        {
            errors.add(prefix(question) + "GAP-Frage hat keine gap_field-Einträge.");
            return;
        }

        Set<Integer> seenGapIndexes = new HashSet<>();

        for (LiveQuestionDatasetSupport.GapFieldData gapField : question.gapFields)
        {
            if (!seenGapIndexes.add(gapField.gapIndex))
            {
                errors.add(prefix(question) + "Gap-Index doppelt vorhanden: " + gapField.gapIndex);
            }

            if (gapField.options.size() < 2)
            {
                errors.add(prefix(question) + "Gap " + gapField.gapId + " hat weniger als 2 Optionen.");
            }

            long correctCount = gapField.options.stream().filter(option -> option.correct).count();
            if (correctCount != 1)
            {
                errors.add(prefix(question) + "Gap " + gapField.gapId + " muss genau 1 richtige Option haben.");
            }

            Set<Integer> seenOrders = new HashSet<>();
            for (LiveQuestionDatasetSupport.GapOptionData option : gapField.options)
            {
                if (!LiveQuestionDatasetSupport.hasText(option.optionText))
                {
                    errors.add(prefix(question) + "Gap-Option " + option.gapOptionId + " hat leeren Text.");
                }

                if (!seenOrders.add(option.optionOrder))
                {
                    errors.add(prefix(question) + "Gap " + gapField.gapId + " hat doppelte option_order: " + option.optionOrder);
                }
            }
        }
    }

    private void validateAnswerOptions(LiveQuestionDatasetSupport.QuestionData question, List<String> errors)
    {
        if (question.answers.size() < 2)
        {
            errors.add(prefix(question) + "hat weniger als 2 Antwortoptionen.");
        }

        Set<Integer> seenOrders = new HashSet<>();

        for (LiveQuestionDatasetSupport.AnswerData answer : question.answers)
        {
            if (!LiveQuestionDatasetSupport.hasText(answer.optionText))
            {
                errors.add(prefix(question) + "Antwort " + answer.answerId + " hat leeren Text.");
            }

            if (!seenOrders.add(answer.optionOrder))
            {
                errors.add(prefix(question) + "hat doppelte option_order bei answer_option: " + answer.optionOrder);
            }
        }
    }

    private String prefix(LiveQuestionDatasetSupport.QuestionData question)
    {
        return "Frage " + question.questionId + " (" + question.questionType + "): ";
    }
}