package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Repräsentiert eine einzelne Lücke innerhalb einer Lückentextfrage.
 *
 * Gespeichert werden die Position der Lücke, der Text davor und danach
 * sowie die zugehörigen Auswahlmöglichkeiten.
 */
@Entity
@Table(name = "gap_field")
public class GapField
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gap_id")
    private Long gapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "gap_index", nullable = false)
    private int gapIndex;

    @Column(name = "text_before", columnDefinition = "TEXT")
    private String textBefore;

    @Column(name = "text_after", columnDefinition = "TEXT")
    private String textAfter;

    @OneToMany(mappedBy = "gapField", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("optionOrder ASC")
    private Set<GapOption> gapOptions = new LinkedHashSet<>();

    // Constructors
    public GapField()
    {
    }

    public GapField(Question question, int gapIndex)
    {
        this.question = question;
        this.gapIndex = gapIndex;
    }

    // Getters & Setters
    public Long getGapId()
    {
        return gapId;
    }

    public void setGapId(Long gapId)
    {
        this.gapId = gapId;
    }

    public Question getQuestion()
    {
        return question;
    }

    public void setQuestion(Question question)
    {
        this.question = question;
    }

    public int getGapIndex()
    {
        return gapIndex;
    }

    public void setGapIndex(int gapIndex)
    {
        this.gapIndex = gapIndex;
    }

    public String getTextBefore()
    {
        return textBefore;
    }

    public void setTextBefore(String textBefore)
    {
        this.textBefore = textBefore;
    }

    public String getTextAfter()
    {
        return textAfter;
    }

    public void setTextAfter(String textAfter)
    {
        this.textAfter = textAfter;
    }

    public Set<GapOption> getGapOptions()
    {
        return gapOptions;
    }

    public void setGapOptions(Set<GapOption> gapOptions)
    {
        this.gapOptions = gapOptions;
    }
}
