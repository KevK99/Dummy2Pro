package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RunReviewDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.RunGapAnswer;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QuizSessionManagerReviewHelperTest
{
    private QuizSessionManager manager;

    @BeforeEach
    void setUp()
    {
        manager = new QuizSessionManager(
                Mockito.mock(QuizSessionGenerator.class),
                Mockito.mock(QuestionRepository.class),
                Mockito.mock(GameRunRepository.class),
                Mockito.mock(QuestionProgressRepository.class),
                Mockito.mock(RunSelectedAnswerRepository.class),
                Mockito.mock(RunGapAnswerRepository.class),
                Mockito.mock(UserRepository.class)
        );
    }

    @Test
    void buildChoiceReviews_shouldHandleNullAndSortSelectedFlags() throws Exception
    {
        Method method = QuizSessionManager.class.getDeclaredMethod("buildChoiceReviews", Question.class, Set.class);
        method.setAccessible(true);

        Question withoutAnswers = new Question();
        withoutAnswers.setQuestionType(QuestionType.MC);
        withoutAnswers.setAnswerOptions(null);

        @SuppressWarnings("unchecked")
        List<RunReviewDto.ChoiceReviewDto> emptyResult =
                (List<RunReviewDto.ChoiceReviewDto>) method.invoke(manager, withoutAnswers, Set.of());

        assertEquals(List.of(), emptyResult);

        Question question = new Question(QuestionType.MC, "Start", null, "Ende", false, 5);
        question.setQuestionId(100L);

        AnswerOption option2 = new AnswerOption(question, "B", false, 2);
        option2.setAnswerId(1002L);

        AnswerOption option1 = new AnswerOption(question, "A", true, 1);
        option1.setAnswerId(1001L);

        question.setAnswerOptions(List.of(option2, option1));

        @SuppressWarnings("unchecked")
        List<RunReviewDto.ChoiceReviewDto> result =
                (List<RunReviewDto.ChoiceReviewDto>) method.invoke(manager, question, Set.of(1002L));

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getOptionText());
        assertTrue(result.get(0).isCorrect());
        assertFalse(result.get(0).isSelected());
        assertEquals("B", result.get(1).getOptionText());
        assertFalse(result.get(1).isCorrect());
        assertTrue(result.get(1).isSelected());
    }

    @Test
    void buildGapReviews_shouldCoverCorrectWrongMissingAndNullSelectedOption() throws Exception
    {
        Method method = QuizSessionManager.class.getDeclaredMethod("buildGapReviews", Question.class, Map.class);
        method.setAccessible(true);

        Question withoutGaps = new Question();
        withoutGaps.setQuestionType(QuestionType.GAP);
        withoutGaps.setGapFields(null);

        @SuppressWarnings("unchecked")
        List<RunReviewDto.GapReviewDto> emptyResult =
                (List<RunReviewDto.GapReviewDto>) method.invoke(manager, withoutGaps, Map.of());

        assertEquals(List.of(), emptyResult);

        Question question = new Question(QuestionType.GAP, "Start", null, "Ende", false, 4);
        question.setQuestionId(200L);

        GapField gap1 = new GapField(question, 0);
        gap1.setGapId(11L);
        gap1.setTextBefore(" vor ");
        gap1.setTextAfter(" nach ");

        GapOption gap1Correct = new GapOption(gap1, "richtig", true, 1);
        gap1Correct.setGapOptionId(111L);
        gap1.setGapOptions(new LinkedHashSet<>(List.of(gap1Correct)));

        GapField gap2 = new GapField(question, 1);
        gap2.setGapId(22L);
        gap2.setTextBefore("   ");
        gap2.setTextAfter(null);

        GapOption gap2Wrong = new GapOption(gap2, "falsch", false, 1);
        gap2Wrong.setGapOptionId(221L);
        gap2.setGapOptions(new LinkedHashSet<>(List.of(gap2Wrong)));

        GapField gap3 = new GapField(question, 2);
        gap3.setGapId(33L);
        gap3.setTextBefore("x");
        gap3.setTextAfter("y");

        GapOption gap3Correct = new GapOption(gap3, "ok", true, 1);
        gap3Correct.setGapOptionId(331L);
        gap3.setGapOptions(new LinkedHashSet<>(List.of(gap3Correct)));

        question.setGapFields(new LinkedHashSet<>(List.of(gap3, gap2, gap1)));

        GameRun run = new GameRun();
        run.setRunId(1L);

        RunGapAnswer correctAnswer = new RunGapAnswer(run, question, gap1, gap1Correct, LocalDateTime.now());

        RunGapAnswer nullSelectedOption = new RunGapAnswer();
        nullSelectedOption.setGapField(gap3);
        nullSelectedOption.setQuestion(question);
        nullSelectedOption.setRun(run);
        nullSelectedOption.setSelectedGapOption(null);

        @SuppressWarnings("unchecked")
        List<RunReviewDto.GapReviewDto> result =
                (List<RunReviewDto.GapReviewDto>) method.invoke(
                        manager,
                        question,
                        Map.of(
                                11L, correctAnswer,
                                33L, nullSelectedOption
                        )
                );

        assertEquals(3, result.size());

        assertEquals(11L, result.get(0).getGapId());
        assertEquals("vor _____ nach", result.get(0).getLabel());
        assertEquals("richtig", result.get(0).getSelectedText());
        assertEquals("richtig", result.get(0).getCorrectText());
        assertTrue(result.get(0).isCorrect());

        assertEquals(22L, result.get(1).getGapId());
        assertEquals("_____", result.get(1).getLabel());
        assertNull(result.get(1).getSelectedText());
        assertNull(result.get(1).getCorrectText());
        assertFalse(result.get(1).isCorrect());

        assertEquals(33L, result.get(2).getGapId());
        assertEquals("x _____ y", result.get(2).getLabel());
        assertNull(result.get(2).getSelectedText());
        assertEquals("ok", result.get(2).getCorrectText());
        assertFalse(result.get(2).isCorrect());
    }

    @Test
    void textHelpers_shouldBuildNormalizedQuestionAndGapTexts() throws Exception
    {
        Method questionTextMethod = QuizSessionManager.class.getDeclaredMethod("buildQuestionReviewText", Question.class);
        questionTextMethod.setAccessible(true);

        Method gapLabelMethod = QuizSessionManager.class.getDeclaredMethod("buildGapLabel", GapField.class);
        gapLabelMethod.setAccessible(true);

        Method appendMethod = QuizSessionManager.class.getDeclaredMethod("appendQuestionPart", StringBuilder.class, String.class);
        appendMethod.setAccessible(true);

        Question mcQuestion = new Question(QuestionType.MC, "  Hallo  ", null, "  Welt ", false, 2);
        String mcText = (String) questionTextMethod.invoke(manager, mcQuestion);
        assertEquals("Hallo Welt", mcText);

        Question gapQuestion = new Question(QuestionType.GAP, "  Start ", null, " Ende  ", false, 2);
        GapField gap = new GapField(gapQuestion, 0);
        gap.setGapId(1L);
        gap.setTextBefore(" vor ");
        gap.setTextAfter(" nach ");
        gapQuestion.setGapFields(new LinkedHashSet<>(List.of(gap)));

        String gapText = (String) questionTextMethod.invoke(manager, gapQuestion);
        assertEquals("Start vor _____ nach Ende", gapText);

        GapField blankGap = new GapField(gapQuestion, 1);
        blankGap.setGapId(2L);
        blankGap.setTextBefore("   ");
        blankGap.setTextAfter(null);

        String fallbackLabel = (String) gapLabelMethod.invoke(manager, blankGap);
        assertEquals("_____", fallbackLabel);

        StringBuilder builder = new StringBuilder();
        appendMethod.invoke(manager, builder, "  A  ");
        appendMethod.invoke(manager, builder, "   ");
        appendMethod.invoke(manager, builder, null);
        appendMethod.invoke(manager, builder, " B ");

        assertEquals("A B", builder.toString());
    }
}