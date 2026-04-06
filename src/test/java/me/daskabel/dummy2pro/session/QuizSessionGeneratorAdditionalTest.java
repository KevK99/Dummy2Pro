package me.daskabel.dummy2pro.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;

import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.session.QuizSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizSessionGeneratorAdditionalTest
{
    @Mock
    private QuestionRepository questionRepo;

    @Mock
    private ThemeRepository themeRepo;

    @InjectMocks
    private QuizSessionGenerator generator;

    @Test
    void loadQuestionsByIdsOrdered_shouldReturnEmptyList_forNullAndEmptyInput()
    {
        assertEquals(List.of(), generator.loadQuestionsByIdsOrdered(null));
        assertEquals(List.of(), generator.loadQuestionsByIdsOrdered(List.of()));
    }

    @Test
    void getThemesOrdered_shouldDelegateToRepository()
    {
        Theme theme1 = new Theme("Recht");
        Theme theme2 = new Theme("SQL");

        when(themeRepo.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme1, theme2));

        List<Theme> result = generator.getThemesOrdered();

        assertEquals(2, result.size());
        assertEquals("Recht", result.get(0).getName());
        assertEquals("SQL", result.get(1).getName());
        verify(themeRepo).findAllByOrderByThemeIdAsc();
    }

    @Test
    void buildRoomSession_shouldThrow_whenThemeHasNoQuestions()
    {
        Theme theme = new Theme("Leerer Raum");
        theme.setThemeId(50L);

        when(questionRepo.findQuestionIdsByThemeId(50L)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> generator.buildRoomSession(theme, 5)
        );

        assertEquals("Für Raum 5 sind keine Fragen in der Datenbank vorhanden.", ex.getMessage());
    }

    @Test
    void generate_shouldCreatePreparedRoomsForAllThemes()
    {
        Theme theme1 = new Theme("Recht");
        theme1.setThemeId(10L);

        Theme theme2 = new Theme("SQL");
        theme2.setThemeId(20L);

        Question question1 = question(101L, 2);
        Question question2 = question(202L, 5);

        when(themeRepo.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme1, theme2));

        when(questionRepo.findQuestionIdsByThemeId(10L)).thenReturn(List.of(101L));
        when(questionRepo.findByQuestionIdsWithAnswers(List.of(101L))).thenReturn(List.of(question1));
        when(questionRepo.findByQuestionIdsWithGaps(List.of(101L))).thenReturn(List.of(question1));

        when(questionRepo.findQuestionIdsByThemeId(20L)).thenReturn(List.of(202L));
        when(questionRepo.findByQuestionIdsWithAnswers(List.of(202L))).thenReturn(List.of(question2));
        when(questionRepo.findByQuestionIdsWithGaps(List.of(202L))).thenReturn(List.of(question2));

        QuizSession session = generator.generate(7L, 70L);

        assertEquals(2, session.getRooms().size());
        assertEquals("Recht", session.getRoom(1).getThemeName());
        assertEquals("SQL", session.getRoom(2).getThemeName());
        assertIterableEquals(List.of(101L), session.getRoom(1).getQuestionSequence());
        assertIterableEquals(List.of(202L), session.getRoom(2).getQuestionSequence());
        assertEquals(2, session.getRoom(1).getMaxPoints());
        assertEquals(5, session.getRoom(2).getMaxPoints());
    }

    @Test
    void loadQuestionsByIdsOrdered_shouldUseQuestionFromGapQuery_whenAnswerQueryIsEmpty()
    {
        Question gapQuestion = new Question(QuestionType.GAP, "Start", null, "Ende", false, 3);
        gapQuestion.setQuestionId(303L);

        GapField gapField = new GapField(gapQuestion, 0);
        gapField.setGapId(900L);
        gapField.setTextBefore("vor");
        gapField.setTextAfter("nach");

        GapOption gapOption = new GapOption(gapField, "Option", true, 1);
        gapOption.setGapOptionId(901L);
        gapField.setGapOptions(new LinkedHashSet<>(List.of(gapOption)));

        gapQuestion.setGapFields(new LinkedHashSet<>(List.of(gapField)));

        when(questionRepo.findByQuestionIdsWithAnswers(List.of(303L))).thenReturn(List.of());
        when(questionRepo.findByQuestionIdsWithGaps(List.of(303L))).thenReturn(List.of(gapQuestion));

        List<Question> ordered = generator.loadQuestionsByIdsOrdered(List.of(303L));

        assertEquals(1, ordered.size());
        assertEquals(303L, ordered.get(0).getQuestionId());
        assertEquals(1, ordered.get(0).getGapFields().size());
    }

    private Question question(Long questionId, int points)
    {
        Question question = new Question(QuestionType.MC, "Frage", null, null, false, points);
        question.setQuestionId(questionId);
        return question;
    }
}