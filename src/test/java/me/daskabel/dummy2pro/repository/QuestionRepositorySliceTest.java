package me.daskabel.dummy2pro.repository;

import jakarta.persistence.EntityManager;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionSet;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Team;
import me.daskabel.dummy2pro.model.Theme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class QuestionRepositorySliceTest
{
    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findQuestionIdsByThemeId_returnsIdsOrderedByQuestionId()
    {
        SeedData data = seedQuestions();

        List<Long> ids = questionRepository.findQuestionIdsByThemeId(data.theme().getThemeId());

        assertEquals(List.of(data.mcQuestion().getQuestionId(), data.gapQuestion().getQuestionId()), ids);
    }

    @Test
    void findByQuestionIdsWithAnswers_fetchesMcQuestionsWithAnswerOptions()
    {
        SeedData data = seedQuestions();

        List<Question> questions = questionRepository.findByQuestionIdsWithAnswers(
                List.of(data.mcQuestion().getQuestionId())
        );

        assertEquals(1, questions.size());

        Question loaded = questions.get(0);
        assertEquals(data.mcQuestion().getQuestionId(), loaded.getQuestionId());
        assertNotNull(loaded.getAnswerOptions());
        assertEquals(2, loaded.getAnswerOptions().size());
        assertTrue(loaded.getAnswerOptions().stream().anyMatch(AnswerOption::getIsCorrect));
    }

    @Test
    void findByQuestionIdsWithGaps_fetchesGapQuestionsWithGapFieldsAndOptions()
    {
        SeedData data = seedQuestions();

        List<Question> questions = questionRepository.findByQuestionIdsWithGaps(
                List.of(data.gapQuestion().getQuestionId())
        );

        assertEquals(1, questions.size());

        Question loaded = questions.get(0);
        assertEquals(data.gapQuestion().getQuestionId(), loaded.getQuestionId());
        assertNotNull(loaded.getGapFields());
        assertEquals(1, loaded.getGapFields().size());

        GapField gapField = loaded.getGapFields().iterator().next();
        assertEquals(2, gapField.getGapOptions().size());
        assertTrue(gapField.getGapOptions().stream().anyMatch(GapOption::getIsCorrect));
    }

    @Test
    void findByQuestionIdWithAnswers_returnsSingleQuestionWithFetchedAnswers()
    {
        SeedData data = seedQuestions();

        Question loaded = questionRepository.findByQuestionIdWithAnswers(data.mcQuestion().getQuestionId())
                .orElseThrow();

        assertEquals(data.mcQuestion().getQuestionId(), loaded.getQuestionId());
        assertEquals(2, loaded.getAnswerOptions().size());
    }

    @Test
    void findByQuestionIdWithGaps_returnsSingleQuestionWithFetchedGaps()
    {
        SeedData data = seedQuestions();

        Question loaded = questionRepository.findByQuestionIdWithGaps(data.gapQuestion().getQuestionId())
                .orElseThrow();

        assertEquals(data.gapQuestion().getQuestionId(), loaded.getQuestionId());
        assertEquals(1, loaded.getGapFields().size());

        GapField gapField = loaded.getGapFields().iterator().next();
        assertEquals(2, gapField.getGapOptions().size());
    }

    private SeedData seedQuestions()
    {
        Team team = new Team("Team A");
        entityManager.persist(team);

        QuestionSet questionSet = new QuestionSet(team, "Set 1");
        entityManager.persist(questionSet);

        Theme theme = new Theme("Thema 1");
        entityManager.persist(theme);

        Question mcQuestion = new Question(questionSet, QuestionType.MC, 5);
        mcQuestion.setStartText("MC Frage");
        mcQuestion.setAllowsMultiple(false);
        mcQuestion.setThemes(List.of(theme));
        entityManager.persist(mcQuestion);

        AnswerOption mcCorrect = new AnswerOption(mcQuestion, "Richtig", true, 1);
        AnswerOption mcWrong = new AnswerOption(mcQuestion, "Falsch", false, 2);
        entityManager.persist(mcCorrect);
        entityManager.persist(mcWrong);
        mcQuestion.setAnswerOptions(List.of(mcCorrect, mcWrong));

        Question gapQuestion = new Question(questionSet, QuestionType.GAP, 7);
        gapQuestion.setStartText("Gap Frage");
        gapQuestion.setAllowsMultiple(false);
        gapQuestion.setThemes(List.of(theme));
        entityManager.persist(gapQuestion);

        GapField gapField = new GapField(gapQuestion, 0);
        gapField.setTextBefore("Vor");
        gapField.setTextAfter("Nach");
        entityManager.persist(gapField);

        GapOption gapCorrect = new GapOption(gapField, "A", true, 1);
        GapOption gapWrong = new GapOption(gapField, "B", false, 2);
        entityManager.persist(gapCorrect);
        entityManager.persist(gapWrong);

        Set<GapOption> gapOptions = new LinkedHashSet<>();
        gapOptions.add(gapCorrect);
        gapOptions.add(gapWrong);
        gapField.setGapOptions(gapOptions);

        Set<GapField> gapFields = new LinkedHashSet<>();
        gapFields.add(gapField);
        gapQuestion.setGapFields(gapFields);

        entityManager.flush();
        entityManager.clear();

        return new SeedData(theme, mcQuestion, gapQuestion);
    }

    private record SeedData(Theme theme, Question mcQuestion, Question gapQuestion) {}
}