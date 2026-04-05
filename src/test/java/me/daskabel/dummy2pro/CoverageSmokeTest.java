package me.daskabel.dummy2pro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.daskabel.dummy2pro.controller.GameController;
import me.daskabel.dummy2pro.controller.GameProgress;
import me.daskabel.dummy2pro.dto.AnswerComparisonDto;
import me.daskabel.dummy2pro.dto.RoomDtos;
import me.daskabel.dummy2pro.dto.RunReviewDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionProgressId;
import me.daskabel.dummy2pro.model.QuestionSet;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Room;
import me.daskabel.dummy2pro.model.RunGapAnswer;
import me.daskabel.dummy2pro.model.RunGapAnswerId;
import me.daskabel.dummy2pro.model.RunSelectedAnswer;
import me.daskabel.dummy2pro.model.RunSelectedAnswerId;
import me.daskabel.dummy2pro.model.Team;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.session.QuizSessionManager;
import org.junit.jupiter.api.Test;

class CoverageSmokeTest
{
    private static final LocalDateTime SAMPLE_TIME = LocalDateTime.of(2026, 4, 5, 10, 15);

    @Test
    void beanStyleClasses_roundTripThroughAccessors() throws Exception
    {
        exerciseBean(AnswerComparisonDto.class);
        exerciseBean(RoomDtos.AnswerOptionDto.class);
        exerciseBean(RoomDtos.AnswerRequest.class);
        exerciseBean(RoomDtos.AnswerResultDto.class);
        exerciseBean(RoomDtos.DialogLineDto.class);
        exerciseBean(RoomDtos.GapAnswerEntry.class);
        exerciseBean(RoomDtos.GapFieldDto.class);
        exerciseBean(RoomDtos.GapOptionDto.class);
        exerciseBean(RoomDtos.GapResultEntry.class);
        exerciseBean(RoomDtos.QuestionDto.class);
        exerciseBean(RoomDtos.RoomStartDto.class);
        exerciseBean(RoomDtos.RoomStatusDto.class);
        exerciseBean(RunReviewDto.class);
        exerciseBean(RunReviewDto.RoomReviewDto.class);
        exerciseBean(RunReviewDto.QuestionReviewDto.class);
        exerciseBean(RunReviewDto.ChoiceReviewDto.class);
        exerciseBean(RunReviewDto.GapReviewDto.class);
        exerciseBean(GameProgress.class);
        exerciseBean(QuizSessionManager.SessionOverviewDto.class);
        exerciseBean(AnswerOption.class);
        exerciseBean(GameRun.class);
        exerciseBean(GapField.class);
        exerciseBean(GapOption.class);
        exerciseBean(Question.class);
        exerciseBean(QuestionProgress.class);
        exerciseBean(QuestionProgressId.class);
        exerciseBean(QuestionSet.class);
        exerciseBean(Room.class);
        exerciseBean(RunGapAnswer.class);
        exerciseBean(RunGapAnswerId.class);
        exerciseBean(RunSelectedAnswer.class);
        exerciseBean(RunSelectedAnswerId.class);
        exerciseBean(Team.class);
        exerciseBean(Theme.class);
        exerciseBean(User.class);
    }

    @Test
    void constructors_keepExpectedValues()
    {
        Team team = new Team("Team A");
        QuestionSet questionSet = new QuestionSet(team, "Set A");
        Theme theme = new Theme("Thema", "Beschreibung");
        User user = new User("jan", "hash", "duck.png");
        GameRun run = new GameRun(user, SAMPLE_TIME);
        run.setRunId(11L);

        Question question = new Question(questionSet, QuestionType.MC, 5);
        question.setQuestionId(22L);
        question.setStartText("Start");
        question.setEndText("Ende");
        question.setImageUrl("bild.png");
        question.setAllowsMultiple(true);
        question.setThemes(List.of(theme));

        AnswerOption answerOption = new AnswerOption(question, "Antwort", true, 2);
        answerOption.setAnswerId(33L);

        GapField gapField = new GapField(question, 1);
        gapField.setGapId(44L);
        gapField.setTextBefore("vor");
        gapField.setTextAfter("nach");

        GapOption gapOption = new GapOption(gapField, "Option", true, 3);
        gapOption.setGapOptionId(55L);
        gapField.setGapOptions(new LinkedHashSet<>(Set.of(gapOption)));
        question.setAnswerOptions(List.of(answerOption));
        question.setGapFields(new LinkedHashSet<>(Set.of(gapField)));

        QuestionProgress questionProgress = new QuestionProgress(run, question, 2, 4, ProgressStatus.CORRECT, SAMPLE_TIME);
        RunSelectedAnswer selectedAnswer = new RunSelectedAnswer(run, question, answerOption);
        RunGapAnswer gapAnswer = new RunGapAnswer(run, question, gapField, gapOption, SAMPLE_TIME);
        Room room = new Room("Raum 1", "Programmierung");
        room.setRoomId(7);
        room.setDescription("Beschreibung");
        room.setCurrentUser(user);
        room.addQuestion(question);

        assertEquals("Set A", questionSet.getTitle());
        assertEquals("Beschreibung", theme.getDescription());
        assertEquals("duck.png", user.getAvatar());
        assertEquals(SAMPLE_TIME, run.getStartedAt());
        assertEquals(QuestionType.MC, question.getQuestionType());
        assertEquals("Antwort", answerOption.getOptionText());
        assertEquals("vor", gapField.getTextBefore());
        assertEquals("Option", gapOption.getOptionText());
        assertEquals(11L, questionProgress.getId().getRunId());
        assertEquals(22L, selectedAnswer.getId().getQuestionId());
        assertEquals(44L, gapAnswer.getId().getGapId());
        assertEquals("Programmierung", room.getTheme().getName());
        assertEquals(1, room.getQuestion().size());
    }

    @Test
    void embeddedIdsAndEnums_behaveAsExpected()
    {
        QuestionProgressId questionProgressId = new QuestionProgressId(1L, 2L);
        QuestionProgressId sameQuestionProgressId = new QuestionProgressId(1L, 2L);
        RunGapAnswerId runGapAnswerId = new RunGapAnswerId(1L, 2L, 3L);
        RunGapAnswerId sameRunGapAnswerId = new RunGapAnswerId(1L, 2L, 3L);
        RunSelectedAnswerId runSelectedAnswerId = new RunSelectedAnswerId(1L, 2L, 4L);
        RunSelectedAnswerId sameRunSelectedAnswerId = new RunSelectedAnswerId(1L, 2L, 4L);

        assertEquals(questionProgressId, sameQuestionProgressId);
        assertEquals(questionProgressId.hashCode(), sameQuestionProgressId.hashCode());
        assertEquals(runGapAnswerId, sameRunGapAnswerId);
        assertEquals(runGapAnswerId.hashCode(), sameRunGapAnswerId.hashCode());
        assertEquals(runSelectedAnswerId, sameRunSelectedAnswerId);
        assertEquals(runSelectedAnswerId.hashCode(), sameRunSelectedAnswerId.hashCode());
        assertTrue(ProgressStatus.values().length > 0);
        assertTrue(QuestionType.values().length > 0);
        assertEquals("ok", new GameController.MessageResponse("ok").getMessage());
        assertEquals("BAD", new GameController.ErrorResponse("BAD", "msg").getError());
        assertEquals("msg", new GameController.ErrorResponse("BAD", "msg").getMessage());
    }

    private void exerciseBean(Class<?> type) throws Exception
    {
        Object instance = type.getDeclaredConstructor().newInstance();

        for (Method setter : type.getMethods())
        {
            if (!setter.getName().startsWith("set") || setter.getParameterCount() != 1)
            {
                continue;
            }

            Object value = sampleValue(setter.getParameterTypes()[0]);
            setter.invoke(instance, value);

            Method getter = findGetter(type, setter.getName().substring(3));
            Object actual = getter.invoke(instance);
            assertEquals(value, actual, () -> "Mismatch for " + type.getSimpleName() + "." + setter.getName());
        }
    }

    private Method findGetter(Class<?> type, String suffix) throws Exception
    {
        try
        {
            return type.getMethod("get" + suffix);
        }
        catch (NoSuchMethodException ignored)
        {
            return type.getMethod("is" + suffix);
        }
    }

    private Object sampleValue(Class<?> type) throws Exception
    {
        if (type == String.class)
        {
            return "value";
        }
        if (type == Long.class || type == long.class)
        {
            return 7L;
        }
        if (type == Integer.class || type == int.class)
        {
            return 3;
        }
        if (type == Boolean.class || type == boolean.class)
        {
            return true;
        }
        if (type == Double.class || type == double.class)
        {
            return 12.5;
        }
        if (type == LocalDateTime.class)
        {
            return SAMPLE_TIME;
        }
        if (List.class.isAssignableFrom(type))
        {
            return List.of("x");
        }
        if (Set.class.isAssignableFrom(type))
        {
            return Set.of("x");
        }
        if (Map.class.isAssignableFrom(type))
        {
            return Map.of("k", "v");
        }
        if (type.isEnum())
        {
            return type.getEnumConstants()[0];
        }
        return type.getDeclaredConstructor().newInstance();
    }
}