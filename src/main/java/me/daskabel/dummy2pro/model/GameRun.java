package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Repräsentiert einen einzelnen Spielstand eines Benutzers.
 *
 * Gespeichert werden der Besitzer des Spielstands, der Startzeitpunkt,
 * ein möglicher Abschlusszeitpunkt und ein optionaler Anzeigename.
 */
@Entity
@Table(name = "game_run")
public class GameRun
{
    public GameRun(User user, LocalDateTime startedAt)
    {
        this.user = user;
        this.startedAt = startedAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "display_name")
    private String displayName;

    public GameRun()
    {
    }

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;
    }

    public LocalDateTime getStartedAt()
    {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt)
    {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt()
    {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt)
    {
        this.finishedAt = finishedAt;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }
}
