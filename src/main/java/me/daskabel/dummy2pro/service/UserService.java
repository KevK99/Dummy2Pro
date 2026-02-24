package me.daskabel.dummy2pro.service;

import me.daskabel.dummy2pro.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Erstellen eines neuen Users,
 * prüft Passwortregeln und erstellt einen Hash (BCrypt, weil das der richtige Standard ist und sicherer als normales Hashen).
 */

@Service
public class UserService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public User register(String username, String password) {
        validateUsername(username);
        validatePassword(password);

        String passwordHash = encoder.encode(password);
        return new User(username, passwordHash);
    }

    public boolean matches(String rawPassword, String storedHash) {
        return encoder.matches(rawPassword, storedHash);
    }

    // Regeln für den Usernamen
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username darf nicht leer sein.");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username muss mindestens 3 Zeichen lang sein.");
        }
        if (username.length() > 30) {
            throw new IllegalArgumentException("Username ist zu lang. Er darf nur 30 Zeichen lang sein.");
        }
    }

    // Regeln für das Passwort
    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Passwort darf nicht leer sein.");
        }
        if (password.length() < 13) {
            throw new IllegalArgumentException("Passwort muss mindestens 13 Zeichen lang sein.");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSign = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch));
        boolean hasWhitespace = password.chars().anyMatch(Character::isWhitespace);

        if (!hasUpper || !hasLower || !hasDigit  || !hasSign || hasWhitespace) {
            throw new IllegalArgumentException("Passwort muss Groß-, Kleinbuchstaben, eine Zahl und ein Sonderzeichen enthalten. Es darf kein Leerzeichen benutzt werden!");
        }
    }

    /**
     * Übergangs-Login ohne Datenbank (In-Memory).
     * TODO: durch DB-Login ersetzen!
     */
    public boolean loginInMemory(String username, String password) {
        validateLoginInput(username, password);

        final String demoUsername = "Test";
        final String demoHash = "$2a$10$BzsrNXld8dbvPX7D8gUpAe0qa21gfyj9AKHYtBMWPKmO3nKmBA8QC";

        if (!demoUsername.equals(username)) {
            return false;
        }
        return encoder.matches(password, demoHash);
    }

    private void validateLoginInput(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username darf nicht leer sein.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Passwort darf nicht leer sein.");
        }
    }

}