package me.daskabel.dummy2pro.model;

/**
 * Repräsentiert einen registrierten User (also den Account) des Spiels.
 * Der {@code username} dient als eindeutige Kennung (Login + Anzeige).
 * Das Passwort wird ausschließlich als Hash gespeichert.
 */

public class User {
    /** Eindeutige ID aus der Datenbank (wird später automatisch vergeben). */
    private Long id;

    /** Eindeutiger Anzeigename, welcher auch zum Login verwendet wird */
    private String username;

    /** Gehashtes Passwort. Passwörter sollten nie Cleartext zu sehen sein */
    private String passwordHash;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
