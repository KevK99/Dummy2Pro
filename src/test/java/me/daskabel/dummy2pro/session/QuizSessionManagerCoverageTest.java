package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.dto.RunReviewDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.RunGapAnswer;
import me.daskabel.dummy2pro.model.RunSelectedAnswer;
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
    void getRoomState_andSwitchRoom_usePreparedRoomsAndDialogs()
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

    @Test
    void getRunReview_buildsChoiceAndGapDetails()
    {
        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 5, 11, 30));
        run.setRunId(70L);

        QuizSession session = new QuizSession(7L, 70L);
        cacheSession(session);

        Question mcQuestion = new Question(QuestionType.MC, "MC Start", null, "MC Ende", false, 2);
        mcQuestion.setQuestionId(101L);
        AnswerOption mcOptionA = new AnswerOption(mcQuestion, "B", false, 2);
        mcOptionA.setAnswerId(1002L);
        AnswerOption mcOptionB = new AnswerOption(mcQuestion, "A", true, 1);
        mcOptionB.setAnswerId(1001L);
        mcQuestion.setAnswerOptions(List.of(mcOptionA, mcOptionB));

        Question gapQuestion = new Question(QuestionType.GAP, "Gap Start", null, "Gap Ende", false, 3);
        gapQuestion.setQuestionId(202L);
        GapField gapField = new GapField(gapQuestion, 0);
        gapField.setGapId(3001L);
        gapField.setTextBefore("vor");
        gapField.setTextAfter("nach");
        GapOption wrongOption = new GapOption(gapField, "falsch", false, 2);
        wrongOption.setGapOptionId(4002L);
        GapOption correctOption = new GapOption(gapField, "richtig", true, 1);
        correctOption.setGapOptionId(4001L);
        gapField.setGapOptions(new java.util.LinkedHashSet<>(List.of(wrongOption, correctOption)));
        gapQuestion.setGapFields(new java.util.LinkedHashSet<>(List.of(gapField)));

        QuestionProgress mcProgress = new QuestionProgress(
                run,
                mcQuestion,
                1,
                1,
                ProgressStatus.CORRECT,
                LocalDateTime.of(2026, 4, 5, 11, 31)
        );
        QuestionProgress gapProgress = new QuestionProgress(
                run,
                gapQuestion,
                2,
                1,
                ProgressStatus.WRONG,
                LocalDateTime.of(2026, 4, 5, 11, 32)
        );

        RunSelectedAnswer selectedAnswer = new RunSelectedAnswer(run, mcQuestion, mcOptionB);
        RunGapAnswer selectedGapAnswer = new RunGapAnswer(
                run,
                gapQuestion,
                gapField,
                wrongOption,
                LocalDateTime.of(2026, 4, 5, 11, 32)
        );

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(questionProgressRepository.findDetailedByRunIdOrderByRoomIdAscQuestionOrderAsc(70L))
                .thenReturn(List.of(mcProgress, gapProgress));
        when(questionRepository.findByQuestionIdsWithAnswers(List.of(101L))).thenReturn(List.of(mcQuestion));
        when(questionRepository.findByQuestionIdsWithGaps(List.of(202L))).thenReturn(List.of(gapQuestion));
        when(runSelectedAnswerRepository.findDetailedByRunId(70L)).thenReturn(List.of(selectedAnswer));
        when(runGapAnswerRepository.findDetailedByRunId(70L)).thenReturn(List.of(selectedGapAnswer));
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Recht"), new Theme("SQL")));

        RunReviewDto review = manager.getRunReview(session.getSessionId());

        assertEquals(2, review.getRooms().size());
        assertEquals("Recht", review.getRooms().get(0).getThemeName());
        assertEquals("MC Start MC Ende", review.getRooms().get(0).getQuestions().get(0).getQuestionText());
        assertEquals("A", review.getRooms().get(0).getQuestions().get(0).getChoices().get(0).getOptionText());
        assertEquals(true, review.getRooms().get(0).getQuestions().get(0).getChoices().get(0).isSelected());
        assertEquals("Gap Start vor _____ nach Gap Ende", review.getRooms().get(1).getQuestions().get(0).getQuestionText());
        assertEquals("vor _____ nach", review.getRooms().get(1).getQuestions().get(0).getGaps().get(0).getLabel());
        assertEquals("falsch", review.getRooms().get(1).getQuestions().get(0).getGaps().get(0).getSelectedText());
        assertEquals("richtig", review.getRooms().get(1).getQuestions().get(0).getGaps().get(0).getCorrectText());
        assertFalse(review.getRooms().get(1).getQuestions().get(0).getGaps().get(0).isCorrect());
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