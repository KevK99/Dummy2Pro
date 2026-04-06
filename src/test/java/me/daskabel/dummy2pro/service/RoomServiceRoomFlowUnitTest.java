package me.daskabel.dummy2pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.RunSelectedAnswer;
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
class RoomServiceRoomFlowUnitTest
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
    void getQuestion_shouldSortMcAnswerOptionsByOptionOrder()
    {
        Question question = new Question(QuestionType.MC, "Frage", "bild.png", "Ende", false, 5);
        question.setQuestionId(100L);

        AnswerOption option2 = new AnswerOption(question, "B", false, 2);
        option2.setAnswerId(1002L);

        AnswerOption option1 = new AnswerOption(question, "A", true, 1);
        option1.setAnswerId(1001L);

        question.setAnswerOptions(List.of(option2, option1));

        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        QuestionDto dto = roomService.getQuestion(100L, 1, 4);

        assertEquals(100L, dto.getQuestionId());
        assertEquals(1, dto.getCurrentIndex());
        assertEquals(4, dto.getTotalCount());
        assertEquals("A", dto.getAnswerOptions().get(0).getOptionText());
        assertEquals("B", dto.getAnswerOptions().get(1).getOptionText());
    }

    @Test
    void getQuestion_shouldSortGapFieldsAndGapOptions()
    {
        Question question = new Question(QuestionType.GAP, "Start", null, "Ende", false, 7);
        question.setQuestionId(200L);

        GapField gap2 = new GapField(question, 1);
        gap2.setGapId(22L);
        gap2.setTextBefore("zweite");
        gap2.setTextAfter("Lücke");

        GapField gap1 = new GapField(question, 0);
        gap1.setGapId(11L);
        gap1.setTextBefore("erste");
        gap1.setTextAfter("Lücke");

        GapOption gap1Option2 = new GapOption(gap1, "B", false, 2);
        gap1Option2.setGapOptionId(112L);
        GapOption gap1Option1 = new GapOption(gap1, "A", true, 1);
        gap1Option1.setGapOptionId(111L);
        gap1.setGapOptions(new LinkedHashSet<>(List.of(gap1Option2, gap1Option1)));

        GapOption gap2Option2 = new GapOption(gap2, "D", false, 2);
        gap2Option2.setGapOptionId(222L);
        GapOption gap2Option1 = new GapOption(gap2, "C", true, 1);
        gap2Option1.setGapOptionId(221L);
        gap2.setGapOptions(new LinkedHashSet<>(List.of(gap2Option2, gap2Option1)));

        question.setGapFields(new LinkedHashSet<>(List.of(gap2, gap1)));

        when(questionRepository.findById(200L)).thenReturn(Optional.of(question));

        QuestionDto dto = roomService.getQuestion(200L, 0, 2);

        assertEquals(2, dto.getGapFields().size());
        assertEquals(11L, dto.getGapFields().get(0).getGapId());
        assertEquals("A", dto.getGapFields().get(0).getGapOptions().get(0).getOptionText());
        assertEquals(22L, dto.getGapFields().get(1).getGapId());
        assertEquals("C", dto.getGapFields().get(1).getGapOptions().get(0).getOptionText());
    }

    @Test
    void getRoomStatus_shouldBuildBronzeStatusAndAnswerComparisons()
    {
        Long runId = 70L;

        Theme theme = new Theme("Recht");
        theme.setThemeId(10L);

        GameRun run = new GameRun();
        run.setRunId(runId);

        Question q1 = new Question(QuestionType.MC, "Q1", null, null, false, 5);
        q1.setQuestionId(101L);
        q1.setAnswerOptions(List.of(
                answer(1001L, q1, "A", true, 1),
                answer(1002L, q1, "B", false, 2)
        ));

        Question q2 = new Question(QuestionType.MC, "Q2", null, null, false, 5);
        q2.setQuestionId(102L);
        q2.setAnswerOptions(List.of(
                answer(2001L, q2, "Ja", true, 1),
                answer(2002L, q2, "Nein", false, 2)
        ));

        QuestionProgress p1 = new QuestionProgress(run, q1, 1, 1, ProgressStatus.CORRECT, LocalDateTime.now());
        QuestionProgress p2 = new QuestionProgress(run, q2, 1, 2, ProgressStatus.WRONG, LocalDateTime.now());

        RunSelectedAnswer selected1 = new RunSelectedAnswer(run, q1, q1.getAnswerOptions().get(0));
        RunSelectedAnswer selected2 = new RunSelectedAnswer(run, q2, q2.getAnswerOptions().get(1));

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionProgressRepository.findByRunIdAndRoomIdOrderByQuestionOrder(runId, 1)).thenReturn(List.of(p1, p2));
        when(runSelectedAnswerRepository.findByRun_RunIdAndQuestion_QuestionId(runId, 101L)).thenReturn(List.of(selected1));
        when(runSelectedAnswerRepository.findByRun_RunIdAndQuestion_QuestionId(runId, 102L)).thenReturn(List.of(selected2));

        RoomStatusDto status = roomService.getRoomStatus(1, runId);

        assertEquals(1, status.getRoomId());
        assertEquals("Recht", status.getThemeName());
        assertEquals(2, status.getTotalQuestions());
        assertEquals(2, status.getAnsweredQuestions());
        assertEquals(1, status.getCorrectAnswers());
        assertEquals(1, status.getWrongAnswers());
        assertEquals(0, status.getOpenQuestions());
        assertEquals(10, status.getTotalPoints());
        assertEquals(5, status.getEarnedPoints());
        assertEquals("BRONZE", status.getMedal());
        assertEquals(2, status.getAnswerComparisons().size());
        assertEquals(List.of(1001L), status.getAnswerComparisons().get(0).getSelectedAnswerIds());
    }

    @Test
    void startRoom_shouldThrow_whenNoQuestionsExistForRunAndRoom()
    {
        Long runId = 70L;

        Theme theme = new Theme("Recht");
        theme.setThemeId(10L);

        GameRun run = new GameRun();
        run.setRunId(runId);

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionProgressRepository.findByRunIdAndRoomIdOrderByQuestionOrder(runId, 1)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> roomService.startRoom(1, runId)
        );

        assertEquals("Keine Fragen für Raum 1 im Run 70 gefunden.", ex.getMessage());
    }

    @Test
    void startRoom_shouldReturnFirstQuestionSequenceAndStatus()
    {
        Long runId = 70L;

        Theme theme = new Theme("Recht");
        theme.setThemeId(10L);

        GameRun run = new GameRun();
        run.setRunId(runId);

        Question q1 = new Question(QuestionType.MC, "Q1", null, null, false, 5);
        q1.setQuestionId(101L);
        q1.setAnswerOptions(List.of(
                answer(1001L, q1, "A", true, 1),
                answer(1002L, q1, "B", false, 2)
        ));

        Question q2 = new Question(QuestionType.MC, "Q2", null, null, false, 3);
        q2.setQuestionId(102L);
        q2.setAnswerOptions(List.of(
                answer(2001L, q2, "Ja", true, 1),
                answer(2002L, q2, "Nein", false, 2)
        ));

        QuestionProgress p1 = new QuestionProgress(run, q1, 1, 1, ProgressStatus.OPEN, null);
        QuestionProgress p2 = new QuestionProgress(run, q2, 1, 2, ProgressStatus.OPEN, null);

        when(gameRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme));
        when(questionProgressRepository.findByRunIdAndRoomIdOrderByQuestionOrder(runId, 1)).thenReturn(List.of(p1, p2));
        when(runSelectedAnswerRepository.findByRun_RunIdAndQuestion_QuestionId(runId, 101L)).thenReturn(List.of());
        when(runSelectedAnswerRepository.findByRun_RunIdAndQuestion_QuestionId(runId, 102L)).thenReturn(List.of());

        RoomStartDto result = roomService.startRoom(1, runId);

        assertEquals(101L, result.getFirstQuestion().getQuestionId());
        assertEquals(List.of(101L, 102L), result.getQuestionSequence());
        assertEquals(2, result.getStatus().getTotalQuestions());
        assertEquals(0, result.getStatus().getAnsweredQuestions());
        assertEquals("NONE", result.getStatus().getMedal());
    }

    private AnswerOption answer(Long id, Question question, String text, boolean correct, int order)
    {
        AnswerOption option = new AnswerOption(question, text, correct, order);
        option.setAnswerId(id);
        return option;
    }
}