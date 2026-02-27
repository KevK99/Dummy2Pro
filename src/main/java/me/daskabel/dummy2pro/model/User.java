package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;

/**
 * Repräsentiert einen registrierten User (also den Account) des Spiels.
 * Der {@code username} dient als eindeutige Kennung (Login + Anzeige).
 * Das Passwort wird ausschließlich als Hash gespeichert.
 */

@Entity
@Table(name = "users")
public class User {
    /** Eindeutige ID aus der Datenbank (wird später automatisch vergeben). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")          // passt zu deiner DB-Spalte
    private Long id;

    /** Eindeutiger Anzeigename, welcher auch zum Login verwendet wird */
    @Column(name = "username", nullable = false, unique = true, length = 30)
    private String username;

    /** Gehashtes Passwort. Passwörter sollten nie Cleartext zu sehen sein */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    protected User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
