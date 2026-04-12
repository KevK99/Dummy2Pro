package me.daskabel.dummy2pro.session;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.QuestionType;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Grundlegende Unittests für die Kernfunktionen des
 * {@link QuizSessionManager}.
 *
 * Getestet werden das Erzeugen und Laden von Sitzungen, das Weiterblättern
 * im Frageablauf, das Einreichen von Antworten sowie das Entfernen und
 * Bereinigen gespeicherter Sessions.
 */
@ExtendWith(MockitoExtension.class)
class QuizSessionManagerUnitTest
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
    void createNewRunSession_success_createsAndCachesSession()
    {
        User user = new User("jan", "HASH", "duck.jpg");
        user.setUserId(1L);

        GameRun run = new GameRun();
        run.setRunId(100L);
        run.setUser(user);

        QuizSession session = sessionWithPreparedRoom(1L, 100L, 1000L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gameRunRepository.save(any(GameRun.class))).thenReturn(run);
        when(generator.generateSkeleton(1L, 100L)).thenReturn(session);

        QuizSession created = manager.createNewRunSession(1L);

        assertNotNull(created.getSessionId());
        assertEquals(100L, created.getRunId());
        assertEquals(1, manager.getActiveSessionCount());
        assertSame(created, manager.getSession(created.getSessionId()));
    }

    @Test
    void createNewRunSession_missingUser_throwsException()
    {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> manager.createNewRunSession(99L));
    }

    @Test
    void loadSessionForRun_returnsCachedSessionWhenAlreadyLoaded()
    {
        User user = new User("jan", "HASH", "duck.jpg");
        user.setUserId(1L);

        GameRun run = new GameRun();
        run.setRunId(100L);
        run.setUser(user);

        QuizSession session = sessionWithPreparedRoom(1L, 100L, 1000L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gameRunRepository.save(any(GameRun.class))).thenReturn(run);
        when(generator.generateSkeleton(1L, 100L)).thenReturn(session);

        QuizSession created = manager.createNewRunSession(1L);
        QuizSession loaded = manager.loadSessionForRun(100L);

        assertSame(created, loaded);
        verify(generator, times(1)).generateSkeleton(1L, 100L);
    }

    @Test
    void getSession_unknownSession_throwsException()
    {
        assertThrows(NoSuchElementException.class, () -> manager.getSession("nicht-da"));
    }

    @Test
    void advance_requiresCurrentQuestionAnsweredFirst()
    {
        User user = new User("jan", "HASH", "duck.jpg");
        user.setUserId(1L);

        GameRun run = new GameRun();
        run.setRunId(100L);
        run.setUser(user);

        QuizSession session = sessionWithPreparedRoom(1L, 100L, 1000L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gameRunRepository.save(any(GameRun.class))).thenReturn(run);
        when(generator.generateSkeleton(1L, 100L)).thenReturn(session);

        QuizSession created = manager.createNewRunSession(1L);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> manager.advance(created.getSessionId(), 1)
        );

        assertEquals("Die aktuelle Frage muss zuerst beantwortet werden.", ex.getMessage());
    }

    @Test
    void submitAnswer_rejectsNonCurrentQuestion()
    {
        User user = new User("jan", "HASH", "duck.jpg");
        user.setUserId(1L);

        GameRun run = new GameRun();
        run.setRunId(100L);
        run.setUser(user);

        QuizSession session = sessionWithPreparedRoom(1L, 100L, 1000L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gameRunRepository.save(any(GameRun.class))).thenReturn(run);
        when(generator.generateSkeleton(1L, 100L)).thenReturn(session);

        QuizSession created = manager.createNewRunSession(1L);

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(9999L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> manager.submitAnswer(created.getSessionId(), 1, request)
        );

        assertEquals("Es darf nur die aktuelle Frage beantwortet werden.", ex.getMessage());
    }

    @Test
    void cleanupExpiredSessions_removesOutdatedSessions()
    {
        User user = new User("jan", "HASH", "duck.jpg");
        user.setUserId(1L);

        GameRun run = new GameRun();
        run.setRunId(100L);
        run.setUser(user);

        QuizSession session = sessionWithPreparedRoom(1L, 100L, 1000L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gameRunRepository.save(any(GameRun.class))).thenReturn(run);
        when(generator.generateSkeleton(1L, 100L)).thenReturn(session);

        QuizSession created = manager.createNewRunSession(1L);

        ReflectionTestUtils.setField(created, "lastActivityAt", LocalDateTime.now().minusHours(3));

        int removed = manager.cleanupExpiredSessions();

        assertEquals(1, removed);
        assertEquals(0, manager.getActiveSessionCount());
    }

    @Test
    void removeRunSession_removesExistingMapping()
    {
        User user = new User("jan", "HASH", "duck.jpg");
        user.setUserId(1L);

        GameRun run = new GameRun();
        run.setRunId(100L);
        run.setUser(user);

        QuizSession session = sessionWithPreparedRoom(1L, 100L, 1000L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gameRunRepository.save(any(GameRun.class))).thenReturn(run);
        when(generator.generateSkeleton(1L, 100L)).thenReturn(session);

        QuizSession created = manager.createNewRunSession(1L);
        manager.removeRunSession(100L);

        assertThrows(NoSuchElementException.class, () -> manager.getSession(created.getSessionId()));
    }

    private QuizSession sessionWithPreparedRoom(Long userId, Long runId, Long questionId)
    {
        QuizSession session = new QuizSession(userId, runId);

        QuestionDto questionDto = new QuestionDto();
        questionDto.setQuestionId(questionId);
        questionDto.setQuestionType(QuestionType.MC);

        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(questionId, questionDto);

        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(questionId),
                cache,
                5
        );

        session.addRoom(room);
        return session;
    }
}