package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;

/**
 * Repräsentiert eine auswählbare Antwortmöglichkeit für eine einzelne Lücke.
 *
 * Gespeichert werden der Antworttext, die Reihenfolge innerhalb der Lücke
 * und die Information, ob die Option korrekt ist.
 */
@Entity
@Table(name = "gap_option")
public class GapOption
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gap_option_id")
    private Long gapOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gap_id", nullable = false)
    private GapField gapField;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @Column(name = "option_order")
    private int optionOrder;

    // Constructors
    public GapOption()
    {
    }

    public GapOption(GapField gapField, String optionText, boolean isCorrect, int optionOrder)
    {
        this.gapField = gapField;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
        this.optionOrder = optionOrder;
    }

    // Getters & Setters
    public Long getGapOptionId()
    {
        return gapOptionId;
    }

    public void setGapOptionId(Long gapOptionId)
    {
        this.gapOptionId = gapOptionId;
    }

    public GapField getGapField()
    {
        return gapField;
    }

    public void setGapField(GapField gapField)
    {
        this.gapField = gapField;
    }

    public String getOptionText()
    {
        return optionText;
    }

    public void setOptionText(String optionText)
    {
        this.optionText = optionText;
    }

    public boolean getIsCorrect()
    {
        return isCorrect;
    }

    public void setIsCorrect(boolean isCorrect)
    {
        this.isCorrect = isCorrect;
    }

    public int getOptionOrder()
    {
        return optionOrder;
    }

    public void setOptionOrder(int optionOrder)
    {
        this.optionOrder = optionOrder;
    }
}
