package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Repräsentiert einen registrierten User (also den Account) des Spiels.
 * Der {@code username} dient als eindeutige Kennung (Login + Anzeige).
 * Das Passwort wird ausschließlich als Hash gespeichert.
 */

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserQuestionProgress> questionProgress;

    // Constructors
    public User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswortHash(String passwordHash) { this.passwordHash = passwordHash; }

    public List<UserQuestionProgress> getQuestionProgress() { return questionProgress; }
    public void setQuestionProgress(List<UserQuestionProgress> questionProgress) { this.questionProgress = questionProgress; }
}
