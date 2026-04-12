package me.daskabel.dummy2pro.service;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapAnswerEntry;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoomServiceEvaluationUnitTest
{
    @Test
    void evaluateMcTf_exactCorrectSelection_returnsCorrectAndPoints()
    {
        Question question = buildMcQuestion();
        AnswerRequest request = new AnswerRequest();
        request.setSelectedAnswerIds(List.of(101L));

        AnswerResultDto result = RoomService.evaluateMcTf(question, request);

        assertTrue(result.isCorrect());
        assertEquals(5, result.getPointsEarned());
        assertEquals(List.of(101L), result.getCorrectAnswerIds());
    }

    @Test
    void evaluateMcTf_extraWrongSelection_returnsCorrectAndPoints_whenAtLeastOneCorrectIsSelected()
    {
        Question question = buildMcQuestion();
        AnswerRequest request = new AnswerRequest();
        request.setSelectedAnswerIds(List.of(101L, 102L));

        AnswerResultDto result = RoomService.evaluateMcTf(question, request);

        assertTrue(result.isCorrect());
        assertEquals(5, result.getPointsEarned());
    }

    @Test
    void evaluateMcTf_duplicateSelectedIds_areHandledViaSet()
    {
        Question question = buildMcQuestion();
        AnswerRequest request = new AnswerRequest();
        request.setSelectedAnswerIds(List.of(101L, 101L));

        AnswerResultDto result = RoomService.evaluateMcTf(question, request);

        assertTrue(result.isCorrect());
        assertEquals(5, result.getPointsEarned());
    }

    @Test
    void evaluateGap_allCorrect_returnsCorrectAndGapResults()
    {
        Question question = buildGapQuestion();

        GapAnswerEntry gap1 = new GapAnswerEntry();
        gap1.setGapId(201L);
        gap1.setSelectedGapOptionId(301L);

        GapAnswerEntry gap2 = new GapAnswerEntry();
        gap2.setGapId(202L);
        gap2.setSelectedGapOptionId(303L);

        AnswerRequest request = new AnswerRequest();
        request.setGapAnswers(List.of(gap1, gap2));

        AnswerResultDto result = RoomService.evaluateGap(question, request);

        assertTrue(result.isCorrect());
        assertEquals(6, result.getPointsEarned());
        assertEquals(2, result.getGapResults().size());
        assertTrue(result.getGapResults().stream().allMatch(r -> r.isCorrect()));
    }

    @Test
    void evaluateGap_missingAnswer_returnsWrongAndZeroPoints()
    {
        Question question = buildGapQuestion();

        GapAnswerEntry onlyFirstGap = new GapAnswerEntry();
        onlyFirstGap.setGapId(201L);
        onlyFirstGap.setSelectedGapOptionId(301L);

        AnswerRequest request = new AnswerRequest();
        request.setGapAnswers(List.of(onlyFirstGap));

        AnswerResultDto result = RoomService.evaluateGap(question, request);

        assertFalse(result.isCorrect());
        assertEquals(0, result.getPointsEarned());
        assertEquals(2, result.getGapResults().size());
        assertTrue(result.getGapResults().stream().anyMatch(r -> !r.isCorrect()));
    }

    private Question buildMcQuestion()
    {
        Question question = new Question();
        question.setQuestionId(1L);
        question.setQuestionType(QuestionType.MC);
        question.setPoints(5);

        AnswerOption correct = new AnswerOption();
        correct.setAnswerId(101L);
        correct.setOptionText("richtig");
        correct.setIsCorrect(true);
        correct.setOptionOrder(1);

        AnswerOption wrong = new AnswerOption();
        wrong.setAnswerId(102L);
        wrong.setOptionText("falsch");
        wrong.setIsCorrect(false);
        wrong.setOptionOrder(2);

        question.setAnswerOptions(List.of(correct, wrong));
        return question;
    }

    private Question buildGapQuestion()
    {
        Question question = new Question();
        question.setQuestionId(2L);
        question.setQuestionType(QuestionType.GAP);
        question.setPoints(6);

        GapField gap1 = new GapField();
        gap1.setGapId(201L);
        gap1.setGapIndex(0);

        GapOption gap1Correct = new GapOption();
        gap1Correct.setGapOptionId(301L);
        gap1Correct.setOptionText("A");
        gap1Correct.setIsCorrect(true);
        gap1Correct.setOptionOrder(1);

        GapOption gap1Wrong = new GapOption();
        gap1Wrong.setGapOptionId(302L);
        gap1Wrong.setOptionText("B");
        gap1Wrong.setIsCorrect(false);
        gap1Wrong.setOptionOrder(2);

        gap1.setGapOptions(Set.of(gap1Correct, gap1Wrong));

        GapField gap2 = new GapField();
        gap2.setGapId(202L);
        gap2.setGapIndex(1);

        GapOption gap2Correct = new GapOption();
        gap2Correct.setGapOptionId(303L);
        gap2Correct.setOptionText("C");
        gap2Correct.setIsCorrect(true);
        gap2Correct.setOptionOrder(1);

        GapOption gap2Wrong = new GapOption();
        gap2Wrong.setGapOptionId(304L);
        gap2Wrong.setOptionText("D");
        gap2Wrong.setIsCorrect(false);
        gap2Wrong.setOptionOrder(2);

        gap2.setGapOptions(Set.of(gap2Correct, gap2Wrong));

        question.setGapFields(Set.of(gap1, gap2));
        return question;
    }

    @Test
    void evaluateMcTf_multipleCorrectAnswers_isCorrectWhenAtLeastOneRightAnswerWasSelected()
    {
        Question question = new Question();
        question.setQuestionId(3L);
        question.setQuestionType(QuestionType.MC);
        question.setAllowsMultiple(true);
        question.setPoints(7);

        AnswerOption correctOne = new AnswerOption();
        correctOne.setAnswerId(201L);
        correctOne.setOptionText("richtig 1");
        correctOne.setIsCorrect(true);
        correctOne.setOptionOrder(1);

        AnswerOption correctTwo = new AnswerOption();
        correctTwo.setAnswerId(202L);
        correctTwo.setOptionText("richtig 2");
        correctTwo.setIsCorrect(true);
        correctTwo.setOptionOrder(2);

        AnswerOption wrong = new AnswerOption();
        wrong.setAnswerId(203L);
        wrong.setOptionText("falsch");
        wrong.setIsCorrect(false);
        wrong.setOptionOrder(3);

        question.setAnswerOptions(List.of(correctOne, correctTwo, wrong));

        AnswerRequest onlyOneCorrect = new AnswerRequest();
        onlyOneCorrect.setSelectedAnswerIds(List.of(201L));

        AnswerResultDto oneCorrectResult = RoomService.evaluateMcTf(question, onlyOneCorrect);

        assertTrue(oneCorrectResult.isCorrect());
        assertEquals(7, oneCorrectResult.getPointsEarned());

        AnswerRequest oneCorrectAndOneWrong = new AnswerRequest();
        oneCorrectAndOneWrong.setSelectedAnswerIds(List.of(201L, 203L));

        AnswerResultDto mixedResult = RoomService.evaluateMcTf(question, oneCorrectAndOneWrong);

        assertTrue(mixedResult.isCorrect());
        assertEquals(7, mixedResult.getPointsEarned());

        AnswerRequest onlyWrong = new AnswerRequest();
        onlyWrong.setSelectedAnswerIds(List.of(203L));

        AnswerResultDto wrongResult = RoomService.evaluateMcTf(question, onlyWrong);

        assertFalse(wrongResult.isCorrect());
        assertEquals(0, wrongResult.getPointsEarned());
    }

    @Test
    void evaluateMcTf_onlyWrongSelections_returnsWrongAndZeroPoints()
    {
        Question question = new Question();
        question.setQuestionId(4L);
        question.setQuestionType(QuestionType.MC);
        question.setAllowsMultiple(true);
        question.setPoints(7);

        AnswerOption correctOne = new AnswerOption();
        correctOne.setAnswerId(301L);
        correctOne.setOptionText("richtig");
        correctOne.setIsCorrect(true);
        correctOne.setOptionOrder(1);

        AnswerOption wrongOne = new AnswerOption();
        wrongOne.setAnswerId(302L);
        wrongOne.setOptionText("falsch 1");
        wrongOne.setIsCorrect(false);
        wrongOne.setOptionOrder(2);

        AnswerOption wrongTwo = new AnswerOption();
        wrongTwo.setAnswerId(303L);
        wrongTwo.setOptionText("falsch 2");
        wrongTwo.setIsCorrect(false);
        wrongTwo.setOptionOrder(3);

        question.setAnswerOptions(List.of(correctOne, wrongOne, wrongTwo));

        AnswerRequest onlyWrong = new AnswerRequest();
        onlyWrong.setSelectedAnswerIds(List.of(302L, 303L));

        AnswerResultDto result = RoomService.evaluateMcTf(question, onlyWrong);

        assertFalse(result.isCorrect());
        assertEquals(0, result.getPointsEarned());
    }
}