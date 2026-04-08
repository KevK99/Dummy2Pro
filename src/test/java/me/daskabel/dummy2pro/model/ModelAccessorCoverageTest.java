package me.daskabel.dummy2pro.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ModelAccessorCoverageTest
{
    private static final LocalDateTime SAMPLE_TIME = LocalDateTime.of(2026, 4, 6, 10, 30);

    @Test
    void accessors_shouldRoundTripAcrossModelClasses() throws Exception
    {
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
    void constructors_and_ids_shouldKeepExpectedValues()
    {
        Team team = new Team("Team A");
        team.setTeamId(1L);

        QuestionSet questionSet = new QuestionSet(team, "Set A");
        questionSet.setQuestionSetId(2L);

        Theme theme = new Theme("Thema", "Beschreibung");
        theme.setThemeId(3L);

        User user = new User("jan", "hash", "duck.jpg");
        user.setUserId(4L);

        GameRun run = new GameRun(user, SAMPLE_TIME);
        run.setRunId(5L);
        run.setDisplayName("Run 1");

        Question question = new Question(questionSet, QuestionType.MC, 6);
        question.setQuestionId(7L);
        question.setStartText("Start");
        question.setImageUrl("bild.png");
        question.setEndText("Ende");
        question.setAllowsMultiple(true);
        question.setThemes(List.of(theme));

        AnswerOption answerOption = new AnswerOption(question, "Antwort", true, 1);
        answerOption.setAnswerId(8L);

        GapField gapField = new GapField(question, 0);
        gapField.setGapId(9L);
        gapField.setTextBefore("vor");
        gapField.setTextAfter("nach");

        GapOption gapOption = new GapOption(gapField, "Option", true, 2);
        gapOption.setGapOptionId(10L);
        gapField.setGapOptions(new LinkedHashSet<>(List.of(gapOption)));

        QuestionProgress questionProgress = new QuestionProgress(run, question, 1, 2, ProgressStatus.CORRECT, SAMPLE_TIME);
        RunSelectedAnswer selectedAnswer = new RunSelectedAnswer(run, question, answerOption);
        RunGapAnswer gapAnswer = new RunGapAnswer(run, question, gapField, gapOption, SAMPLE_TIME);

        Room room = new Room("Raum 1", "Programmierung");
        room.setRoomId(11);
        room.setDescription("Beschreibung");
        room.setCurrentUser(user);
        room.addQuestion(question);

        assertEquals("Team A", team.getName());
        assertEquals("Set A", questionSet.getTitle());
        assertEquals("Beschreibung", theme.getDescription());
        assertEquals("duck.jpg", user.getAvatar());
        assertEquals("Run 1", run.getDisplayName());
        assertEquals(QuestionType.MC, question.getQuestionType());
        assertEquals("Antwort", answerOption.getOptionText());
        assertEquals("vor", gapField.getTextBefore());
        assertEquals(10L, gapField.getGapOptions().iterator().next().getGapOptionId());
        assertEquals(5L, questionProgress.getId().getRunId());
        assertEquals(8L, selectedAnswer.getId().getAnswerId());
        assertEquals(9L, gapAnswer.getId().getGapId());
        assertEquals("Programmierung", room.getTheme().getName());
        assertEquals(1, room.getQuestion().size());
    }

    @Test
    void embeddedIds_shouldSupportEqualsAndHashCode()
    {
        QuestionProgressId questionProgressId1 = new QuestionProgressId(1L, 2L);
        QuestionProgressId questionProgressId2 = new QuestionProgressId(1L, 2L);

        RunGapAnswerId runGapAnswerId1 = new RunGapAnswerId(1L, 2L, 3L);
        RunGapAnswerId runGapAnswerId2 = new RunGapAnswerId(1L, 2L, 3L);

        RunSelectedAnswerId runSelectedAnswerId1 = new RunSelectedAnswerId(1L, 2L, 4L);
        RunSelectedAnswerId runSelectedAnswerId2 = new RunSelectedAnswerId(1L, 2L, 4L);

        assertEquals(questionProgressId1, questionProgressId2);
        assertEquals(questionProgressId1.hashCode(), questionProgressId2.hashCode());

        assertEquals(runGapAnswerId1, runGapAnswerId2);
        assertEquals(runGapAnswerId1.hashCode(), runGapAnswerId2.hashCode());

        assertEquals(runSelectedAnswerId1, runSelectedAnswerId2);
        assertEquals(runSelectedAnswerId1.hashCode(), runSelectedAnswerId2.hashCode());
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
        if (type == LocalDateTime.class)
        {
            return SAMPLE_TIME;
        }
        if (List.class.isAssignableFrom(type))
        {
            return List.of();
        }
        if (Set.class.isAssignableFrom(type))
        {
            return new LinkedHashSet<>();
        }
        if (type.isEnum())
        {
            return type.getEnumConstants()[0];
        }

        Object nested = type.getDeclaredConstructor().newInstance();
        assertNotNull(nested);
        return nested;
    }

    @Test
    void embeddedIds_shouldCoverEqualsBranches_forSelfNullOtherTypeAndDifferentValues()
    {
        QuestionProgressId questionProgressId = new QuestionProgressId(1L, 2L);
        assertTrue(questionProgressId.equals(questionProgressId));
        assertFalse(questionProgressId.equals(null));
        assertFalse(questionProgressId.equals("x"));
        assertNotEquals(questionProgressId, new QuestionProgressId(1L, 99L));

        RunGapAnswerId runGapAnswerId = new RunGapAnswerId(1L, 2L, 3L);
        assertTrue(runGapAnswerId.equals(runGapAnswerId));
        assertFalse(runGapAnswerId.equals(null));
        assertFalse(runGapAnswerId.equals("x"));
        assertNotEquals(runGapAnswerId, new RunGapAnswerId(1L, 2L, 99L));

        RunSelectedAnswerId runSelectedAnswerId = new RunSelectedAnswerId(1L, 2L, 4L);
        assertTrue(runSelectedAnswerId.equals(runSelectedAnswerId));
        assertFalse(runSelectedAnswerId.equals(null));
        assertFalse(runSelectedAnswerId.equals("x"));
        assertNotEquals(runSelectedAnswerId, new RunSelectedAnswerId(1L, 2L, 99L));
    }

    @Test
    void roomConstructor_shouldUseProvidedQuestionList_orCreateEmptyListWhenNull()
    {
        Theme theme = new Theme("Thema");
        User user = new User("jan", "hash");
        Question question = new Question(QuestionType.MC, "Start", null, null, false, 3);

        List<Question> providedQuestions = new java.util.ArrayList<>(List.of(question));
        Room roomWithProvidedList = new Room(theme, "Raum A", "Beschreibung", user, providedQuestions);

        Room roomWithNullQuestions = new Room(theme, "Raum B", "Beschreibung", user, null);

        assertSame(providedQuestions, roomWithProvidedList.getQuestion());
        assertEquals(1, roomWithProvidedList.getQuestion().size());

        assertNotNull(roomWithNullQuestions.getQuestion());
        assertEquals(0, roomWithNullQuestions.getQuestion().size());
    }
}