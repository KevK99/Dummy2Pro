package me.daskabel.dummy2pro.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionSet;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.RunSelectedAnswer;
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
class RunSelectedAnswerRepositorySliceTest
{
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRunRepository gameRunRepository;

    @Autowired
    private RunSelectedAnswerRepository runSelectedAnswerRepository;

    @Test
    void findDetailedByRunId_fetchesQuestionAndAnswerOption() 
    {
        PersistedMcData data = persistMcData();

        runSelectedAnswerRepository.save(new RunSelectedAnswer(data.run(), data.question(), data.correctAnswer()));
        runSelectedAnswerRepository.save(new RunSelectedAnswer(data.run(), data.question(), data.wrongAnswer()));

        entityManager.flush();
        entityManager.clear();

        List<RunSelectedAnswer> results = runSelectedAnswerRepository.findDetailedByRunId(data.run().getRunId());

        assertEquals(2, results.size());
        assertTrue(Persistence.getPersistenceUtil().isLoaded(results.get(0).getQuestion()));
        assertTrue(Persistence.getPersistenceUtil().isLoaded(results.get(0).getAnswerOption()));
    }

    @Test
    void questionScopedFinders_count_andDelete_workTogether()
    {
        PersistedMcData data = persistMcData();

        runSelectedAnswerRepository.save(new RunSelectedAnswer(data.run(), data.question(), data.correctAnswer()));
        runSelectedAnswerRepository.save(new RunSelectedAnswer(data.run(), data.question(), data.wrongAnswer()));

        entityManager.flush();
        entityManager.clear();

        assertEquals(2, runSelectedAnswerRepository.findByRun_RunIdAndQuestion_QuestionId(
                data.run().getRunId(),
                data.question().getQuestionId()
        ).size());

        assertEquals(2, runSelectedAnswerRepository.countByRun_RunIdAndQuestion_QuestionId(
                data.run().getRunId(),
                data.question().getQuestionId()
        ));

        assertTrue(runSelectedAnswerRepository.findByRun_RunIdAndQuestion_QuestionIdAndAnswerOption_AnswerId(
                data.run().getRunId(),
                data.question().getQuestionId(),
                data.correctAnswer().getAnswerId()
        ).isPresent());

        runSelectedAnswerRepository.deleteByRun_RunIdAndQuestion_QuestionId(
                data.run().getRunId(),
                data.question().getQuestionId()
        );
        entityManager.flush();

        assertEquals(0, runSelectedAnswerRepository.countByRun_RunIdAndQuestion_QuestionId(
                data.run().getRunId(),
                data.question().getQuestionId()
        ));
    }

    private PersistedMcData persistMcData()
    {
        Team team = new Team("Team RSA " + System.nanoTime());
        entityManager.persist(team);

        QuestionSet questionSet = new QuestionSet(team, "Set RSA");
        entityManager.persist(questionSet);

        Question question = new Question(questionSet, QuestionType.MC, 5);
        question.setStartText("Welche Antworten wurden gewählt?");
        question.setAllowsMultiple(true);
        entityManager.persist(question);

        AnswerOption correctAnswer = new AnswerOption(question, "Antwort A", true, 1);
        AnswerOption wrongAnswer = new AnswerOption(question, "Antwort B", false, 2);
        entityManager.persist(correctAnswer);
        entityManager.persist(wrongAnswer);

        User user = userRepository.save(new User("rsa_" + System.nanoTime(), "hash", "duck.jpg"));
        GameRun run = gameRunRepository.save(new GameRun(user, LocalDateTime.now()));

        entityManager.flush();

        return new PersistedMcData(run, question, correctAnswer, wrongAnswer);
    }

    private record PersistedMcData(GameRun run, Question question, AnswerOption correctAnswer, AnswerOption wrongAnswer)
    {
    }
}
