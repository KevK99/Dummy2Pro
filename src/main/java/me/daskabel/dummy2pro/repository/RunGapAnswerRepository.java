package me.daskabel.dummy2pro.repository;

import java.util.List;
import java.util.Optional;

import me.daskabel.dummy2pro.model.RunGapAnswer;
import me.daskabel.dummy2pro.model.RunGapAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Stellt Datenbankzugriffe für gespeicherte Lückenantworten bereit.
 *
 * Enthält Methoden zum Laden, Suchen und Löschen von Antworten
 * zu Lückentextfragen innerhalb eines Spielstands.
 */
public interface RunGapAnswerRepository extends JpaRepository<RunGapAnswer, RunGapAnswerId>
{
    List<RunGapAnswer> findByRun_RunId(Long runId);

    @Query("""
        SELECT DISTINCT rga
        FROM RunGapAnswer rga
        JOIN FETCH rga.question q
        JOIN FETCH rga.gapField gf
        JOIN FETCH rga.selectedGapOption sgo
        WHERE rga.run.runId = :runId
    """)
    List<RunGapAnswer> findDetailedByRunId(@Param("runId") Long runId);

    List<RunGapAnswer> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    Optional<RunGapAnswer> findByRun_RunIdAndQuestion_QuestionIdAndGapField_GapId(
            Long runId,
            Long questionId,
            Long gapId
    );

    void deleteByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    void deleteByRun_RunId(Long runId);
}
