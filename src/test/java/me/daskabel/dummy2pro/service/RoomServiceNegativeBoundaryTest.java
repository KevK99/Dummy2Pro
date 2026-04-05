package me.daskabel.dummy2pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.GapAnswerEntry;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import org.junit.jupiter.api.Test;

class RoomServiceNegativeBoundaryTest
{
    @Test
    void evaluateMcTf_shouldTreatDuplicateSelectionsAsSingleSelection()
    {
        Question question = new Question(QuestionType.MC, "Frage", null, null, false, 5);
        question.setAnswerOptions(List.of(
            answer(10L, question, "richtig", true, 1),
            answer(11L, question, "falsch", false, 2)
        ));

        AnswerRequest request = new AnswerRequest();
        request.setSelectedAnswerIds(List.of(10L, 10L));

        var result = RoomService.evaluateMcTf(question, request);

        assertTrue(result.isCorrect());
        assertEquals(5, result.getPointsEarned());
        assertEquals(List.of(10L), result.getCorrectAnswerIds());
    }

    @Test
    void evaluateMcTf_shouldReturnIncorrect_whenNothingIsSelected()
    {
        Question question = new Question(QuestionType.TF, "Frage", null, null, false, 3);
        question.setAnswerOptions(List.of(
            answer(20L, question, "ja", true, 1),
            answer(21L, question, "nein", false, 2)
        ));

        AnswerRequest request = new AnswerRequest();
        request.setSelectedAnswerIds(List.of());

        var result = RoomService.evaluateMcTf(question, request);

        assertFalse(result.isCorrect());
        assertEquals(0, result.getPointsEarned());
        assertEquals(List.of(20L), result.getCorrectAnswerIds());
    }

    @Test
    void evaluateGap_shouldReturnIncorrect_whenOneGapIsMissing()
    {
        Question question = new Question(QuestionType.GAP, "Frage", null, null, false, 7);

        GapField firstGap = gapField(100L, question, 0);
        GapField secondGap = gapField(101L, question, 1);

        firstGap.setGapOptions(new LinkedHashSet<>(List.of(
            gapOption(1000L, firstGap, "eins", true, 1),
            gapOption(1001L, firstGap, "zwei", false, 2)
        )));

        secondGap.setGapOptions(new LinkedHashSet<>(List.of(
            gapOption(1010L, secondGap, "drei", true, 1),
            gapOption(1011L, secondGap, "vier", false, 2)
        )));

        question.setGapFields(new LinkedHashSet<>(List.of(firstGap, secondGap)));

        AnswerRequest request = new AnswerRequest();
        request.setGapAnswers(List.of(gapAnswer(100L, 1000L)));

        var result = RoomService.evaluateGap(question, request);

        assertFalse(result.isCorrect());
        assertEquals(0, result.getPointsEarned());
        assertEquals(2, result.getGapResults().size());
    }

    @Test
    void evaluateGap_shouldIgnoreUnknownGapIds_andStillJudgeKnownGapCorrectly()
    {
        Question question = new Question(QuestionType.GAP, "Frage", null, null, false, 4);
        GapField gap = gapField(200L, question, 0);
        gap.setGapOptions(new LinkedHashSet<>(List.of(
            gapOption(2000L, gap, "korrekt", true, 1),
            gapOption(2001L, gap, "falsch", false, 2)
        )));
        question.setGapFields(new LinkedHashSet<>(List.of(gap)));

        AnswerRequest request = new AnswerRequest();
        request.setGapAnswers(List.of(
            gapAnswer(999L, 9999L),
            gapAnswer(200L, 2000L)
        ));

        var result = RoomService.evaluateGap(question, request);

        assertTrue(result.isCorrect());
        assertEquals(4, result.getPointsEarned());
        assertEquals(1, result.getGapResults().size());
        assertEquals(200L, result.getGapResults().get(0).getGapId());
    }

    private AnswerOption answer(Long id, Question question, String text, boolean correct, int order)
    {
        AnswerOption option = new AnswerOption(question, text, correct, order);
        option.setAnswerId(id);
        return option;
    }

    private GapField gapField(Long id, Question question, int index)
    {
        GapField gapField = new GapField(question, index);
        gapField.setGapId(id);
        return gapField;
    }

    private GapOption gapOption(Long id, GapField gapField, String text, boolean correct, int order)
    {
        GapOption option = new GapOption(gapField, text, correct, order);
        option.setGapOptionId(id);
        return option;
    }

    private GapAnswerEntry gapAnswer(Long gapId, Long selectedGapOptionId)
    {
        GapAnswerEntry entry = new GapAnswerEntry();
        entry.setGapId(gapId);
        entry.setSelectedGapOptionId(selectedGapOptionId);
        return entry;
    }
}
