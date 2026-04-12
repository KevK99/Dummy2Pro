package me.daskabel.dummy2pro.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.session.QuizSession;
import me.daskabel.dummy2pro.session.QuizSession.RoomSession;

/**
 * Unittests für den {@link QuizSessionGenerator}.
 *
 * Getestet werden hier der Aufbau von Platzhalter-Räumen, das Laden von Fragen
 * in stabiler Reihenfolge und der Aufbau einer vollständigen
 * {@link RoomSession}. Der Fokus liegt auf der Generatorlogik selbst,
 * unabhängig von Spring-Kontext oder Datenbank.
 */
@ExtendWith(MockitoExtension.class)
class QuizSessionGeneratorTest
{
    @Mock
    private QuestionRepository questionRepo;

    @Mock
    private ThemeRepository themeRepo;

    @InjectMocks
    private QuizSessionGenerator generator;

    @Test
    void loadQuestionsByIdsOrdered_ShouldReturnQuestionsInRequestedOrder()
    {
        Question question1 = question(1L, 2);
        Question question2 = question(2L, 3);
        Question question3 = question(3L, 4);

        when(questionRepo.findByQuestionIdsWithAnswers(List.of(3L, 1L, 2L))).thenReturn(List.of(question1, question2, question3));
        when(questionRepo.findByQuestionIdsWithGaps(List.of(3L, 1L, 2L))).thenReturn(List.of(question1, question2, question3));

        List<Question> ordered = generator.loadQuestionsByIdsOrdered(List.of(3L, 1L, 2L));

        assertIterableEquals(List.of(3L, 1L, 2L), ordered.stream().map(Question::getQuestionId).toList());
    }

    @Test
    void loadQuestionsByIdsOrdered_ShouldThrow_WhenASelectedQuestionCannotBeLoaded()
    {
        Question question1 = question(1L, 2);
        Question question2 = question(2L, 3);

        when(questionRepo.findByQuestionIdsWithAnswers(List.of(1L, 2L, 3L))).thenReturn(List.of(question1, question2));
        when(questionRepo.findByQuestionIdsWithGaps(List.of(1L, 2L, 3L))).thenReturn(List.of(question1, question2));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> generator.loadQuestionsByIdsOrdered(List.of(1L, 2L, 3L))
        );

        assertEquals("Nicht alle ausgewählten Fragen konnten geladen werden.", ex.getMessage());
    }

    @Test
    void generateSkeleton_ShouldCreatePlaceholderRoomForEachTheme()
    {
        Theme theme1 = new Theme("Recht");
        theme1.setThemeId(10L);
        Theme theme2 = new Theme("Wirtschaft");
        theme2.setThemeId(11L);
        Theme theme3 = new Theme("SQL");
        theme3.setThemeId(12L);

        when(themeRepo.findAllByOrderByThemeIdAsc()).thenReturn(List.of(theme1, theme2, theme3));

        QuizSession session = generator.generateSkeleton(7L, 77L);

        assertEquals(3, session.getRooms().size());
        assertEquals("Recht", session.getRoom(1).getThemeName());
        assertEquals("Wirtschaft", session.getRoom(2).getThemeName());
        assertEquals("SQL", session.getRoom(3).getThemeName());
        assertTrue(session.getRoom(1).getQuestionSequence().isEmpty());
        assertEquals(0, session.getRoom(1).getMaxPoints());
    }

    @Test
    void buildRoomSession_ShouldUseThemeNameQuestionsAndPointSum()
    {
        Theme theme = new Theme("Programmierung");
        theme.setThemeId(20L);

        Question question1 = question(101L, 2);
        Question question2 = question(102L, 5);

        when(questionRepo.findQuestionIdsByThemeId(20L)).thenReturn(List.of(101L, 102L));
        when(questionRepo.findByQuestionIdsWithAnswers(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(question1, question2));
        when(questionRepo.findByQuestionIdsWithGaps(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(question1, question2));

        RoomSession roomSession = generator.buildRoomSession(theme, 8);

        assertEquals(8, roomSession.getRoomId());
        assertEquals("Programmierung", roomSession.getThemeName());
        assertEquals(2, roomSession.getQuestionSequence().size());
        assertEquals(Set.of(101L, 102L), roomSession.getQuestionSequence().stream().collect(Collectors.toSet()));
        assertEquals(7, roomSession.getMaxPoints());
    }

    private static Question question(Long questionId, int points)
    {
        Question question = new Question(QuestionType.MC, "Frage", null, null, false, points);
        question.setQuestionId(questionId);
        return question;
    }
}
