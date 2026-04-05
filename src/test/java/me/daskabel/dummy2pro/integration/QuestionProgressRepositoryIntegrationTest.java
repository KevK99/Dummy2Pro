package me.daskabel.dummy2pro.integration;

import jakarta.persistence.EntityManager;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class QuestionProgressRepositoryIntegrationTest
{
    @Autowired
    private QuestionProgressRepository questionProgressRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void updateStatusAndAnsweredAt_updatesExistingProgressRow()
    {
        TestData data = createBasicData();

        int updated = questionProgressRepository.updateStatusAndAnsweredAt(
                data.run.getRunId(),
                data.question1.getQuestionId(),
                ProgressStatus.CORRECT,
                LocalDateTime.now()
        );

        entityManager.flush();
        entityManager.clear();

        QuestionProgress loaded = questionProgressRepository
                .findByRun_RunIdAndQuestion_QuestionId(data.run.getRunId(), data.question1.getQuestionId())
                .orElseThrow();

        assertEquals(1, updated);
        assertEquals(ProgressStatus.CORRECT, loaded.getStatus());
        assertNotNull(loaded.getAnsweredAt());
    }

    @Test
    void summarizeRoomProgressByRunId_returnsAggregatedRoomValues()
    {
        TestData data = createBasicData();

        QuestionProgress progress1 = questionProgressRepository
                .findByRun_RunIdAndQuestion_QuestionId(data.run.getRunId(), data.question1.getQuestionId())
                .orElseThrow();
        progress1.setStatus(ProgressStatus.CORRECT);
        progress1.setAnsweredAt(LocalDateTime.now());

        QuestionProgress progress2 = questionProgressRepository
                .findByRun_RunIdAndQuestion_QuestionId(data.run.getRunId(), data.question2.getQuestionId())
                .orElseThrow();
        progress2.setStatus(ProgressStatus.WRONG);
        progress2.setAnsweredAt(LocalDateTime.now());

        questionProgressRepository.save(progress1);
        questionProgressRepository.save(progress2);

        entityManager.flush();
        entityManager.clear();

        List<QuestionProgressRepository.RoomProgressSummary> summaries =
                questionProgressRepository.summarizeRoomProgressByRunId(data.run.getRunId());

        assertEquals(1, summaries.size());

        QuestionProgressRepository.RoomProgressSummary summary = summaries.get(0);
        assertEquals(1, summary.getRoomId());
        assertEquals(2L, summary.getTotalQuestions());
        assertEquals(2L, summary.getAnsweredQuestions());
        assertEquals(1L, summary.getCorrectAnswers());
        assertEquals(1L, summary.getWrongAnswers());
        assertEquals(8L, summary.getTotalPoints());
        assertEquals(5L, summary.getEarnedPoints());
    }

    private TestData createBasicData()
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

        Question question1 = new Question(questionSet, QuestionType.MC, 5);
        question1.setStartText("Frage 1");
        question1.setAllowsMultiple(false);
        question1.setThemes(List.of(theme));
        entityManager.persist(question1);

        Question question2 = new Question(questionSet, QuestionType.MC, 3);
        question2.setStartText("Frage 2");
        question2.setAllowsMultiple(false);
        question2.setThemes(List.of(theme));
        entityManager.persist(question2);

        entityManager.flush();

        QuestionProgress qp1 = new QuestionProgress(run, question1, 1, 1, ProgressStatus.OPEN, null);
        QuestionProgress qp2 = new QuestionProgress(run, question2, 1, 2, ProgressStatus.OPEN, null);

        questionProgressRepository.save(qp1);
        questionProgressRepository.save(qp2);

        entityManager.flush();

        return new TestData(run, question1, question2);
    }

    private record TestData(GameRun run, Question question1, Question question2) {}
}