package me.daskabel.dummy2pro.integration;

import jakarta.persistence.EntityManager;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionSet;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Team;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoomServiceIntegrationTest
{
    @Autowired
    private RoomService roomService;

    @Autowired
    private QuestionProgressRepository questionProgressRepository;

    @Autowired
    private RunSelectedAnswerRepository runSelectedAnswerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void submitAnswer_mcCorrect_persistsSelectedAnswerAndUpdatesRoomStatus()
    {
        TestData data = createMcData();

        AnswerRequest request = new AnswerRequest();
        request.setQuestionId(data.question.getQuestionId());
        request.setSelectedAnswerIds(List.of(data.correctAnswer.getAnswerId()));

        AnswerResultDto result = roomService.submitAnswer(1, data.run.getRunId(), request);

        entityManager.flush();
        entityManager.clear();

        assertTrue(result.isCorrect());
        assertEquals(5, result.getPointsEarned());

        QuestionProgress progress = questionProgressRepository
                .findByRun_RunIdAndQuestion_QuestionId(data.run.getRunId(), data.question.getQuestionId())
                .orElseThrow();

        assertEquals(ProgressStatus.CORRECT, progress.getStatus());
        assertNotNull(progress.getAnsweredAt());

        long savedAnswers = runSelectedAnswerRepository
                .countByRun_RunIdAndQuestion_QuestionId(data.run.getRunId(), data.question.getQuestionId());

        assertEquals(1, savedAnswers);

        RoomStatusDto status = roomService.getRoomStatus(1, data.run.getRunId());

        assertEquals(1, status.getTotalQuestions());
        assertEquals(1, status.getAnsweredQuestions());
        assertEquals(1, status.getCorrectAnswers());
        assertEquals(0, status.getWrongAnswers());
        assertEquals(5, status.getEarnedPoints());
        assertEquals("GOLD", status.getMedal());
    }

    private TestData createMcData()
    {
        User user = new User("jan" + System.nanoTime(), "HASH", "duck.jpg");
        entityManager.persist(user);

        GameRun run = new GameRun();
        run.setUser(user);
        run.setStartedAt(LocalDateTime.now());
        entityManager.persist(run);

        Team team = new Team("Team A");
        entityManager.persist(team);

        QuestionSet questionSet = new QuestionSet(team, "Set 1");
        entityManager.persist(questionSet);

        Theme theme = new Theme("Thema 1");
        entityManager.persist(theme);

        Question question = new Question(questionSet, QuestionType.MC, 5);
        question.setStartText("Was ist richtig?");
        question.setAllowsMultiple(false);
        question.setThemes(List.of(theme));
        entityManager.persist(question);

        AnswerOption correct = new AnswerOption(question, "Richtig", true, 1);
        entityManager.persist(correct);

        AnswerOption wrong = new AnswerOption(question, "Falsch", false, 2);
        entityManager.persist(wrong);

        question.setAnswerOptions(List.of(correct, wrong));

        entityManager.flush();

        QuestionProgress progress = new QuestionProgress(
                run,
                question,
                1,
                1,
                ProgressStatus.OPEN,
                null
        );
        questionProgressRepository.save(progress);

        entityManager.flush();

        return new TestData(run, question, correct);
    }

    private record TestData(GameRun run, Question question, AnswerOption correctAnswer) {}
}