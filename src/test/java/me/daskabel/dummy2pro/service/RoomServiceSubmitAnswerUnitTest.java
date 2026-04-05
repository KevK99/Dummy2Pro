package me.daskabel.dummy2pro.service;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapAnswerEntry;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceSubmitAnswerUnitTest
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
    void submitAnswer_mcCorrect_persistsCorrectProgressAndSelection()
    {
        Long runId = 10L;
        Theme theme = new Theme("Thema 1");
        theme.setThemeId(100L);

        Question question = buildMcQuestion(theme);
        GameRun run = new GameRun();
        run.setRunId(runId);

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
        request.setSelectedAnswerIds(List.of(1001L));

        AnswerResultDto result = roomService.submitAnswer(1, runId, request);

        assertTrue(result.isCorrect());
        assertEquals(5, result.getPointsEarned());
        assertEquals(ProgressStatus.CORRECT, progress.getStatus());

        verify(questionProgressRepository).save(progress);
        verify(runSelectedAnswerRepository).deleteByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId());
        verify(runSelectedAnswerRepository, times(1)).save(any());
        verify(runGapAnswerRepository, never()).save(any());
    }

    @Test
    void submitAnswer_questionNotFound_throwsException()
    {
        Long runId = 10L;
        GameRun run = new GameRun();
        run.setRunId(runId);

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(999L);
        request.setSelectedAnswerIds(List.of(1L));

        assertThrows(NoSuchElementException.class, () -> roomService.submitAnswer(1, runId, request));
    }

    @Test
    void submitAnswer_questionDoesNotBelongToRoom_throwsException()
    {
        Long runId = 10L;
        Theme roomTheme = new Theme("Raum 1");
        roomTheme.setThemeId(1L);

        Theme otherTheme = new Theme("Anderes Thema");
        otherTheme.setThemeId(2L);

        GameRun run = new GameRun();
        run.setRunId(runId);

        Question question = buildMcQuestion(otherTheme);

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(roomTheme));
        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(question.getQuestionId());
        request.setSelectedAnswerIds(List.of(1001L));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roomService.submitAnswer(1, runId, request)
        );

        assertTrue(ex.getMessage().contains("gehört nicht zu Raum 1"));
    }

    @Test
    void submitAnswer_invalidAnswerId_throwsException()
    {
        Long runId = 10L;
        Theme theme = new Theme("Thema 1");
        theme.setThemeId(100L);

        Question question = buildMcQuestion(theme);
        GameRun run = new GameRun();
        run.setRunId(runId);

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
        request.setSelectedAnswerIds(List.of(9999L));

        assertThrows(IllegalArgumentException.class, () -> roomService.submitAnswer(1, runId, request));
    }

    @Test
    void submitAnswer_gapWithInvalidGapId_throwsException()
    {
        Long runId = 10L;
        Theme theme = new Theme("Thema 1");
        theme.setThemeId(100L);

        Question question = buildGapQuestion(theme);
        GameRun run = new GameRun();
        run.setRunId(runId);

        QuestionProgress progress = new QuestionProgress();
        progress.setRun(run);
        progress.setQuestion(question);
        progress.setStatus(ProgressStatus.OPEN);

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionRepository.findById(question.getQuestionId())).thenReturn(Optional.of(question));
        when(questionProgressRepository.findByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId()))
                .thenReturn(Optional.of(progress));

        GapAnswerEntry entry = new GapAnswerEntry();
        entry.setGapId(999L);
        entry.setSelectedGapOptionId(3001L);

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(question.getQuestionId());
        request.setGapAnswers(List.of(entry));

        assertThrows(IllegalArgumentException.class, () -> roomService.submitAnswer(1, runId, request));
    }

    private Question buildMcQuestion(Theme theme)
    {
        Question question = new Question();
        question.setQuestionId(500L);
        question.setQuestionType(QuestionType.MC);
        question.setPoints(5);
        question.setThemes(List.of(theme));

        AnswerOption correct = new AnswerOption();
        correct.setAnswerId(1001L);
        correct.setOptionText("richtig");
        correct.setIsCorrect(true);
        correct.setOptionOrder(1);
        correct.setQuestion(question);

        AnswerOption wrong = new AnswerOption();
        wrong.setAnswerId(1002L);
        wrong.setOptionText("falsch");
        wrong.setIsCorrect(false);
        wrong.setOptionOrder(2);
        wrong.setQuestion(question);

        question.setAnswerOptions(List.of(correct, wrong));
        return question;
    }

    private Question buildGapQuestion(Theme theme)
    {
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
        correct.setOptionOrder(1);

        GapOption wrong = new GapOption();
        wrong.setGapOptionId(3002L);
        wrong.setGapField(gapField);
        wrong.setOptionText("falsch");
        wrong.setIsCorrect(false);
        wrong.setOptionOrder(2);

        gapField.setGapOptions(Set.of(correct, wrong));
        question.setGapFields(Set.of(gapField));
        return question;
    }
}