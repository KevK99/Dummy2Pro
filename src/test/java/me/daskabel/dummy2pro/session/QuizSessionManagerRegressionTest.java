package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regressionstests für bereits behobene Fehlerbilder im
 * {@link QuizSessionManager}.
 *
 * Der Schwerpunkt liegt auf dem Wiederverwenden bereits geladener Sessions
 * und auf der korrekten Auswahl des ersten noch unvollständigen Raums beim
 * Laden vorhandener Spielstände.
 */
@ExtendWith(MockitoExtension.class)
class QuizSessionManagerRegressionTest
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

    @Test
    void loadSessionForRun_shouldReuseAlreadyLoadedSession_andNotReconstructItAgain()
    {
        QuizSessionManager manager = createManager();

        User user = new User("TestUser", "hash");
        user.setUserId(42L);

        GameRun run = new GameRun(user, LocalDateTime.now());
        run.setRunId(100L);

        QuizSession skeleton = buildSkeletonSession(42L, 100L, "Raum 1", "Raum 2");

        when(gameRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(generator.generateSkeleton(42L, 100L)).thenReturn(skeleton);
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Raum 1"), new Theme("Raum 2")));
        when(questionProgressRepository.summarizeRoomProgressByRunId(100L)).thenReturn(List.of());

        QuizSession firstLoad = manager.loadSessionForRun(100L);
        QuizSession secondLoad = manager.loadSessionForRun(100L);

        assertSame(firstLoad, secondLoad);
        verify(gameRunRepository, times(1)).findById(100L);
        verify(generator, times(1)).generateSkeleton(42L, 100L);
    }

    @Test
    void loadSessionForRun_shouldPickFirstIncompleteRoom_fromStoredProgressSummary()
    {
        QuizSessionManager manager = createManager();

        User user = new User("TestUser", "hash");
        user.setUserId(42L);

        GameRun run = new GameRun(user, LocalDateTime.now());
        run.setRunId(200L);

        QuizSession skeleton = buildSkeletonSession(42L, 200L, "Raum 1", "Raum 2", "Raum 3");

        when(gameRunRepository.findById(200L)).thenReturn(Optional.of(run));
        when(generator.generateSkeleton(42L, 200L)).thenReturn(skeleton);
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Raum 1"), new Theme("Raum 2"), new Theme("Raum 3")));
        when(questionProgressRepository.summarizeRoomProgressByRunId(200L)).thenReturn(List.of(
            summary(1, 10L, 10L, 10L, 0L, 50L, 50L),
            summary(2, 10L, 4L, 3L, 1L, 50L, 15L),
            summary(3, 0L, 0L, 0L, 0L, 0L, 0L)
        ));

        QuizSession loaded = manager.loadSessionForRun(200L);

        assertEquals(2, loaded.getActiveRoomId());
    }

    private QuizSessionManager createManager()
    {
        return new QuizSessionManager(
            generator,
            questionRepository,
            gameRunRepository,
            questionProgressRepository,
            runSelectedAnswerRepository,
            runGapAnswerRepository,
            userRepository
        );
    }

    private QuizSession buildSkeletonSession(Long userId, Long runId, String... roomNames)
    {
        QuizSession session = new QuizSession(userId, runId);

        for (int i = 0; i < roomNames.length; i++)
        {
            int roomId = i + 1;
            session.addRoom(new QuizSession.RoomSession(roomId, roomNames[i], List.of(), java.util.Map.of(), 0));
        }

        return session;
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
