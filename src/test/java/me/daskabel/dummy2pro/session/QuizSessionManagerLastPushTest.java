package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
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

/**
 * Unittests für späte Hilfs- und Fehlerpfade im {@link QuizSessionManager}.
 *
 * Die Tests prüfen den Aufbau der Review-Struktur, Reflection-basierte
 * Fehlerfälle beim Rekonstruieren von Räumen sowie Schutzlogik beim
 * Weiterblättern und beim Erzeugen neuer Sitzungen.
 */
@ExtendWith(MockitoExtension.class)
class QuizSessionManagerLastPushTest
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
    void getRunReview_shouldBuildChoiceGapAndFallbackRoomName()
    {
        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 18, 0));
        run.setRunId(70L);

        QuizSession session = new QuizSession(7L, 70L);
        cacheSession(session);

        Question mcQuestion = new Question(QuestionType.MC, "MC Start", null, "MC Ende", false, 2);
        mcQuestion.setQuestionId(101L);

        AnswerOption wrongMc = new AnswerOption(mcQuestion, "B", false, 2);
        wrongMc.setAnswerId(1002L);
        AnswerOption correctMc = new AnswerOption(mcQuestion, "A", true, 1);
        correctMc.setAnswerId(1001L);
        mcQuestion.setAnswerOptions(List.of(wrongMc, correctMc));

        Question gapQuestion = new Question(QuestionType.GAP, "Gap Start", null, "Gap Ende", false, 3);
        gapQuestion.setQuestionId(202L);

        GapField gapField = new GapField(gapQuestion, 0);
        gapField.setGapId(3001L);
        gapField.setTextBefore("vor");
        gapField.setTextAfter("nach");

        GapOption wrongGap = new GapOption(gapField, "falsch", false, 2);
        wrongGap.setGapOptionId(4002L);
        GapOption correctGap = new GapOption(gapField, "richtig", true, 1);
        correctGap.setGapOptionId(4001L);
        gapField.setGapOptions(new java.util.LinkedHashSet<>(List.of(wrongGap, correctGap)));
        gapQuestion.setGapFields(new java.util.LinkedHashSet<>(List.of(gapField)));

        QuestionProgress mcProgress = new QuestionProgress(
                run,
                mcQuestion,
                1,
                1,
                ProgressStatus.CORRECT,
                LocalDateTime.of(2026, 4, 6, 18, 1)
        );
        QuestionProgress gapProgress = new QuestionProgress(
                run,
                gapQuestion,
                3,
                1,
                ProgressStatus.WRONG,
                LocalDateTime.of(2026, 4, 6, 18, 2)
        );

        RunSelectedAnswer selectedMc = new RunSelectedAnswer(run, mcQuestion, correctMc);
        RunGapAnswer selectedGap = new RunGapAnswer(
                run,
                gapQuestion,
                gapField,
                wrongGap,
                LocalDateTime.of(2026, 4, 6, 18, 2)
        );

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(questionProgressRepository.findDetailedByRunIdOrderByRoomIdAscQuestionOrderAsc(70L))
                .thenReturn(List.of(mcProgress, gapProgress));
        when(questionRepository.findByQuestionIdsWithAnswers(List.of(101L))).thenReturn(List.of(mcQuestion));
        when(questionRepository.findByQuestionIdsWithGaps(List.of(202L))).thenReturn(List.of(gapQuestion));
        when(runSelectedAnswerRepository.findDetailedByRunId(70L)).thenReturn(List.of(selectedMc));
        when(runGapAnswerRepository.findDetailedByRunId(70L)).thenReturn(List.of(selectedGap));
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Recht"), new Theme("SQL")));

        RunReviewDto review = manager.getRunReview(session.getSessionId());

        assertEquals(2, review.getRooms().size());

        assertEquals("Recht", review.getRooms().get(0).getThemeName());
        assertEquals("MC Start MC Ende", review.getRooms().get(0).getQuestions().get(0).getQuestionText());
        assertEquals("A", review.getRooms().get(0).getQuestions().get(0).getChoices().get(0).getOptionText());
        assertEquals(true, review.getRooms().get(0).getQuestions().get(0).getChoices().get(0).isSelected());

        assertEquals("Raum 3", review.getRooms().get(1).getThemeName());
        assertEquals("Gap Start vor _____ nach Gap Ende", review.getRooms().get(1).getQuestions().get(0).getQuestionText());
        assertEquals("vor _____ nach", review.getRooms().get(1).getQuestions().get(0).getGaps().get(0).getLabel());
        assertEquals("falsch", review.getRooms().get(1).getQuestions().get(0).getGaps().get(0).getSelectedText());
        assertEquals("richtig", review.getRooms().get(1).getQuestions().get(0).getGaps().get(0).getCorrectText());
        assertEquals(false, review.getRooms().get(1).getQuestions().get(0).getGaps().get(0).isCorrect());
    }

    @Test
    void buildRoomSessionFromProgressEntries_shouldThrow_whenEntriesAreEmpty() throws Exception
    {
        Method method = QuizSessionManager.class.getDeclaredMethod(
                "buildRoomSessionFromProgressEntries",
                int.class,
                List.class
        );
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(manager, 1, List.of())
        );

        assertEquals(IllegalArgumentException.class, ex.getCause().getClass());
        assertEquals("roomProgressEntries darf nicht leer sein.", ex.getCause().getMessage());
    }

    @Test
    void buildRoomSessionFromProgressEntries_shouldThrow_whenLoadedQuestionIsMissing() throws Exception
    {
        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun run = new GameRun(user, LocalDateTime.of(2026, 4, 6, 18, 30));
        run.setRunId(70L);

        Question question = new Question(QuestionType.MC, "Frage", null, null, false, 5);
        question.setQuestionId(999L);

        QuestionProgress progress = new QuestionProgress(
                run,
                question,
                1,
                1,
                ProgressStatus.CORRECT,
                LocalDateTime.of(2026, 4, 6, 18, 31)
        );

        when(generator.loadQuestionsByIdsOrdered(List.of(999L))).thenReturn(List.of());
        when(generator.getThemesOrdered()).thenReturn(List.of(new Theme("Recht")));

        Method method = QuizSessionManager.class.getDeclaredMethod(
                "buildRoomSessionFromProgressEntries",
                int.class,
                List.class
        );
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(manager, 1, List.of(progress))
        );

        assertEquals(NoSuchElementException.class, ex.getCause().getClass());
        assertEquals("Frage 999 nicht gefunden.", ex.getCause().getMessage());
    }

    @Test
    void createSession_shouldDelegateToCreateNewRunSession()
    {
        User user = new User("jan", "hash");
        user.setUserId(7L);

        GameRun savedRun = new GameRun(user, LocalDateTime.now());
        savedRun.setRunId(70L);

        QuizSession skeleton = new QuizSession(7L, 70L);
        skeleton.addRoom(preparedRoom(1, 101L));

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(gameRunRepository.save(org.mockito.ArgumentMatchers.any(GameRun.class))).thenReturn(savedRun);
        when(generator.generateSkeleton(7L, 70L)).thenReturn(skeleton);

        QuizSession session = manager.createSession(7L);

        assertNotNull(session);
        assertEquals(7L, session.getUserId());
        assertEquals(70L, session.getRunId());
        assertEquals(1, manager.getActiveSessionCount());
    }

    @Test
    void advance_shouldThrow_whenCurrentQuestionIsStillUnanswered()
    {
        QuizSession session = new QuizSession(7L, 70L);
        session.addRoom(preparedRoom(1, 101L, 102L));
        cacheSession(session);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> manager.advance(session.getSessionId(), 1)
        );

        assertEquals("Die aktuelle Frage muss zuerst beantwortet werden.", ex.getMessage());
    }

    private QuizSession.RoomSession preparedRoom(int roomId, Long... questionIds)
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

    @SuppressWarnings("unchecked")
    private void cacheSession(QuizSession session)
    {
        ((Map<String, QuizSession>) ReflectionTestUtils.getField(manager, "sessions"))
                .put(session.getSessionId(), session);
        ((Map<Long, String>) ReflectionTestUtils.getField(manager, "runSessionMap"))
                .put(session.getRunId(), session.getSessionId());
    }
}