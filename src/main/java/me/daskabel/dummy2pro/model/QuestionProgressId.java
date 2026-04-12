package me.daskabel.dummy2pro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Zusammengesetzter Schlüssel für den Bearbeitungsstand einer Frage.
 *
 * Der Schlüssel besteht aus Spielstand und Frage, damit jede Frage
 * pro Spielstand genau einen Bearbeitungsstand besitzt.
 */
@Embeddable
public class QuestionProgressId implements Serializable
{
    @Column(name = "run_id")
    private Long runId;

    @Column(name = "question_id")
    private Long questionId;

    public QuestionProgressId()
    {
    }

    public QuestionProgressId(Long runId, Long questionId)
    {
        this.runId = runId;
        this.questionId = questionId;
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

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof QuestionProgressId that))
        {
            return false;
        }
        return Objects.equals(runId, that.runId)
                && Objects.equals(questionId, that.questionId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(runId, questionId);
    }
}
