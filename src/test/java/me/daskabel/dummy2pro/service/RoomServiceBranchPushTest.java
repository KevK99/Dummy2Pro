package me.daskabel.dummy2pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceBranchPushTest
{
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ThemeRepository themeRepository;
    @Mock
    private GameRunRepository gameRunRepository;
    @Mock
    private QuestionProgressRepository questionProgressRepository;
    @Mock
    private RunSelectedAnswerRepository runSelectedAnswerRepository;
    @Mock
    private RunGapAnswerRepository runGapAnswerRepository;

    private RoomService roomService;

    @BeforeEach
    void setUp()
    {
        roomService = new RoomService(
                questionRepository,
                themeRepository,
                gameRunRepository,
                questionProgressRepository,
                runSelectedAnswerRepository,
                runGapAnswerRepository
        );
    }

    @Test
    void getQuestion_shouldThrow_whenQuestionDoesNotExist()
    {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> roomService.getQuestion(999L, 0, 1));
    }

    @Test
    void getRoomStatus_shouldThrow_whenRunIdIsNull()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roomService.getRoomStatus(1, null)
        );

        assertEquals("runId darf nicht null sein.", ex.getMessage());
    }

    @Test
    void getRoomStatus_shouldThrow_whenRoomIdIsInvalid()
    {
        GameRun run = new GameRun();
        run.setRunId(70L);

        when(gameRunRepository.findById(70L)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(new Theme("Recht")));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roomService.getRoomStatus(2, 70L)
        );

        assertEquals("Raum-ID ist ungültig.", ex.getMessage());
    }

    @Test
    void submitAnswer_shouldStoreWrongResult_whenNoMcAnswersWereSelected()
    {
        Long runId = 10L;
        Theme theme = new Theme("Thema 1");
        theme.setThemeId(100L);

        GameRun run = new GameRun();
        run.setRunId(runId);

        Question question = new Question();
        question.setQuestionId(500L);
        question.setQuestionType(QuestionType.MC);
        question.setPoints(5);
        question.setThemes(List.of(theme));

        AnswerOption correct = new AnswerOption();
        correct.setAnswerId(1001L);
        correct.setOptionText("richtig");
        correct.setIsCorrect(true);
        correct.setQuestion(question);

        question.setAnswerOptions(List.of(correct));

        QuestionProgress progress = new QuestionProgress();
        progress.setRun(run);
        progress.setQuestion(question);
        progress.setStatus(ProgressStatus.OPEN);

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));
        when(questionProgressRepository.findByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId()))
                .thenReturn(Optional.of(progress));

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(question.getQuestionId());
        request.setSelectedAnswerIds(null);

        var result = roomService.submitAnswer(1, runId, request);

        assertFalse(result.isCorrect());
        assertEquals(0, result.getPointsEarned());
        assertEquals(ProgressStatus.WRONG, progress.getStatus());

        verify(runSelectedAnswerRepository).deleteByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId());
        verify(runSelectedAnswerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submitAnswer_shouldStoreWrongResult_whenGapAnswersAreMissing()
    {
        Long runId = 10L;
        Theme theme = new Theme("Thema 1");
        theme.setThemeId(100L);

        GameRun run = new GameRun();
        run.setRunId(runId);

        Question question = new Question();
        question.setQuestionId(600L);
        question.setQuestionType(QuestionType.GAP);
        question.setPoints(7);
        question.setThemes(List.of(theme));

        GapField gapField = new GapField();
        gapField.setGapId(2001L);
        gapField.setQuestion(question);
        gapField.setGapIndex(0);

        GapOption correct = new GapOption();
        correct.setGapOptionId(3001L);
        correct.setGapField(gapField);
        correct.setOptionText("richtig");
        correct.setIsCorrect(true);

        gapField.setGapOptions(new LinkedHashSet<>(List.of(correct)));
        question.setGapFields(new LinkedHashSet<>(List.of(gapField)));

        QuestionProgress progress = new QuestionProgress();
        progress.setRun(run);
        progress.setQuestion(question);
        progress.setStatus(ProgressStatus.OPEN);

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));
        when(questionProgressRepository.findByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId()))
                .thenReturn(Optional.of(progress));

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(question.getQuestionId());
        request.setGapAnswers(null);

        var result = roomService.submitAnswer(1, runId, request);

        assertFalse(result.isCorrect());
        assertEquals(0, result.getPointsEarned());
        assertEquals(ProgressStatus.WRONG, progress.getStatus());

        verify(runGapAnswerRepository).deleteByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId());
        verify(runGapAnswerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submitAnswer_shouldThrow_whenQuestionProgressIsMissing()
    {
        Long runId = 10L;
        Theme theme = new Theme("Thema 1");
        theme.setThemeId(100L);

        GameRun run = new GameRun();
        run.setRunId(runId);

        Question question = new Question();
        question.setQuestionId(500L);
        question.setQuestionType(QuestionType.MC);
        question.setPoints(5);
        question.setThemes(List.of(theme));

        AnswerOption correct = new AnswerOption();
        correct.setAnswerId(1001L);
        correct.setOptionText("richtig");
        correct.setIsCorrect(true);
        correct.setQuestion(question);
        question.setAnswerOptions(List.of(correct));

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));
        when(questionProgressRepository.findByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId()))
                .thenReturn(Optional.empty());

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(question.getQuestionId());
        request.setSelectedAnswerIds(List.of(1001L));

        assertThrows(java.util.NoSuchElementException.class, () -> roomService.submitAnswer(1, runId, request));
    }

    @Test
    void privateGetRoomIdForQuestion_shouldThrow_whenQuestionHasNoTheme()
            throws Exception
    {
        Question question = new Question();
        question.setQuestionId(700L);
        question.setThemes(List.of());

        Method method = RoomService.class.getDeclaredMethod("getRoomIdForQuestion", Question.class);
        method.setAccessible(true);

        Exception ex = assertThrows(Exception.class, () -> method.invoke(roomService, question));

        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("Frage 700 hat kein Theme.", ex.getCause().getMessage());
    }

    @Test
    void privateGetRoomIdForQuestion_shouldThrow_whenQuestionHasMultipleThemes()
            throws Exception
    {
        Theme theme1 = new Theme("A");
        theme1.setThemeId(1L);

        Theme theme2 = new Theme("B");
        theme2.setThemeId(2L);

        Question question = new Question();
        question.setQuestionId(701L);
        question.setThemes(List.of(theme1, theme2));

        Method method = RoomService.class.getDeclaredMethod("getRoomIdForQuestion", Question.class);
        method.setAccessible(true);

        Exception ex = assertThrows(Exception.class, () -> method.invoke(roomService, question));

        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals(
                "Frage 701 hat mehrere Themes und ist keinem eindeutigen Raum zuordenbar.",
                ex.getCause().getMessage()
        );
    }

    private static void assertTrue(boolean condition)
    {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }

    @Test
    void privateLoadAllQuestionsForTheme_shouldReturnEmptyList_whenThemeHasNoQuestions() throws Exception
    {
        Theme theme = new Theme("Thema 1");
        theme.setThemeId(100L);

        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionRepository.findQuestionIdsByThemeId(100L)).thenReturn(List.of());

        Method method = RoomService.class.getDeclaredMethod("loadAllQuestionsForTheme", int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Question> result = (List<Question>) method.invoke(roomService, 1);

        assertEquals(List.of(), result);
    }

    @Test
    void privateLoadAllQuestionsForTheme_shouldMergeGapFieldsKeepOrderAndIncludeGapOnlyQuestions() throws Exception
    {
        Theme theme = new Theme("Thema 1");
        theme.setThemeId(100L);

        Question answerQuestion = new Question(QuestionType.MC, "Q1", null, null, false, 5);
        answerQuestion.setQuestionId(10L);

        AnswerOption answer = new AnswerOption(answerQuestion, "A", true, 1);
        answer.setAnswerId(1001L);
        answerQuestion.setAnswerOptions(List.of(answer));

        Question gapOverlay = new Question(QuestionType.MC, "Q1", null, null, false, 5);
        gapOverlay.setQuestionId(10L);

        GapField mergedGap = new GapField(gapOverlay, 0);
        mergedGap.setGapId(500L);
        mergedGap.setTextBefore("vor");
        mergedGap.setTextAfter("nach");

        GapOption mergedGapOption = new GapOption(mergedGap, "opt", true, 1);
        mergedGapOption.setGapOptionId(501L);
        mergedGap.setGapOptions(new LinkedHashSet<>(List.of(mergedGapOption)));
        gapOverlay.setGapFields(new LinkedHashSet<>(List.of(mergedGap)));

        Question gapOnlyQuestion = new Question(QuestionType.GAP, "Q2", null, null, false, 7);
        gapOnlyQuestion.setQuestionId(20L);

        GapField gapOnlyField = new GapField(gapOnlyQuestion, 0);
        gapOnlyField.setGapId(600L);
        gapOnlyField.setTextBefore("x");
        gapOnlyField.setTextAfter("y");

        GapOption gapOnlyOption = new GapOption(gapOnlyField, "ok", true, 1);
        gapOnlyOption.setGapOptionId(601L);
        gapOnlyField.setGapOptions(new LinkedHashSet<>(List.of(gapOnlyOption)));
        gapOnlyQuestion.setGapFields(new LinkedHashSet<>(List.of(gapOnlyField)));

        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionRepository.findQuestionIdsByThemeId(100L)).thenReturn(List.of(10L, 20L));
        when(questionRepository.findByQuestionIdsWithAnswers(List.of(10L, 20L))).thenReturn(List.of(answerQuestion));
        when(questionRepository.findByQuestionIdsWithGaps(List.of(10L, 20L))).thenReturn(List.of(gapOverlay, gapOnlyQuestion));

        Method method = RoomService.class.getDeclaredMethod("loadAllQuestionsForTheme", int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Question> result = (List<Question>) method.invoke(roomService, 1);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getQuestionId());
        assertEquals(1, result.get(0).getGapFields().size());
        assertEquals(20L, result.get(1).getQuestionId());
        assertEquals(1, result.get(1).getGapFields().size());
    }

    @Test
    void privateGetRoomIdForThemeId_shouldReturnIndexAndThrowForUnknownTheme() throws Exception
    {
        Theme theme1 = new Theme("A");
        theme1.setThemeId(11L);

        Theme theme2 = new Theme("B");
        theme2.setThemeId(22L);

        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme1, theme2));

        Method method = RoomService.class.getDeclaredMethod("getRoomIdForThemeId", Long.class);
        method.setAccessible(true);

        int roomId = (int) method.invoke(roomService, 22L);
        assertEquals(2, roomId);

        Exception ex = assertThrows(Exception.class, () -> method.invoke(roomService, 99L));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("Kein Raum für themeId 99 gefunden.", ex.getCause().getMessage());
    }
}