package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
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
class QuizSessionManagerBranchPushTest
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
    void loadSessionForRun_shouldDefaultToRoom1_whenNoRoomStatusExists()
    {
        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 15, 0));
        run.setRunId(70L);

        QuizSession skeleton = new QuizSession(7L, 70L);
        skeleton.addRoom(emptyRoom(1));
        skeleton.addRoom(emptyRoom(2));

        when(gameRunRepository.findById(70L)).thenReturn(Optional.of(run));
        when(generator.generateSkeleton(7L, 70L)).thenReturn(skeleton);
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Recht"), new Theme("SQL")));
        when(questionProgressRepository.summarizeRoomProgressByRunId(70L)).thenReturn(List.of());

        QuizSession loaded = manager.loadSessionForRun(70L);

        assertEquals(1, loaded.getActiveRoomId());
    }

    @Test
    void getOverview_shouldMarkSessionFullyCompleted_whenAllRoomsFinished()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(emptyRoom(1));
        session.addRoom(emptyRoom(2));
        cacheSession(session);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Recht"), new Theme("SQL")));
        when(questionProgressRepository.summarizeRoomProgressByRunId(70L)).thenReturn(List.of(
                summary(1, 2L, 2L, 2L, 0L, 10L, 10L),
                summary(2, 3L, 3L, 2L, 1L, 15L, 10L)
        ));

        var overview = manager.getOverview(session.getSessionId());

        assertTrue(overview.isFullyCompleted());
        assertEquals(20, overview.getTotalEarnedPoints());
        assertEquals(25, overview.getTotalMaxPoints());
    }

    @Test
    void prepareRoom_shouldReturnExistingPreparedRoom_withoutRepositoryWork()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(preparedRoom(1, 101L));
        cacheSession(session);

        var status = manager.prepareRoom(session.getSessionId(), 1);

        assertEquals(1, status.getRoomId());
        assertEquals(1, status.getTotalQuestions());

        verifyNoInteractions(questionProgressRepository);
        verifyNoInteractions(gameRunRepository);
    }

    @Test
    void submitAnswer_shouldThrow_whenRoomAlreadyCompleted()
    {
        QuizSession.RoomSession room = preparedRoom(1, 101L);
        room.setCompleted(true);

        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(room);
        cacheSession(session);

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(101L);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> manager.submitAnswer(session.getSessionId(), 1, request)
        );

        assertEquals("Der Raum ist bereits abgeschlossen.", ex.getMessage());
    }

    @Test
    void submitAnswer_shouldThrow_whenPersistedQuestionProgressIsMissing()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(preparedRoom(1, 101L));
        cacheSession(session);

        Question question = new Question(QuestionType.MC, "Frage", null, null, false, 5);
        question.setQuestionId(101L);

        AnswerOption correct = new AnswerOption(question, "A", true, 1);
        correct.setAnswerId(1001L);
        question.setAnswerOptions(List.of(correct));

        when(questionRepository.findByQuestionIdWithAnswers(101L)).thenReturn(Optional.of(question));
        when(questionProgressRepository.updateStatusAndAnsweredAt(
                org.mockito.ArgumentMatchers.eq(70L),
                org.mockito.ArgumentMatchers.eq(101L),
                org.mockito.ArgumentMatchers.eq(ProgressStatus.CORRECT),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(0);

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(101L);
        request.setSelectedAnswerIds(List.of(1001L));

        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> manager.submitAnswer(session.getSessionId(), 1, request)
        );

        assertTrue(ex.getMessage().contains("QuestionProgress"));
    }

    @Test
    void removeRunSession_shouldKeepUserMapping_whenAnotherSessionIsCurrent()
    {
        QuizSession oldSession = new QuizSession(7L, 70L);
        oldSession.addRoom(emptyRoom(1));

        QuizSession currentSession = new QuizSession(7L, 71L);
        currentSession.addRoom(emptyRoom(1));

        putSession(oldSession);
        putSession(currentSession);

        @SuppressWarnings("unchecked")
        Map<Long, String> userSessionMap = (Map<Long, String>) ReflectionTestUtils.getField(manager, "userSessionMap");
        userSessionMap.put(7L, currentSession.getSessionId());

        manager.removeRunSession(70L);

        assertEquals(currentSession.getSessionId(), userSessionMap.get(7L));
        assertEquals(currentSession, manager.getSession(currentSession.getSessionId()));
    }

    @Test
    void cleanupExpiredSessions_shouldRemoveOnlyExpiredOnes()
    {
        QuizSession expired = new QuizSession(7L, 70L);
        expired.addRoom(emptyRoom(1));

        QuizSession fresh = new QuizSession(8L, 80L);
        fresh.addRoom(emptyRoom(1));

        putSession(expired);
        putSession(fresh);

        ReflectionTestUtils.setField(expired, "lastActivityAt", LocalDateTime.now().minusHours(3));
        ReflectionTestUtils.setField(fresh, "lastActivityAt", LocalDateTime.now().minusMinutes(10));

        int removed = manager.cleanupExpiredSessions();

        assertEquals(1, removed);
        assertEquals(fresh, manager.getSession(fresh.getSessionId()));
    }

    private QuizSession.RoomSession emptyRoom(int roomId)
    {
        return new QuizSession.RoomSession(roomId, "Thema " + roomId, List.of(), Map.of(), 0);
    }

    private QuizSession.RoomSession preparedRoom(int roomId, Long questionId)
    {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(questionId);
        dto.setQuestionType(QuestionType.MC);

        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(questionId, dto);

        return new QuizSession.RoomSession(roomId, "Thema " + roomId, List.of(questionId), cache, 5);
    }

    @SuppressWarnings("unchecked")
    private void cacheSession(QuizSession session)
    {
        ((Map<String, QuizSession>) ReflectionTestUtils.getField(manager, "sessions"))
                .put(session.getSessionId(), session);
        ((Map<Long, String>) ReflectionTestUtils.getField(manager, "runSessionMap"))
                .put(session.getRunId(), session.getSessionId());
    }

    @SuppressWarnings("unchecked")
    private void putSession(QuizSession session)
    {
        ((Map<String, QuizSession>) ReflectionTestUtils.getField(manager, "sessions"))
                .put(session.getSessionId(), session);
        ((Map<Long, String>) ReflectionTestUtils.getField(manager, "runSessionMap"))
                .put(session.getRunId(), session.getSessionId());
    }

    private QuestionProgressRepository.RoomProgressSummary summary(
            Integer roomId,
            Long totalQuestions,
            Long answeredQuestions,
            Long correctAnswers,
            Long wrongAnswers,
            Long totalPoints,
            Long earnedPoints)
    {
        return new QuestionProgressRepository.RoomProgressSummary()
        {
            @Override
            public Integer getRoomId()
            {
                return roomId;
            }

            @Override
            public Long getTotalQuestions()
            {
                return totalQuestions;
            }

            @Override
            public Long getAnsweredQuestions()
            {
                return answeredQuestions;
            }

            @Override
            public Long getCorrectAnswers()
            {
                return correctAnswers;
            }

            @Override
            public Long getWrongAnswers()
            {
                return wrongAnswers;
            }

            @Override
            public Long getTotalPoints()
            {
                return totalPoints;
            }

            @Override
            public Long getEarnedPoints()
            {
                return earnedPoints;
            }
        };
    }
}