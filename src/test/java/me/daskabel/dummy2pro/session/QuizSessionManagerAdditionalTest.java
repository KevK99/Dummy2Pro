package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionProgress;
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
class QuizSessionManagerAdditionalTest
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
    void loadSessionForRun_shouldUseLastRoom_whenAllRoomsCompleted()
    {
        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.now());
        run.setRunId(70L);

        QuizSession skeleton = new QuizSession(7L, 70L);
        skeleton.addRoom(new QuizSession.RoomSession(1, "Recht", List.of(), Map.of(), 0));
        skeleton.addRoom(new QuizSession.RoomSession(2, "SQL", List.of(), Map.of(), 0));
        skeleton.addRoom(new QuizSession.RoomSession(3, "UML", List.of(), Map.of(), 0));

        when(gameRunRepository.findById(70L)).thenReturn(Optional.of(run));
        when(generator.generateSkeleton(7L, 70L)).thenReturn(skeleton);
        when(generator.getThemesOrdered()).thenReturn(List.of(
                new Theme("Recht"),
                new Theme("SQL"),
                new Theme("UML")
        ));
        when(questionProgressRepository.summarizeRoomProgressByRunId(70L)).thenReturn(List.of(
                summary(1, 3L, 3L, 3L, 0L, 15L, 15L),
                summary(2, 2L, 2L, 1L, 1L, 10L, 5L),
                summary(3, 4L, 4L, 4L, 0L, 20L, 20L)
        ));

        QuizSession loaded = manager.loadSessionForRun(70L);

        assertEquals(3, loaded.getActiveRoomId());
    }

    @Test
    void prepareRoom_shouldRestoreOpenQuestionFromStoredProgress()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(new QuizSession.RoomSession(1, "Recht", List.of(), Map.of(), 0));
        cacheSession(session);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 10, 0));
        run.setRunId(70L);

        Question question1 = question(101L, 5);
        Question question2 = question(102L, 3);

        QuestionProgress progress1 = new QuestionProgress(
                run,
                question1,
                1,
                1,
                ProgressStatus.CORRECT,
                LocalDateTime.of(2026, 4, 6, 10, 1)
        );
        QuestionProgress progress2 = new QuestionProgress(
                run,
                question2,
                1,
                2,
                ProgressStatus.OPEN,
                null
        );

        when(questionProgressRepository.findByRunIdAndRoomIdOrderByQuestionOrder(70L, 1))
                .thenReturn(List.of(progress1, progress2));
        when(generator.loadQuestionsByIdsOrdered(List.of(101L, 102L)))
                .thenReturn(List.of(question1, question2));
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Recht")));

        RoomStartDto roomState = manager.getRoomState(session.getSessionId(), 1);

        assertNotNull(roomState.getFirstQuestion());
        assertEquals(102L, roomState.getFirstQuestion().getQuestionId());
        assertEquals(1, roomState.getStatus().getAnsweredQuestions());
        assertEquals(1, roomState.getStatus().getCorrectAnswers());
        assertEquals(0, roomState.getStatus().getWrongAnswers());
        assertEquals(1, roomState.getStatus().getOpenQuestions());
        assertEquals(5, roomState.getStatus().getEarnedPoints());
        assertFalse(roomState.getStatus().getCompletionPercent() == 100.0);
    }

    @Test
    void getRoomState_shouldReturnNullFirstQuestion_whenRestoredRoomIsAlreadyCompleted()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(new QuizSession.RoomSession(1, "Recht", List.of(), Map.of(), 0));
        cacheSession(session);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 10, 0));
        run.setRunId(70L);

        Question question1 = question(101L, 5);
        Question question2 = question(102L, 3);

        QuestionProgress progress1 = new QuestionProgress(
                run,
                question1,
                1,
                1,
                ProgressStatus.CORRECT,
                LocalDateTime.of(2026, 4, 6, 10, 1)
        );
        QuestionProgress progress2 = new QuestionProgress(
                run,
                question2,
                1,
                2,
                ProgressStatus.WRONG,
                LocalDateTime.of(2026, 4, 6, 10, 2)
        );

        when(questionProgressRepository.findByRunIdAndRoomIdOrderByQuestionOrder(70L, 1))
                .thenReturn(List.of(progress1, progress2));
        when(generator.loadQuestionsByIdsOrdered(List.of(101L, 102L)))
                .thenReturn(List.of(question1, question2));
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Recht")));

        RoomStartDto roomState = manager.getRoomState(session.getSessionId(), 1);

        assertNull(roomState.getFirstQuestion());
        assertEquals(2, roomState.getStatus().getAnsweredQuestions());
        assertEquals(1, roomState.getStatus().getCorrectAnswers());
        assertEquals(1, roomState.getStatus().getWrongAnswers());
        assertEquals(0, roomState.getStatus().getOpenQuestions());
        assertEquals("BRONZE", roomState.getStatus().getMedal());
    }

    @Test
    void advance_shouldReturnNextQuestion_whenCurrentQuestionWasAlreadyAnswered()
    {
        QuizSession session = new QuizSession(7L, 70L);

        QuestionDto question1 = questionDto(101L);
        QuestionDto question2 = questionDto(102L);

        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(101L, question1);
        cache.put(102L, question2);

        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Recht",
                List.of(101L, 102L),
                cache,
                8
        );
        room.recordResult(101L, true, 5);

        session.addRoom(room);
        cacheSession(session);

        QuestionDto next = manager.advance(session.getSessionId(), 1);

        assertNotNull(next);
        assertEquals(102L, next.getQuestionId());
    }

    @Test
    void submitAnswer_shouldRejectAlreadyAnsweredCurrentQuestion()
    {
        QuizSession session = new QuizSession(7L, 70L);

        QuestionDto question = questionDto(101L);

        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(101L, question);

        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Recht",
                List.of(101L),
                cache,
                5
        );
        room.recordResult(101L, true, 5);

        session.addRoom(room);
        cacheSession(session);

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(101L);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> manager.submitAnswer(session.getSessionId(), 1, request)
        );

        assertEquals("Diese Frage wurde bereits beantwortet.", ex.getMessage());
    }

    @Test
    void loadSessionForRun_shouldThrow_whenRunDoesNotExist()
    {
        when(gameRunRepository.findById(999L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> manager.loadSessionForRun(999L)
        );

        assertEquals("Run 999 nicht gefunden.", ex.getMessage());
    }

    private Question question(Long questionId, int points)
    {
        Question question = new Question(QuestionType.MC, "Frage", null, null, false, points);
        question.setQuestionId(questionId);
        return question;
    }

    private QuestionDto questionDto(Long questionId)
    {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(questionId);
        dto.setQuestionType(QuestionType.MC);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private void cacheSession(QuizSession session)
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