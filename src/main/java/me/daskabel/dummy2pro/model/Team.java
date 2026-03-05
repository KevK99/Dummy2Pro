package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_ID")
    private Long teamId;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionSet> questionSets;

    // Constructors
    public Team() {}

    public Team(String name) {
        this.name = name;
    }

    // Getters & Setters
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<QuestionSet> getQuestionSets() { return questionSets; }
    public void setQuestionSets(List<QuestionSet> questionSets) { this.questionSets = questionSets; }
}
