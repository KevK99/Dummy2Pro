package me.daskabel.dummy2pro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionProgressId;

public interface QuestionProgressRepository extends JpaRepository<QuestionProgress, QuestionProgressId>
{
    interface RoomOverviewProjection
    {
        Integer getRoomId();
        Long getTotalQuestions();
        Long getAnsweredQuestions();
        Long getCorrectAnswers();
    }

    long countByRun_RunId(Long runId);

    long countByRun_RunIdAndStatus(Long runId, ProgressStatus status);

    void deleteByRun_RunId(Long runId);

    List<QuestionProgress> findByRun_RunId(Long runId);

    Optional<QuestionProgress> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    List<QuestionProgress> findByRun_RunIdOrderByRoomIdAscQuestionOrderAsc(Long runId);

    @Query("""
        SELECT qp FROM QuestionProgress qp
        WHERE qp.run.runId = :runId
          AND qp.roomId = :roomId
          AND qp.status = :status
        ORDER BY qp.questionOrder
        """)
    List<QuestionProgress> findByRunIdAndRoomIdAndStatusOrderByQuestionOrder(@Param("runId") Long runId,
                                                                             @Param("roomId") int roomId, @Param("status") ProgressStatus status);

    @Query("""
        SELECT qp FROM QuestionProgress qp
        WHERE qp.run.runId = :runId
          AND qp.roomId = :roomId
        ORDER BY qp.questionOrder
        """)
    List<QuestionProgress> findByRunIdAndRoomIdOrderByQuestionOrder(@Param("runId") Long runId,
                                                                    @Param("roomId") int roomId);

    @Query(value = """
        SELECT
            qp.room_id AS roomId,
            COUNT(*) AS totalQuestions,
            SUM(CASE WHEN qp.status <> 'OPEN' THEN 1 ELSE 0 END) AS answeredQuestions,
            SUM(CASE WHEN qp.status = 'CORRECT' THEN 1 ELSE 0 END) AS correctAnswers
        FROM question_progress qp
        WHERE qp.run_id = :runId
        GROUP BY qp.room_id
        ORDER BY qp.room_id
        """, nativeQuery = true)
    List<RoomOverviewProjection> summarizeByRunId(@Param("runId") Long runId);

}
