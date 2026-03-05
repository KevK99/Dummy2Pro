package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "passwort_hash", nullable = false)
    private String passwortHash;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserQuestionProgress> questionProgress;

    // Constructors
    public User() {}

    public User(String username, String passwortHash) {
        this.username = username;
        this.passwortHash = passwortHash;
    }

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswortHash() { return passwortHash; }
    public void setPasswortHash(String passwortHash) { this.passwortHash = passwortHash; }

    public List<UserQuestionProgress> getQuestionProgress() { return questionProgress; }
    public void setQuestionProgress(List<UserQuestionProgress> questionProgress) { this.questionProgress = questionProgress; }
}
