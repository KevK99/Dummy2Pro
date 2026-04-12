package me.daskabel.dummy2pro.repository;

import java.util.List;
import java.util.Optional;

import me.daskabel.dummy2pro.model.RunSelectedAnswer;
import me.daskabel.dummy2pro.model.RunSelectedAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Stellt Datenbankzugriffe für gespeicherte Antwortauswahlen bereit.
 *
 * Enthält Methoden zum Laden, Suchen, Zählen und Löschen von Antworten
 * zu Multiple-Choice- und Richtig/Falsch-Fragen innerhalb eines Spielstands.
 */
public interface RunSelectedAnswerRepository extends JpaRepository<RunSelectedAnswer, RunSelectedAnswerId>
{
    List<RunSelectedAnswer> findByRun_RunId(Long runId);

    @Query("""
        SELECT DISTINCT rsa
        FROM RunSelectedAnswer rsa
        JOIN FETCH rsa.question q
        JOIN FETCH rsa.answerOption ao
        WHERE rsa.run.runId = :runId
    """)
    List<RunSelectedAnswer> findDetailedByRunId(@Param("runId") Long runId);

    List<RunSelectedAnswer> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    Optional<RunSelectedAnswer> findByRun_RunIdAndQuestion_QuestionIdAndAnswerOption_AnswerId(
            Long runId,
            Long questionId,
            Long answerId
    );

    void deleteByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    void deleteByRun_RunId(Long runId);

    long countByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);
}
