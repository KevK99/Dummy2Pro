package me.daskabel.dummy2pro.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

/**
 * Repräsentiert eine einzelne Frage des Quizsystems.
 *
 * Eine Frage gehört zu einem Fragensatz und kann je nach Fragetyp
 * Antwortoptionen, Lückenfelder und Theme-Zuordnungen besitzen.
 *
 * Das Modell ist bewusst generisch gehalten: MC-, TF- und GAP-Fragen werden
 * über dieselbe Entität abgebildet und unterscheiden sich vor allem durch
 * {@code questionType} sowie die jeweils belegten Beziehungen.
 */
@Entity
@Table(name = "question")
public class Question
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    /**
     * Fachliche Gruppierung der Frage innerhalb eines Fragensatzes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false)
    private QuestionSet questionSet;

    /**
     * Legt fest, wie die Frage dargestellt und ausgewertet wird.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    /**
     * Einleitung bzw. erster Textteil der Frage.
     *
     * Bei GAP-Fragen steht hier meist der Text vor den Lücken, bei anderen
     * Fragetypen der eigentliche Fragetext.
     */
    @Column(name = "start_text")
    private String startText;

    /**
     * Optionale Bildreferenz zur Frage.
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Zweiter Textteil der Frage.
     *
     * Relevant vor allem für mehrteilige Texte oder GAP-Fragen mit Text hinter
     * den Lücken.
     */
    @Column(name = "end_text")
    private String endText;

    /**
     * Kennzeichnet, ob bei MC-Fragen mehrere Antworten auswählbar sind.
     */
    @Column(name = "allows_multiple", nullable = false)
    private boolean allowsMultiple;

    /**
     * Punktwert der Frage bei vollständiger korrekter Beantwortung.
     */
    @Column(name = "points", nullable = false)
    private int points;

    /**
     * Antwortoptionen für MC- und TF-Fragen.
     *
     * Die Beziehung wird mit Cascade und Orphan-Removal geführt, damit die
     * Kindobjekte zusammen mit der Frage gepflegt werden können.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<AnswerOption> answerOptions;

    /**
     * Lückenfelder für GAP-Fragen.
     *
     * Die Reihenfolge wird über {@code gapIndex} stabil gehalten.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("gapIndex ASC")
    private Set<GapField> gapFields = new LinkedHashSet<>();

    /**
     * Thematische Zuordnung der Frage.
     *
     * Eine Frage kann mehreren Themes zugeordnet sein.
     */
    @ManyToMany
    @BatchSize(size = 100)
    @JoinTable(
            name = "question_theme",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    private List<Theme> themes;

    public Question()
    {
    }

    /**
     * Minimaler Konstruktor für programmatische Erzeugung mit Fragensatzbezug.
     */
    public Question(QuestionSet questionSet, QuestionType questionType, int points)
    {
        this.questionSet = questionSet;
        this.questionType = questionType;
        this.points = points;
    }

    /**
     * Komfortkonstruktor für die direkte Initialisierung der zentralen Felder.
     */
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

    public Set<GapField> getGapFields()
    {
        return gapFields;
    }

    public void setGapFields(Set<GapField> gapFields)
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
