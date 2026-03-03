package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false)
    private QuestionSet questionSet;

    /**
     * Type of question: MC = Multiple Choice, TF = True/False, GAP = Gap Fill
     */
    @Column(name = "question_type", nullable = false, length = 10)
    private String questionType;

    @Column(name = "start_text", columnDefinition = "TEXT")
    private String startText;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "end_text", columnDefinition = "TEXT")
    private String endText;

    @Column(name = "allows_multiple")
    private Boolean allowsMultiple;

    @Column(name = "points")
    private Integer points;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<McAnswer> mcAnswers;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GapField> gapFields;

    @ManyToMany
    @JoinTable(
        name = "QUESTION_THEME",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    private List<Theme> themes;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserQuestionProgress> userProgress;

    // Constructors
    public Question() {}

    public Question(QuestionSet questionSet, String questionType, Integer points) {
        this.questionSet = questionSet;
        this.questionType = questionType;
        this.points = points;
    }

    // Getters & Setters
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public QuestionSet getQuestionSet() { return questionSet; }
    public void setQuestionSet(QuestionSet questionSet) { this.questionSet = questionSet; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public String getStartText() { return startText; }
    public void setStartText(String startText) { this.startText = startText; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getEndText() { return endText; }
    public void setEndText(String endText) { this.endText = endText; }

    public Boolean getAllowsMultiple() { return allowsMultiple; }
    public void setAllowsMultiple(Boolean allowsMultiple) { this.allowsMultiple = allowsMultiple; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public List<McAnswer> getMcAnswers() { return mcAnswers; }
    public void setMcAnswers(List<McAnswer> mcAnswers) { this.mcAnswers = mcAnswers; }

    public List<GapField> getGapFields() { return gapFields; }
    public void setGapFields(List<GapField> gapFields) { this.gapFields = gapFields; }

    public List<Theme> getThemes() { return themes; }
    public void setThemes(List<Theme> themes) { this.themes = themes; }

    public List<UserQuestionProgress> getUserProgress() { return userProgress; }
    public void setUserProgress(List<UserQuestionProgress> userProgress) { this.userProgress = userProgress; }
}
