package me.daskabel.dummy2pro.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionProgressId;

public interface QuestionProgressRepository extends JpaRepository<QuestionProgress, QuestionProgressId>
{
    interface RoomProgressSummary
    {
        Integer getRoomId();
        Long getTotalQuestions();
        Long getAnsweredQuestions();
        Long getCorrectAnswers();
        Long getWrongAnswers();
        Long getTotalPoints();
        Long getEarnedPoints();
    }

    long countByRun_RunId(Long runId);

    long countByRun_RunIdAndStatus(Long runId, ProgressStatus status);

    void deleteByRun_RunId(Long runId);

    List<QuestionProgress> findByRun_RunId(Long runId);

    Optional<QuestionProgress> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    List<QuestionProgress> findByRun_RunIdOrderByRoomIdAscQuestionOrderAsc(Long runId);

    @Query("""
        SELECT qp
        FROM QuestionProgress qp
        JOIN FETCH qp.question q
        WHERE qp.run.runId = :runId
        ORDER BY qp.roomId, qp.questionOrder
    """)
    List<QuestionProgress> findDetailedByRunIdOrderByRoomIdAscQuestionOrderAsc(@Param("runId") Long runId);

    @Query("""
        SELECT qp FROM QuestionProgress qp
        WHERE qp.run.runId = :runId
          AND qp.roomId = :roomId
          AND qp.status = :status
        ORDER BY qp.questionOrder
        """)
    List<QuestionProgress> findByRunIdAndRoomIdAndStatusOrderByQuestionOrder(
            @Param("runId") Long runId,
            @Param("roomId") int roomId,
            @Param("status") ProgressStatus status
    );

    @Query("""
        SELECT qp FROM QuestionProgress qp
        WHERE qp.run.runId = :runId
          AND qp.roomId = :roomId
        ORDER BY qp.questionOrder
        """)
    List<QuestionProgress> findByRunIdAndRoomIdOrderByQuestionOrder(
            @Param("runId") Long runId,
            @Param("roomId") int roomId
    );

    @Query("""
        SELECT
            qp.roomId AS roomId,
            COUNT(qp) AS totalQuestions,
            SUM(CASE
                    WHEN qp.status <> me.daskabel.dummy2pro.model.ProgressStatus.OPEN
                    THEN 1
                    ELSE 0
                END) AS answeredQuestions,
            SUM(CASE
                    WHEN qp.status = me.daskabel.dummy2pro.model.ProgressStatus.CORRECT
                    THEN 1
                    ELSE 0
                END) AS correctAnswers,
            SUM(CASE
                    WHEN qp.status = me.daskabel.dummy2pro.model.ProgressStatus.WRONG
                    THEN 1
                    ELSE 0
                END) AS wrongAnswers,
            COALESCE(SUM(q.points), 0) AS totalPoints,
            COALESCE(SUM(CASE
                    WHEN qp.status = me.daskabel.dummy2pro.model.ProgressStatus.CORRECT
                    THEN q.points
                    ELSE 0
                END), 0) AS earnedPoints
        FROM QuestionProgress qp
        JOIN qp.question q
        WHERE qp.run.runId = :runId
        GROUP BY qp.roomId
        ORDER BY qp.roomId
    """)
    List<RoomProgressSummary> summarizeRoomProgressByRunId(@Param("runId") Long runId);

    @Modifying
    @Query("""
        UPDATE QuestionProgress qp
        SET qp.status = :status,
            qp.answeredAt = :answeredAt
        WHERE qp.run.runId = :runId
          AND qp.question.questionId = :questionId
    """)
    int updateStatusAndAnsweredAt(
            @Param("runId") Long runId,
            @Param("questionId") Long questionId,
            @Param("status") ProgressStatus status,
            @Param("answeredAt") LocalDateTime answeredAt
    );
}