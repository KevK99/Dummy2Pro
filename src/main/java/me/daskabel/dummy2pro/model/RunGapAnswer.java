package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Speichert die ausgewählte Antwort zu einer einzelnen Lücke innerhalb
 * eines Spielstands.
 *
 * Damit kann für jede Lücke einer Frage nachvollzogen werden, welche
 * Option gewählt wurde und wann die Antwort erfolgt ist.
 */
@Entity
@Table(name = "run_gap_answer")
public class RunGapAnswer
{
    @EmbeddedId
    private RunGapAnswerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("runId")
    @JoinColumn(name = "run_id", nullable = false)
    private GameRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gapId")
    @JoinColumn(name = "gap_id", nullable = false)
    private GapField gapField;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_gap_option_id", nullable = false)
    private GapOption selectedGapOption;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    public RunGapAnswer()
    {
    }

    public RunGapAnswer(GameRun run, Question question, GapField gapField,
                        GapOption selectedGapOption, LocalDateTime answeredAt)
    {
        this.id = new RunGapAnswerId(run.getRunId(), question.getQuestionId(), gapField.getGapId());
        this.run = run;
        this.question = question;
        this.gapField = gapField;
        this.selectedGapOption = selectedGapOption;
        this.answeredAt = answeredAt;
    }

    public RunGapAnswerId getId()
    {
        return id;
    }

    public void setId(RunGapAnswerId id)
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

    public GapField getGapField()
    {
        return gapField;
    }

    public void setGapField(GapField gapField)
    {
        this.gapField = gapField;
    }

    public GapOption getSelectedGapOption()
    {
        return selectedGapOption;
    }

    public void setSelectedGapOption(GapOption selectedGapOption)
    {
        this.selectedGapOption = selectedGapOption;
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
