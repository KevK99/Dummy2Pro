package me.daskabel.dummy2pro.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * Repräsentiert ein Theme beziehungsweise Themengebiet im Quiz.
 *
 * Ein Theme besitzt einen Namen, eine Beschreibung und kann mehreren
 * Fragen zugeordnet sein.
 */
@Entity
@Table(name = "theme")
public class Theme
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_id")
    private Long themeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToMany(mappedBy = "themes")
    private List<Question> questions;

    // Constructors
    public Theme()
    {
    }

    public Theme(String name)
    {
        this.name = name;
    }

    public Theme(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public String getName()
    {
        return name;
    }

    public List<Question> getQuestions()
    {
        return questions;
    }

    // Getters & Setters
    public Long getThemeId()
    {
        return themeId;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setQuestions(List<Question> questions)
    {
        this.questions = questions;
    }

    public void setThemeId(Long themeId)
    {
        this.themeId = themeId;
    }
}