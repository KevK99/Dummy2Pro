package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.GapAnswerEntry;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuizSessionManagerPersistenceTest
{
    @Mock
    private QuizSessionGenerator generator;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private GameRunRepository gameRunRepository;
    @Mock
    private QuestionProgressRepository questionProgressRepository;
    @Mock
    private RunSelectedAnswerRepository runSelectedAnswerRepository;
    @Mock
    private RunGapAnswerRepository runGapAnswerRepository;
    @Mock
    private UserRepository userRepository;

    private QuizSessionManager manager;

    @BeforeEach
    void setUp()
    {
        manager = new QuizSessionManager(
                generator,
                questionRepository,
                gameRunRepository,
                questionProgressRepository,
                runSelectedAnswerRepository,
                runGapAnswerRepository,
                userRepository
        );
    }

    @Test
    void prepareRoom_shouldBuildRoomAndPersistInitialOpenProgress()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(new QuizSession.RoomSession(1, "Recht", List.of(), Map.of(), 0));
        cacheSession(session);

        Theme theme = new Theme("Recht");
        theme.setThemeId(10L);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 11, 0));
        run.setRunId(70L);

        Question question1 = question(101L, 5);
        Question question2 = question(102L, 3);

        QuizSession.RoomSession preparedRoom = roomWithQuestions(1, 101L, 102L);

        when(questionProgressRepository.findByRunIdAndRoomIdOrderByQuestionOrder(70L, 1))
                .thenReturn(List.of());
        when(generator.getThemesOrdered()).thenReturn(List.of(theme));
        when(generator.buildRoomSession(theme, 1)).thenReturn(preparedRoom);
        when(gameRunRepository.findById(70L)).thenReturn(Optional.of(run));
        when(questionRepository.getReferenceById(101L)).thenReturn(question1);
        when(questionRepository.getReferenceById(102L)).thenReturn(question2);

        RoomStartDto roomState = manager.getRoomState(session.getSessionId(), 1);

        assertNotNull(roomState.getFirstQuestion());
        assertEquals(101L, roomState.getFirstQuestion().getQuestionId());
        assertEquals(2, roomState.getQuestionSequence().size());

        verify(questionProgressRepository).saveAll(anyList());
    }

    @Test
    void submitAnswer_shouldPersistSelectedAnswers_forCorrectMcQuestion_andFinishRun()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(roomWithQuestions(1, 101L));
        cacheSession(session);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 11, 30));
        run.setRunId(70L);

        Question question = question(101L, 5);
        AnswerOption correct = new AnswerOption(question, "A", true, 1);
        correct.setAnswerId(1001L);
        AnswerOption wrong = new AnswerOption(question, "B", false, 2);
        wrong.setAnswerId(1002L);
        question.setAnswerOptions(List.of(correct, wrong));

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(101L);
        request.setSelectedAnswerIds(List.of(1001L));

        when(questionRepository.findByQuestionIdWithAnswers(101L)).thenReturn(Optional.of(question));
        when(questionProgressRepository.updateStatusAndAnsweredAt(
                eq(70L),
                eq(101L),
                eq(ProgressStatus.CORRECT),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(gameRunRepository.getReferenceById(70L)).thenReturn(run);
        when(gameRunRepository.findById(70L)).thenReturn(Optional.of(run));

        var result = manager.submitAnswer(session.getSessionId(), 1, request);

        assertTrue(result.isCorrect());
        assertEquals(5, result.getPointsEarned());
        assertTrue(session.getRoom(1).isCompleted());
        assertNotNull(run.getFinishedAt());

        verify(runSelectedAnswerRepository).deleteByRun_RunIdAndQuestion_QuestionId(70L, 101L);
        verify(runSelectedAnswerRepository).saveAll(anyList());
        verify(runGapAnswerRepository, never()).saveAll(anyList());
        verify(gameRunRepository).save(run);
    }

    @Test
    void submitAnswer_shouldPersistGapAnswers_forCorrectGapQuestion()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(roomWithGapQuestion(1, 201L));
        cacheSession(session);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 12, 0));
        run.setRunId(70L);

        Question question = new Question(QuestionType.GAP, "Start", null, "Ende", false, 4);
        question.setQuestionId(201L);

        GapField gapField = new GapField(question, 0);
        gapField.setGapId(301L);
        gapField.setTextBefore("vor");
        gapField.setTextAfter("nach");

        GapOption correctOption = new GapOption(gapField, "richtig", true, 1);
        correctOption.setGapOptionId(401L);
        gapField.setGapOptions(new java.util.LinkedHashSet<>(List.of(correctOption)));
        question.setGapFields(new java.util.LinkedHashSet<>(List.of(gapField)));

        GapAnswerEntry entry = new GapAnswerEntry();
        entry.setGapId(301L);
        entry.setSelectedGapOptionId(401L);

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(201L);
        request.setGapAnswers(List.of(entry));

        when(questionRepository.findByQuestionIdWithGaps(201L)).thenReturn(Optional.of(question));
        when(questionProgressRepository.updateStatusAndAnsweredAt(
                eq(70L),
                eq(201L),
                eq(ProgressStatus.CORRECT),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(gameRunRepository.getReferenceById(70L)).thenReturn(run);
        when(gameRunRepository.findById(70L)).thenReturn(Optional.of(run));

        var result = manager.submitAnswer(session.getSessionId(), 1, request);

        assertTrue(result.isCorrect());
        assertEquals(4, result.getPointsEarned());
        assertTrue(session.getRoom(1).isCompleted());

        verify(runGapAnswerRepository).deleteByRun_RunIdAndQuestion_QuestionId(70L, 201L);
        verify(runGapAnswerRepository).saveAll(anyList());
        verify(runSelectedAnswerRepository, never()).saveAll(anyList());
    }

    @Test
    void submitAnswer_shouldThrow_whenSelectedAnswerIdIsInvalid()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(roomWithQuestions(1, 101L));
        cacheSession(session);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 12, 30));
        run.setRunId(70L);

        Question question = question(101L, 5);
        AnswerOption correct = new AnswerOption(question, "A", true, 1);
        correct.setAnswerId(1001L);
        question.setAnswerOptions(List.of(correct));

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(101L);
        request.setSelectedAnswerIds(List.of(9999L));

        when(questionRepository.findByQuestionIdWithAnswers(101L)).thenReturn(Optional.of(question));
        when(questionProgressRepository.updateStatusAndAnsweredAt(
                eq(70L),
                eq(101L),
                eq(ProgressStatus.WRONG),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(gameRunRepository.getReferenceById(70L)).thenReturn(run);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> manager.submitAnswer(session.getSessionId(), 1, request)
        );

        assertEquals("Ungültige answerId: 9999", ex.getMessage());
        verify(runSelectedAnswerRepository).deleteByRun_RunIdAndQuestion_QuestionId(70L, 101L);
    }

    @Test
    void submitAnswer_shouldThrow_whenGapOptionIdIsInvalid()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(roomWithGapQuestion(1, 201L));
        cacheSession(session);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 13, 0));
        run.setRunId(70L);

        Question question = new Question(QuestionType.GAP, "Start", null, "Ende", false, 4);
        question.setQuestionId(201L);

        GapField gapField = new GapField(question, 0);
        gapField.setGapId(301L);
        gapField.setTextBefore("vor");
        gapField.setTextAfter("nach");

        GapOption correctOption = new GapOption(gapField, "richtig", true, 1);
        correctOption.setGapOptionId(401L);
        gapField.setGapOptions(new java.util.LinkedHashSet<>(List.of(correctOption)));
        question.setGapFields(new java.util.LinkedHashSet<>(List.of(gapField)));

        GapAnswerEntry entry = new GapAnswerEntry();
        entry.setGapId(301L);
        entry.setSelectedGapOptionId(9999L);

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(201L);
        request.setGapAnswers(List.of(entry));

        when(questionRepository.findByQuestionIdWithGaps(201L)).thenReturn(Optional.of(question));
        when(questionProgressRepository.updateStatusAndAnsweredAt(
                eq(70L),
                eq(201L),
                eq(ProgressStatus.WRONG),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(gameRunRepository.getReferenceById(70L)).thenReturn(run);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> manager.submitAnswer(session.getSessionId(), 1, request)
        );

        assertEquals("Ungültige selectedGapOptionId: 9999", ex.getMessage());
        verify(runGapAnswerRepository).deleteByRun_RunIdAndQuestion_QuestionId(70L, 201L);
    }

    @Test
    void createNewRunSession_shouldThrow_whenUserIdIsNull()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> manager.createNewRunSession(null)
        );

        assertEquals("userId darf nicht null sein.", ex.getMessage());
    }

    @Test
    void cleanupExpiredSessions_shouldReturnZero_whenNothingExpired()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(roomWithQuestions(1, 101L));
        cacheSession(session);

        int removed = manager.cleanupExpiredSessions();

        assertEquals(0, removed);
        assertEquals(1, manager.getActiveSessionCount());
    }

    private Question question(Long questionId, int points)
    {
        Question question = new Question(QuestionType.MC, "Frage", null, null, false, points);
        question.setQuestionId(questionId);
        return question;
    }

    private QuizSession.RoomSession roomWithQuestions(int roomId, Long... questionIds)
    {
        Map<Long, QuestionDto> cache = new HashMap<>();

        for (Long questionId : questionIds)
        {
            QuestionDto dto = new QuestionDto();
            dto.setQuestionId(questionId);
            dto.setQuestionType(QuestionType.MC);
            cache.put(questionId, dto);
        }

        return new QuizSession.RoomSession(
                roomId,
                "Thema " + roomId,
                List.of(questionIds),
                cache,
                5 * questionIds.length
        );
    }

    private QuizSession.RoomSession roomWithGapQuestion(int roomId, Long questionId)
    {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(questionId);
        dto.setQuestionType(QuestionType.GAP);

        return new QuizSession.RoomSession(
                roomId,
                "Thema " + roomId,
                List.of(questionId),
                Map.of(questionId, dto),
                4
        );
    }

    @SuppressWarnings("unchecked")
    private void cacheSession(QuizSession session)
    {
        ((Map<String, QuizSession>) ReflectionTestUtils.getField(manager, "sessions"))
                .put(session.getSessionId(), session);
        ((Map<Long, String>) ReflectionTestUtils.getField(manager, "runSessionMap"))
                .put(session.getRunId(), session.getSessionId());
    }
}