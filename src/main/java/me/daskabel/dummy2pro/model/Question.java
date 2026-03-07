package me.daskabel.dummy2pro.model;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "question")
public class Question
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false)
    private QuestionSet questionSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(name = "start_text")
    private String startText;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "end_text")
    private String endText;

    @Column(name = "allows_multiple", nullable = false)
    private boolean allowsMultiple;

    @Column(name = "points", nullable = false)
    private int points;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerOption> answerOptions;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GapField> gapFields;

    @ManyToMany
    @JoinTable(
            name = "Question_Theme",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    private List<Theme> themes;

    public Question()
    {
    }

    public Question(QuestionSet questionSet, QuestionType questionType, int points)
    {
        this.questionSet = questionSet;
        this.questionType = questionType;
        this.points = points;
    }

    public Question(QuestionType questionType, String startText, String imageUrl, String endText,
                    boolean allowsMultiple, int points)
    {
        this.questionType = questionType;
        this.startText = startText;
        this.imageUrl = imageUrl;
        this.endText = endText;
        this.allowsMultiple = allowsMultiple;
        this.points = points;
    }

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public QuestionSet getQuestionSet()
    {
        return questionSet;
    }

    public void setQuestionSet(QuestionSet questionSet)
    {
        this.questionSet = questionSet;
    }

    public QuestionType getQuestionType()
    {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType)
    {
        this.questionType = questionType;
    }

    public String getStartText()
    {
        return startText;
    }

    public void setStartText(String startText)
    {
        this.startText = startText;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getEndText()
    {
        return endText;
    }

    public void setEndText(String endText)
    {
        this.endText = endText;
    }

    public boolean getAllowsMultiple()
    {
        return allowsMultiple;
    }

    public void setAllowsMultiple(boolean allowsMultiple)
    {
        this.allowsMultiple = allowsMultiple;
    }

    public int getPoints()
    {
        return points;
    }

    public void setPoints(int points)
    {
        this.points = points;
    }

    public List<AnswerOption> getAnswerOptions()
    {
        return answerOptions;
    }

    public void setAnswerOptions(List<AnswerOption> answerOptions)
    {
        this.answerOptions = answerOptions;
    }

    public List<GapField> getGapFields()
    {
        return gapFields;
    }

    public void setGapFields(List<GapField> gapFields)
    {
        this.gapFields = gapFields;
    }

    public List<Theme> getThemes()
    {
        return themes;
    }

    public void setThemes(List<Theme> themes)
    {
        this.themes = themes;
    }
}