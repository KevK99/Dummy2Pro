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

    Optional<QuestionProgress> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);
    List<QuestionProgress> findByRun_RunIdOrderByRoomIdAscQuestionOrderAsc(Long runId);

    long countByRun_RunId(Long runId);

    long countByRun_RunIdAndStatus(Long runId, ProgressStatus status);

    @Query("""
    SELECT qp FROM QuestionProgress qp
    WHERE qp.run.runId = :runId
      AND qp.roomId = :roomId
    ORDER BY qp.questionOrder
    """)
    List<QuestionProgress> findByRunIdAndRoomIdOrderByQuestionOrder(@Param("runId") Long runId,
        @Param("roomId") int roomId);

    @Query("""
    SELECT qp FROM QuestionProgress qp
    WHERE qp.run.runId = :runId
      AND qp.roomId = :roomId
      AND qp.status = :status
    ORDER BY qp.questionOrder
    """)
    List<QuestionProgress> findByRunIdAndRoomIdAndStatusOrderByQuestionOrder(@Param("runId") Long runId,
        @Param("roomId") int roomId,
        @Param("status") ProgressStatus status);

}