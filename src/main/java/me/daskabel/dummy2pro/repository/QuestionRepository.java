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
    @Query("""
        SELECT q.questionId
        FROM Question q
        JOIN q.themes t
        WHERE t.themeId = :themeId
        ORDER BY q.questionId
    """)
    List<Long> findQuestionIdsByThemeId(@Param("themeId") Long themeId);

    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.answerOptions
        WHERE q.questionId IN :questionIds
    """)
    List<Question> findByQuestionIdsWithAnswers(@Param("questionIds") List<Long> questionIds);

    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.gapFields gf
        LEFT JOIN FETCH gf.gapOptions
        WHERE q.questionId IN :questionIds
    """)
    List<Question> findByQuestionIdsWithGaps(@Param("questionIds") List<Long> questionIds);

    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.answerOptions
        WHERE q.questionId = :questionId
    """)
    Optional<Question> findByQuestionIdWithAnswers(@Param("questionId") Long questionId);

    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.gapFields gf
        LEFT JOIN FETCH gf.gapOptions
        WHERE q.questionId = :questionId
    """)
    Optional<Question> findByQuestionIdWithGaps(@Param("questionId") Long questionId);
}
