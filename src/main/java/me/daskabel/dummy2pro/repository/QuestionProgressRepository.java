package me.daskabel.dummy2pro.repository;

import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionProgressRepository extends JpaRepository<QuestionProgress, QuestionProgressId>
{
    List<QuestionProgress> findByRun_RunId(Long runId);

    List<QuestionProgress> findByRun_RunIdOrderByQuestion_QuestionIdAsc(Long runId);

    Optional<QuestionProgress> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    long countByRun_RunId(Long runId);

    long countByRun_RunIdAndStatus(Long runId, ProgressStatus status);

    @Query("""
            SELECT qp FROM QuestionProgress qp
            JOIN qp.question q
            JOIN q.themes t
            WHERE qp.run.runId = :runId
              AND t.themeId = :themeId
            """)
    List<QuestionProgress> findByRunIdAndThemeId(@Param("runId") Long runId,
                                                 @Param("themeId") Long themeId);
}