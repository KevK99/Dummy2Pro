package me.daskabel.dummy2pro.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionSet;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.RunGapAnswer;
import me.daskabel.dummy2pro.model.Team;
import me.daskabel.dummy2pro.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class RunGapAnswerRepositorySliceTest
{
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRunRepository gameRunRepository;

    @Autowired
    private RunGapAnswerRepository runGapAnswerRepository;

    @Test
    void findDetailedByRunId_fetchesQuestionGapFieldAndSelectedOption()
    {
        PersistedGapData data = persistGapData();

        runGapAnswerRepository.save(new RunGapAnswer(
                data.run(),
                data.question(),
                data.gapField(),
                data.correctOption(),
                LocalDateTime.now()
        ));

        entityManager.flush();
        entityManager.clear();

        List<RunGapAnswer> results = runGapAnswerRepository.findDetailedByRunId(data.run().getRunId());

        assertEquals(1, results.size());
        assertTrue(Persistence.getPersistenceUtil().isLoaded(results.get(0).getQuestion()));
        assertTrue(Persistence.getPersistenceUtil().isLoaded(results.get(0).getGapField()));
        assertTrue(Persistence.getPersistenceUtil().isLoaded(results.get(0).getSelectedGapOption()));
    }

    @Test
    void questionAndGapScopedFinders_andDelete_workTogether()
    {
        PersistedGapData data = persistGapData();

        runGapAnswerRepository.save(new RunGapAnswer(
                data.run(),
                data.question(),
                data.gapField(),
                data.correctOption(),
                LocalDateTime.now()
        ));

        entityManager.flush();
        entityManager.clear();

        assertEquals(1, runGapAnswerRepository.findByRun_RunIdAndQuestion_QuestionId(
                data.run().getRunId(),
                data.question().getQuestionId()
        ).size());

        assertTrue(runGapAnswerRepository.findByRun_RunIdAndQuestion_QuestionIdAndGapField_GapId(
                data.run().getRunId(),
                data.question().getQuestionId(),
                data.gapField().getGapId()
        ).isPresent());

        runGapAnswerRepository.deleteByRun_RunIdAndQuestion_QuestionId(
                data.run().getRunId(),
                data.question().getQuestionId()
        );
        entityManager.flush();

        assertEquals(0, runGapAnswerRepository.findByRun_RunIdAndQuestion_QuestionId(
                data.run().getRunId(),
                data.question().getQuestionId()
        ).size());
    }

    private PersistedGapData persistGapData()
    {
        Team team = new Team("Team RGA " + System.nanoTime());
        entityManager.persist(team);

        QuestionSet questionSet = new QuestionSet(team, "Set RGA");
        entityManager.persist(questionSet);

        Question question = new Question(questionSet, QuestionType.GAP, 5);
        question.setStartText("Spring Boot läuft mit");
        question.setAllowsMultiple(false);
        entityManager.persist(question);

        GapField gapField = new GapField(question, 0);
        gapField.setTextBefore("auf");
        gapField.setTextAfter(".");
        entityManager.persist(gapField);

        GapOption correctOption = new GapOption(gapField, "Java", true, 1);
        GapOption wrongOption = new GapOption(gapField, "PHP", false, 2);
        entityManager.persist(correctOption);
        entityManager.persist(wrongOption);

        User user = userRepository.save(new User("rga_" + System.nanoTime(), "hash", "duck.jpg"));
        GameRun run = gameRunRepository.save(new GameRun(user, LocalDateTime.now()));

        entityManager.flush();

        return new PersistedGapData(run, question, gapField, correctOption);
    }

    private record PersistedGapData(GameRun run, Question question, GapField gapField, GapOption correctOption)
    {
    }
}
