package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question_progress")
public class QuestionProgress
{
    @EmbeddedId
    private QuestionProgressId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("runId")
    @JoinColumn(name = "run_id", nullable = false)
    private GameRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProgressStatus status = ProgressStatus.OPEN;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    public QuestionProgress()
    {
    }

    public QuestionProgress(GameRun run, Question question, ProgressStatus status, LocalDateTime answeredAt)
    {
        this.id = new QuestionProgressId(run.getRunId(), question.getQuestionId());
        this.run = run;
        this.question = question;
        this.status = status;
        this.answeredAt = answeredAt;
    }

    public QuestionProgressId getId()
    {
        return id;
    }

    public void setId(QuestionProgressId id)
    {
        this.id = id;
    }

    public GameRun getRun()
    {
        return run;
    }

    public void setRun(GameRun run)
    {
        this.run = run;
    }

    public Question getQuestion()
    {
        return question;
    }

    public void setQuestion(Question question)
    {
        this.question = question;
    }

    public ProgressStatus getStatus()
    {
        return status;
    }

    public void setStatus(ProgressStatus status)
    {
        this.status = status;
    }

    public LocalDateTime getAnsweredAt()
    {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt)
    {
        this.answeredAt = answeredAt;
    }
}