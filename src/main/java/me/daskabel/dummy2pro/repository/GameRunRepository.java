package me.daskabel.dummy2pro.repository;

import me.daskabel.dummy2pro.model.GameRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRunRepository extends JpaRepository<GameRun, Long>
{
    List<GameRun> findByUser_UserIdOrderByStartedAtDesc(Long userId);

    Optional<GameRun> findByRunIdAndUser_UserId(Long runId, Long userId);

    Optional<GameRun> findTopByUser_UserIdOrderByStartedAtDesc(Long userId);

    List<GameRun> findByUser_UserIdOrderByFinishedAtAscStartedAtDesc(Long userId);
}