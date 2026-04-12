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

/**
 * Stellt Datenbankzugriffe für den Bearbeitungsstand von Fragen bereit.
 *
 * Enthält Such-, Änderungs- und Auswertungsmethoden für Fragen
 * innerhalb eines Spielstands.
 *
 * Die Methoden decken sowohl einfache CRUD-Zugriffe als auch projektspezifische
 * Auswertungen für Raumübersichten, Wiederherstellung und Review ab.
 */
public interface QuestionProgressRepository extends JpaRepository<QuestionProgress, QuestionProgressId>
{
    /**
     * Projektion für die zusammengefasste Raumübersicht eines Spielstands.
     *
     * Das Interface wird direkt von Spring Data befüllt und vermeidet dafür
     * eine zusätzliche DTO-Mappingklasse auf Repository-Ebene.
     */
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

    /**
     * Zählt alle Fortschrittseinträge eines Spielstands.
     */
    long countByRun_RunId(Long runId);

    /**
     * Zählt alle Fortschrittseinträge eines Spielstands in einem bestimmten Status.
     */
    long countByRun_RunIdAndStatus(Long runId, ProgressStatus status);

    /**
     * Entfernt sämtliche Fortschrittsdaten eines Spielstands.
     */
    void deleteByRun_RunId(Long runId);

    /**
     * Lädt alle Fortschrittsdaten eines Spielstands ohne feste Sortierung.
     */
    List<QuestionProgress> findByRun_RunId(Long runId);

    /**
     * Lädt den Fortschritt einer konkreten Frage innerhalb eines Spielstands.
     */
    Optional<QuestionProgress> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    /**
     * Lädt alle Fortschrittsdaten eines Spielstands in stabiler Raum- und Fragefolge.
     */
    List<QuestionProgress> findByRun_RunIdOrderByRoomIdAscQuestionOrderAsc(Long runId);

    /**
     * Lädt Fortschrittsdaten inklusive Frageentität für eine detaillierte Ansicht.
     *
     * Der Fetch Join reduziert Nachladezugriffe bei nachgelagerter Auswertung.
     */
    @Query("""
        SELECT qp
        FROM QuestionProgress qp
        JOIN FETCH qp.question q
        WHERE qp.run.runId = :runId
        ORDER BY qp.roomId, qp.questionOrder
    """)
    List<QuestionProgress> findDetailedByRunIdOrderByRoomIdAscQuestionOrderAsc(@Param("runId") Long runId);

    /**
     * Lädt alle Fragen eines Raums mit einem bestimmten Bearbeitungsstatus.
     *
     * Die Reihenfolge folgt der gespeicherten Raumreihenfolge und nicht der
     * zufälligen Datenbankreihenfolge.
     */
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

    /**
     * Lädt alle Fragen eines Raums in der gespeicherten Bearbeitungsreihenfolge.
     */
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

    /**
     * Erzeugt eine zusammengefasste Raumübersicht für den kompletten Spielstand.
     *
     * Berechnet werden Anzahl, Statusverteilung und Punktesummen je Raum.
     * Die Abfrage dient als Grundlage für Übersichten und Dashboardwerte.
     */
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

    /**
     * Aktualisiert Status und Antwortzeitpunkt einer konkreten Frage.
     */
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
