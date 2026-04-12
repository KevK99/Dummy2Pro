package me.daskabel.dummy2pro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.daskabel.dummy2pro.model.Question;

/**
 * Stellt Datenbankzugriffe für Fragen bereit.
 *
 * Enthält Methoden zum Laden von Fragen, Fragen-IDs und den zugehörigen
 * Antwort- bzw. Lückendaten.
 */
public interface QuestionRepository extends JpaRepository<Question, Long>
{
    /**
     * Liefert die IDs aller Fragen eines Themes in aufsteigender Reihenfolge.
     *
     * Die Methode wird dann verwendet, wenn zunächst nur die Reihenfolge
     * bestimmt werden soll und die eigentlichen Fragedaten in einem zweiten
     * Schritt gezielt mit Unterobjekten nachgeladen werden.
     */
    @Query("""
        SELECT q.questionId
        FROM Question q
        JOIN q.themes t
        WHERE t.themeId = :themeId
        ORDER BY q.questionId
    """)
    List<Long> findQuestionIdsByThemeId(@Param("themeId") Long themeId);

    /**
     * Lädt Fragen zusammen mit ihren Antwortoptionen.
     *
     * Das {@code FETCH JOIN} verhindert zusätzliche Nachladezugriffe beim
     * Zugriff auf MC- oder TF-Antworten.
     */
    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.answerOptions
        WHERE q.questionId IN :questionIds
    """)
    List<Question> findByQuestionIdsWithAnswers(@Param("questionIds") List<Long> questionIds);

    /**
     * Lädt Fragen zusammen mit ihren Lückenfeldern und deren Auswahloptionen.
     *
     * Die Abfrage ist für GAP-Fragen gedacht, damit die komplette Struktur in
     * einem Zugriff vorliegt.
     */
    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.gapFields gf
        LEFT JOIN FETCH gf.gapOptions
        WHERE q.questionId IN :questionIds
    """)
    List<Question> findByQuestionIdsWithGaps(@Param("questionIds") List<Long> questionIds);

    /**
     * Lädt genau eine Frage inklusive Antwortoptionen.
     *
     * Wird typischerweise für die Auswertung von MC- und TF-Antworten verwendet.
     */
    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.answerOptions
        WHERE q.questionId = :questionId
    """)
    Optional<Question> findByQuestionIdWithAnswers(@Param("questionId") Long questionId);

    /**
     * Lädt genau eine Frage inklusive Lückenfeldern und deren Optionen.
     *
     * Damit kann eine GAP-Frage vollständig ausgewertet werden, ohne dass
     * weitere Lazy-Loads nötig sind.
     */
    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.gapFields gf
        LEFT JOIN FETCH gf.gapOptions
        WHERE q.questionId = :questionId
    """)
    Optional<Question> findByQuestionIdWithGaps(@Param("questionId") Long questionId);
}
