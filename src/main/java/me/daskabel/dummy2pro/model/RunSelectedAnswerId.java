package me.daskabel.dummy2pro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RunSelectedAnswerId implements Serializable
{
    @Column(name = "run_id")
    private Long runId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "answer_id")
    private Long answerId;

    public RunSelectedAnswerId()
    {
    }

    public RunSelectedAnswerId(Long runId, Long questionId, Long answerId)
    {
        this.runId = runId;
        this.questionId = questionId;
        this.answerId = answerId;
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

    public Long getAnswerId()
    {
        return answerId;
    }

    public void setAnswerId(Long answerId)
    {
        this.answerId = answerId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RunSelectedAnswerId that)) return false;
        return Objects.equals(runId, that.runId)
                && Objects.equals(questionId, that.questionId)
                && Objects.equals(answerId, that.answerId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(runId, questionId, answerId);
    }
}