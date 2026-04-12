package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.util.List;

/**
 * Repräsentiert ein Team, das Fragensätze erstellt oder verwaltet.
 *
 * Ein Team besitzt einen Namen und kann mehreren Fragensätzen
 * zugeordnet sein.
 */
@Entity
@Table(name = "team")
public class Team
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionSet> questionSets;

    public Team()
    {
    }

    public Team(String name)
    {
        this.name = name;
    }

    public Long getTeamId()
    {
        return teamId;
    }

    public void setTeamId(Long teamId)
    {
        this.teamId = teamId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<QuestionSet> getQuestionSets()
    {
        return questionSets;
    }

    public void setQuestionSets(List<QuestionSet> questionSets)
    {
        this.questionSets = questionSets;
    }
}