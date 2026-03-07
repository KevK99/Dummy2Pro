package me.daskabel.dummy2pro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RunGapAnswerId implements Serializable
{
    @Column(name = "run_id")
    private Long runId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "gap_id")
    private Long gapId;

    public RunGapAnswerId()
    {
    }

    public RunGapAnswerId(Long runId, Long questionId, Long gapId)
    {
        this.runId = runId;
        this.questionId = questionId;
        this.gapId = gapId;
    }

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public Long getGapId()
    {
        return gapId;
    }

    public void setGapId(Long gapId)
    {
        this.gapId = gapId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RunGapAnswerId that)) return false;
        return Objects.equals(runId, that.runId)
                && Objects.equals(questionId, that.questionId)
                && Objects.equals(gapId, that.gapId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(runId, questionId, gapId);
    }
}