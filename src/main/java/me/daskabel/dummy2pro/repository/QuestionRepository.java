package me.daskabel.dummy2pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.daskabel.dummy2pro.model.Question;

/**
 * Datenbankzugriff für Fragen.
 *
 * Wichtigste Query: Alle Fragen eines bestimmten Themas laden, inklusive aller
 * Antwortoptionen (MC/TF) und Gap-Felder mit deren Optionen. Die Shuffle-Logik
 * passiert im RoomService (nicht per SQL ORDER BY RAND(), da das bei großen
 * Mengen langsam ist und wir eh alles in den Speicher laden).
 */
public interface QuestionRepository extends JpaRepository<Question, Long>
{

	/**
	 * Alle Fragen zu einem bestimmten Theme (über die Join-Tabelle question_theme).
	 * DISTINCT verhindert Duplikate durch den Join.
	 */
	@Query("""
        SELECT DISTINCT q FROM Question q
        JOIN q.themes t
        WHERE t.themeId = :themeId
        ORDER BY q.questionId
    """)
	List<Question> findByThemeId(@Param("themeId") Long themeId);

	/**
	 * Alle Fragen zu einem Theme, mit Antwortoptionen vorgeladen (verhindert N+1). Wird
	 * für MC/TF-Fragen genutzt.
	 */
	@Query("""
        SELECT DISTINCT q FROM Question q
        LEFT JOIN FETCH q.answerOptions
        JOIN q.themes t
        WHERE t.themeId = :themeId
    """)
	List<Question> findByThemeIdWithAnswers(@Param("themeId") Long themeId);

    /**
     * Alle Fragen zu einem Theme, mit GapFields vorgeladen.
     * GapOptions werden anschließend geladen.
     */
    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.gapFields gf
        LEFT JOIN FETCH gf.gapOptions go
        JOIN q.themes t
        WHERE t.themeId = :themeId
        ORDER BY q.questionId
        """)
            List<Question> findByThemeIdWithGaps(@Param("themeId") Long themeId);

    @Query("""
        SELECT COUNT(DISTINCT q.questionId)
        FROM Question q
        JOIN q.themes t
        WHERE t.themeId = :themeId
        """)
    long countQuestionsByThemeId(@Param("themeId") Long themeId);
}