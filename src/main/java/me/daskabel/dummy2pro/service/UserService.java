package me.daskabel.dummy2pro.service;

import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Erstellen eines neuen Users,
 * prüft Passwortregeln und erstellt einen Hash (BCrypt, weil das der richtige Standard ist und sicherer als normales Hashen).
 */

@Service
public class UserService {

    private final BCryptPasswordEncoder encoder;
    private final UserRepository userRepository;

    public UserService(BCryptPasswordEncoder encoder, UserRepository userRepository) {
        this.encoder = encoder;
        this.userRepository = userRepository;
    }

    public User register(String username, String password) {
        validateUsername(username);
        validatePassword(password);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username ist bereits vergeben.");
        }

        String passwordHash = encoder.encode(password);
        User user = new User(username, passwordHash);

        return userRepository.save(user);
    }

    public boolean login(String username, String password) {
        validateLoginInput(username, password);

        return userRepository.findByUsername(username)
                .map(u -> encoder.matches(password, u.getPasswordHash()))
                .orElse(false);
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

    // Regeln für das Passwort: mit sinnvollen Kriterien den Nutzer zu sichereren Passwörtern zwingen
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

        if (!hasUpper || !hasLower || !hasDigit || !hasSign || hasWhitespace) {
            throw new IllegalArgumentException(
                    "Passwort muss Groß-, Kleinbuchstaben, eine Zahl und ein Sonderzeichen enthalten. " +
                            "Es darf kein Leerzeichen benutzt werden!"
            );
        }
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