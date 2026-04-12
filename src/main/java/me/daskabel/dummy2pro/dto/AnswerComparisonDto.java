package me.daskabel.dummy2pro.dto;

import java.util.List;

/**
 * Enthält den Vergleich zwischen ausgewählten und richtigen Antworten
 * zu einer Frage.
 */
public class AnswerComparisonDto
{
    private Long questionId;
    private List<Long> selectedAnswerIds; // Player's answers
    private List<Long> correctAnswerIds; // Correct answers

    public AnswerComparisonDto()
    {
    }

    public List<Long> getCorrectAnswerIds()
    {
        return correctAnswerIds;
    }

    // Getters and Setters
    public Long getQuestionId()
    {
        return questionId;
    }

    public List<Long> getSelectedAnswerIds()
    {
        return selectedAnswerIds;
    }

    public void setCorrectAnswerIds(List<Long> correctAnswerIds)
    {
        this.correctAnswerIds = correctAnswerIds;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public void setSelectedAnswerIds(List<Long> selectedAnswerIds)
    {
        this.selectedAnswerIds = selectedAnswerIds;
    }
}
