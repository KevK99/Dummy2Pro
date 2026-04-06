package me.daskabel.dummy2pro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

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

class PojoCoverageTest
{
    private static final LocalDateTime SAMPLE_TIME = LocalDateTime.of(2026, 4, 6, 9, 30);

    @Test
    void roomDtos_roundTripThroughAccessors()
    {
        RoomDtos.AnswerOptionDto answerOptionDto = new RoomDtos.AnswerOptionDto();
        answerOptionDto.setAnswerId(1L);
        answerOptionDto.setOptionText("A");
        answerOptionDto.setOptionOrder(2);
        answerOptionDto.setCorrectAnswers(3);
        answerOptionDto.setWrongAnswers(4);

        RoomDtos.GapOptionDto gapOptionDto = new RoomDtos.GapOptionDto();
        gapOptionDto.setGapOptionId(10L);
        gapOptionDto.setOptionText("Option");
        gapOptionDto.setOptionOrder(1);

        RoomDtos.GapFieldDto gapFieldDto = new RoomDtos.GapFieldDto();
        gapFieldDto.setGapId(20L);
        gapFieldDto.setGapIndex(0);
        gapFieldDto.setTextBefore("vor");
        gapFieldDto.setTextAfter("nach");
        gapFieldDto.setGapOptions(List.of(gapOptionDto));

        RoomDtos.QuestionDto questionDto = new RoomDtos.QuestionDto();
        questionDto.setQuestionId(30L);
        questionDto.setQuestionType(QuestionType.GAP);
        questionDto.setStartText("Start");
        questionDto.setImageUrl("bild.png");
        questionDto.setEndText("Ende");
        questionDto.setAllowsMultiple(true);
        questionDto.setPoints(5);
        questionDto.setAnswerOptions(List.of(answerOptionDto));
        questionDto.setGapFields(List.of(gapFieldDto));
        questionDto.setCurrentIndex(1);
        questionDto.setTotalCount(7);

        RoomDtos.GapAnswerEntry gapAnswerEntry = new RoomDtos.GapAnswerEntry();
        gapAnswerEntry.setGapId(40L);
        gapAnswerEntry.setSelectedGapOptionId(41L);

        RoomDtos.AnswerRequest answerRequest = new RoomDtos.AnswerRequest();
        answerRequest.setQuestionId(50L);
        answerRequest.setSelectedAnswerIds(List.of(1L, 2L));
        answerRequest.setGapAnswers(List.of(gapAnswerEntry));

        RoomDtos.GapResultEntry gapResultEntry = new RoomDtos.GapResultEntry();
        gapResultEntry.setGapId(60L);
        gapResultEntry.setCorrect(true);
        gapResultEntry.setCorrectGapOptionId(61L);
        gapResultEntry.setCorrectOptionText("richtig");

        RoomDtos.AnswerResultDto answerResultDto = new RoomDtos.AnswerResultDto();
        answerResultDto.setCorrect(true);
        answerResultDto.setCorrectAnswerIds(List.of(70L));
        answerResultDto.setGapResults(List.of(gapResultEntry));
        answerResultDto.setPointsEarned(8);
        answerResultDto.setExplanation("ok");

        RoomDtos.DialogLineDto dialogLineDto = new RoomDtos.DialogLineDto("npc", "Hallo");

        AnswerComparisonDto answerComparisonDto = new AnswerComparisonDto();
        answerComparisonDto.setQuestionId(71L);
        answerComparisonDto.setSelectedAnswerIds(List.of(1L));
        answerComparisonDto.setCorrectAnswerIds(List.of(2L));

        RoomDtos.RoomStatusDto roomStatusDto = new RoomDtos.RoomStatusDto();
        roomStatusDto.setRoomId(2);
        roomStatusDto.setThemeName("SQL");
        roomStatusDto.setTotalQuestions(10);
        roomStatusDto.setAnsweredQuestions(8);
        roomStatusDto.setCorrectAnswers(6);
        roomStatusDto.setWrongAnswers(2);
        roomStatusDto.setOpenQuestions(2);
        roomStatusDto.setTotalPoints(20);
        roomStatusDto.setEarnedPoints(12);
        roomStatusDto.setCompletionPercent(80.0);
        roomStatusDto.setMedal("SILVER");
        roomStatusDto.setAnswerComparisons(List.of(answerComparisonDto));

        RoomDtos.RoomStartDto roomStartDto = new RoomDtos.RoomStartDto();
        roomStartDto.setStatus(roomStatusDto);
        roomStartDto.setFirstQuestion(questionDto);
        roomStartDto.setQuestionSequence(List.of(1L, 2L, 3L));
        roomStartDto.setIntroDialog(List.of(dialogLineDto));

        assertEquals(1L, answerOptionDto.getAnswerId());
        assertEquals(4, answerOptionDto.getWrongAnswers());
        assertEquals(20L, gapFieldDto.getGapId());
        assertEquals("Option", gapFieldDto.getGapOptions().get(0).getOptionText());
        assertEquals(QuestionType.GAP, questionDto.getQuestionType());
        assertTrue(questionDto.getAllowsMultiple());
        assertEquals(41L, answerRequest.getGapAnswers().get(0).getSelectedGapOptionId());
        assertTrue(answerResultDto.isCorrect());
        assertEquals("Hallo", dialogLineDto.getText());
        assertEquals(71L, roomStatusDto.getAnswerComparisons().get(0).getQuestionId());
        assertEquals("SILVER", roomStatusDto.getMedal());
        assertSame(roomStatusDto, roomStartDto.getStatus());
        assertEquals(3, roomStartDto.getQuestionSequence().size());
    }

    @Test
    void runReviewDtos_roundTripThroughAccessors()
    {
        RunReviewDto.ChoiceReviewDto choiceReviewDto = new RunReviewDto.ChoiceReviewDto();
        choiceReviewDto.setAnswerId(1L);
        choiceReviewDto.setOptionText("Antwort");
        choiceReviewDto.setSelected(true);
        choiceReviewDto.setCorrect(false);

        RunReviewDto.GapReviewDto gapReviewDto = new RunReviewDto.GapReviewDto();
        gapReviewDto.setGapId(2L);
        gapReviewDto.setGapIndex(3);
        gapReviewDto.setLabel("vor _____ nach");
        gapReviewDto.setSelectedText("falsch");
        gapReviewDto.setCorrectText("richtig");
        gapReviewDto.setCorrect(false);

        RunReviewDto.QuestionReviewDto questionReviewDto = new RunReviewDto.QuestionReviewDto();
        questionReviewDto.setQuestionId(4L);
        questionReviewDto.setQuestionOrder(5);
        questionReviewDto.setQuestionType("MC");
        questionReviewDto.setQuestionText("Frage");
        questionReviewDto.setImageUrl("bild.png");
        questionReviewDto.setPoints(6);
        questionReviewDto.setStatus("CORRECT");
        questionReviewDto.setAnsweredAt(SAMPLE_TIME);
        questionReviewDto.setChoices(List.of(choiceReviewDto));
        questionReviewDto.setGaps(List.of(gapReviewDto));

        RunReviewDto.RoomReviewDto roomReviewDto = new RunReviewDto.RoomReviewDto();
        roomReviewDto.setRoomId(7);
        roomReviewDto.setThemeName("Programmierung");
        roomReviewDto.setMedal("GOLD");
        roomReviewDto.setTotalQuestions(8);
        roomReviewDto.setCorrectAnswers(6);
        roomReviewDto.setWrongAnswers(1);
        roomReviewDto.setOpenQuestions(1);
        roomReviewDto.setQuestions(List.of(questionReviewDto));

        RunReviewDto runReviewDto = new RunReviewDto();
        runReviewDto.setRunId(9L);
        runReviewDto.setUsername("jan");
        runReviewDto.setRooms(List.of(roomReviewDto));

        assertTrue(choiceReviewDto.isSelected());
        assertFalse(gapReviewDto.isCorrect());
        assertEquals(SAMPLE_TIME, questionReviewDto.getAnsweredAt());
        assertEquals("Programmierung", roomReviewDto.getThemeName());
        assertEquals(9L, runReviewDto.getRunId());
        assertEquals("jan", runReviewDto.getUsername());
    }

    @Test
    void modelConstructors_ids_andSimpleAccessors_workAsExpected()
    {
        Team team = new Team("Team A");
        team.setTeamId(1L);

        QuestionSet questionSet = new QuestionSet(team, "Set A");
        questionSet.setQuestionSetId(2L);

        Theme theme = new Theme("Thema", "Beschreibung");
        theme.setThemeId(3L);

        User user = new User("jan", "hash", "duck.png");
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

        GameProgress gameProgress = new GameProgress();
        gameProgress.setRunId(12L);
        gameProgress.setCurrentRoom(room);

        QuestionProgressId questionProgressId = new QuestionProgressId(5L, 7L);
        RunGapAnswerId runGapAnswerId = new RunGapAnswerId(5L, 7L, 9L);
        RunSelectedAnswerId runSelectedAnswerId = new RunSelectedAnswerId(5L, 7L, 8L);

        QuizSessionManager.SessionOverviewDto sessionOverviewDto = new QuizSessionManager.SessionOverviewDto();
        sessionOverviewDto.setSessionId("abc");
        sessionOverviewDto.setUsername("jan");
        sessionOverviewDto.setTotalEarnedPoints(13);
        sessionOverviewDto.setTotalMaxPoints(20);
        sessionOverviewDto.setTotalCorrect(3);
        sessionOverviewDto.setTotalWrong(1);
        sessionOverviewDto.setFullyCompleted(false);

        assertEquals("Team A", team.getName());
        assertEquals("Set A", questionSet.getTitle());
        assertEquals("Beschreibung", theme.getDescription());
        assertEquals("duck.png", user.getAvatar());
        assertEquals("Run 1", run.getDisplayName());
        assertEquals(QuestionType.MC, question.getQuestionType());
        assertTrue(answerOption.getIsCorrect());
        assertEquals("vor", gapField.getTextBefore());
        assertEquals(10L, gapField.getGapOptions().iterator().next().getGapOptionId());
        assertEquals(5L, questionProgress.getId().getRunId());
        assertEquals(8L, selectedAnswer.getId().getAnswerId());
        assertEquals(9L, gapAnswer.getId().getGapId());
        assertEquals("Programmierung", room.getTheme().getName());
        assertEquals(1, room.getQuestion().size());
        assertSame(room, gameProgress.getCurrentRoom());
        assertEquals(new QuestionProgressId(5L, 7L), questionProgressId);
        assertEquals(new RunGapAnswerId(5L, 7L, 9L), runGapAnswerId);
        assertEquals(new RunSelectedAnswerId(5L, 7L, 8L), runSelectedAnswerId);
        assertEquals("abc", sessionOverviewDto.getSessionId());
        assertEquals("jan", sessionOverviewDto.getUsername());
        assertFalse(sessionOverviewDto.isFullyCompleted());
        assertEquals("ok", new GameController.MessageResponse("ok").getMessage());
        assertEquals("BAD", new GameController.ErrorResponse("BAD", "msg").getError());
    }
}