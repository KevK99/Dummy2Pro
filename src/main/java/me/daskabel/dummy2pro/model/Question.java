package me.daskabel.dummy2pro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "question")
public class Question
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @Column(name = "question_set_id", nullable = false)
    private Integer questionSetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(name = "start_text", columnDefinition = "TEXT")
    private String startText;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "end_text", columnDefinition = "TEXT")
    private String endText;

    @Column(name = "allows_multiple", nullable = false)
    private boolean allowsMultiple = false;

    @Column(name = "points", nullable = false)
    private Integer points = 1;

    // --- Konstruktoren ---

    public Question()
    {
    }

    // --- Getter & Setter ---

    public String getEndText()
    {
        return endText;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public Integer getPoints()
    {
        return points;
    }

    public Integer getQuestionId()
    {
        return questionId;
    }

    public Integer getQuestionSetId()
    {
        return questionSetId;
    }

    public QuestionType getQuestionType()
    {
        return questionType;
    }

    public String getStartText()
    {
        return startText;
    }

    public boolean isAllowsMultiple()
    {
        return allowsMultiple;
    }

    public void setAllowsMultiple(boolean allowsMultiple)
    {
        this.allowsMultiple = allowsMultiple;
    }

    public void setEndText(String endText)
    {
        this.endText = endText;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public void setPoints(Integer points)
    {
        this.points = points;
    }

    public void setQuestionId(Integer questionId)
    {
        this.questionId = questionId;
    }

    public void setQuestionSetId(Integer questionSetId)
    {
        this.questionSetId = questionSetId;
    }

    public void setQuestionType(QuestionType questionType)
    {
        this.questionType = questionType;
    }

    public void setStartText(String startText)
    {
        this.startText = startText;
    }
}
