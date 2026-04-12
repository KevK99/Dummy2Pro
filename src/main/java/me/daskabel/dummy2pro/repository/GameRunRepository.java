package me.daskabel.dummy2pro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.daskabel.dummy2pro.model.GameRun;

/**
 * Stellt Datenbankzugriffe für Spielstände bereit.
 *
 * Enthält Suchmethoden für einzelne Spielstände, alle Spielstände
 * eines Benutzers sowie verschiedene Sortierungen.
 */
public interface GameRunRepository extends JpaRepository<GameRun, Long>
{
    Optional<GameRun> findByRunIdAndUser_UserId(Long runId, Long userId);

    List<GameRun> findByUser_UserId(Long userId);

    List<GameRun> findByUser_UserIdOrderByFinishedAtAscStartedAtDesc(Long userId);

    List<GameRun> findByUser_UserIdOrderByStartedAtDesc(Long userId);

    /**
     * Liefert den zuletzt gestarteten Spielstand des Benutzers.
     */
    Optional<GameRun> findTopByUser_UserIdOrderByStartedAtDesc(Long userId);

    long countByUser_UserId(Long userId);
}
