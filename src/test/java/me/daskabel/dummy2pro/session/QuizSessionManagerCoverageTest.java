package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.dto.RunReviewDto;
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
class QuizSessionManagerCoverageTest
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
    void getOverview_aggregatesDatabaseValuesAndFallbackRooms()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(new QuizSession.RoomSession(1, "Recht", List.of(), Map.of(), 0));
        session.addRoom(new QuizSession.RoomSession(2, "SQL", List.of(), Map.of(), 0));
        cacheSession(session);

        User user = new User("jan", "hash");
        user.setUserId(7L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Recht"), new Theme("SQL")));
        when(questionProgressRepository.summarizeRoomProgressByRunId(70L)).thenReturn(List.of(
                summary(1, 4L, 4L, 3L, 1L, 10L, 7L)
        ));

        QuizSessionManager.SessionOverviewDto overview = manager.getOverview(session.getSessionId());

        assertEquals(session.getSessionId(), overview.getSessionId());
        assertEquals("jan", overview.getUsername());
        assertEquals(7, overview.getTotalEarnedPoints());
        assertEquals(10, overview.getTotalMaxPoints());
        assertEquals(3, overview.getTotalCorrect());
        assertEquals(1, overview.getTotalWrong());
        assertFalse(overview.isFullyCompleted());
        assertEquals(2, overview.getRooms().size());
        assertEquals("SILVER", overview.getRooms().get(0).getMedal());
        assertEquals(0, overview.getRooms().get(1).getTotalQuestions());
    }

    @Test
    void getRoomState_switchRoom_andGetRoomStatus_usePreparedRooms()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(preparedRoom(1, 101L));
        session.addRoom(preparedRoom(2, 202L));
        cacheSession(session);

        RoomStartDto roomState = manager.getRoomState(session.getSessionId(), 1);
        RoomStartDto switchedRoom = manager.switchRoom(session.getSessionId(), 2);
        RoomStatusDto roomStatus = manager.getRoomStatus(session.getSessionId(), 2);

        assertEquals(101L, roomState.getFirstQuestion().getQuestionId());
        assertFalse(roomState.getIntroDialog().isEmpty());
        assertEquals(2, session.getActiveRoomId());
        assertEquals(202L, switchedRoom.getFirstQuestion().getQuestionId());
        assertEquals(2, roomStatus.getRoomId());
    }

    @Test
    void advance_returnsNull_whenPreparedRoomIsAlreadyCompleted()
    {
        QuizSession.RoomSession room = preparedRoom(1, 101L);
        room.setCompleted(true);

        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(room);
        cacheSession(session);

        assertNull(manager.advance(session.getSessionId(), 1));
    }

    @Test
    void getRunReview_returnsEmptyRoomsWhenNoProgressExists()
    {
        QuizSession session = new QuizSession(7L, 70L);
        cacheSession(session);

        when(userRepository.findById(7L)).thenReturn(Optional.empty());
        when(questionProgressRepository.findDetailedByRunIdOrderByRoomIdAscQuestionOrderAsc(70L)).thenReturn(List.of());

        RunReviewDto review = manager.getRunReview(session.getSessionId());

        assertEquals(70L, review.getRunId());
        assertEquals("Unbekannt", review.getUsername());
        assertEquals(List.of(), review.getRooms());
    }

    private QuizSession.RoomSession preparedRoom(int roomId, Long questionId)
    {
        QuestionDto questionDto = new QuestionDto();
        questionDto.setQuestionId(questionId);
        questionDto.setQuestionType(QuestionType.MC);

        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(questionId, questionDto);

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