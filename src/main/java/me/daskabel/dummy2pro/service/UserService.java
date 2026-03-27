package me.daskabel.dummy2pro.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.UserRepository;

/**
 * Erstellen eines neuen Users, prüft Passwortregeln und erstellt einen Hash (BCrypt, weil das der richtige Standard ist
 * und sicherer als normales Hashen).
 */

@Service
public class UserService
{

    private final BCryptPasswordEncoder encoder;
    private final UserRepository userRepository;
    private final GameRunRepository gameRunRepository;
    private final QuestionProgressRepository questionProgressRepository;

    public UserService(BCryptPasswordEncoder encoder, UserRepository userRepository,
        GameRunRepository gameRunRepository, QuestionProgressRepository questionProgressRepository)
    {
        this.encoder = encoder;
        this.userRepository = userRepository;
        this.gameRunRepository = gameRunRepository;
        this.questionProgressRepository = questionProgressRepository;
    }

    public User authenticate(String username, String password)
    {
        validateLoginInput(username, password);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Benutzername oder Passwort falsch."));

        if (!encoder.matches(password, user.getPasswordHash()))
        {
            throw new IllegalArgumentException("Benutzername oder Passwort falsch.");
        }

        return user;
    }

    public void deleteUser(Long userId)
    {
        User user =
            userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("Benutzer nicht gefunden"));

        // Lösche alle Spielstände des Benutzers
        List<GameRun> gameRuns = gameRunRepository.findByUser_UserId(userId);
        for (GameRun run : gameRuns)
        {
            questionProgressRepository.deleteByRun_RunId(run.getRunId());
        }
        gameRunRepository.deleteAll(gameRuns);

        // Lösche den Benutzer
        userRepository.delete(user);
    }

    public boolean login(String username, String password)
    {
        validateLoginInput(username, password);

        return userRepository.findByUsername(username)
            .map(u -> encoder.matches(password, u.getPasswordHash()))
            .orElse(false);
    }

    public User register(String username, String password)
    {
        validateUsername(username);
        validatePassword(password);

        if (userRepository.existsByUsername(username))
        {
            throw new IllegalArgumentException("Username ist bereits vergeben.");
        }

        String passwordHash = encoder.encode(password);
        User user = new User(username, passwordHash);

        return userRepository.save(user);
    }

    public void saveCurrentGameProgress(Long userId)
    {
        // Hier kannst du die Logik implementieren, um den aktuellen Spielstand zu speichern.
        // Das könnte das Speichern von `GameRun`-Objekten und deren Fortschritt beinhalten.
        GameRun run = gameRunRepository.findTopByUser_UserIdOrderByStartedAtDesc(userId)
            .orElseThrow(() -> new NoSuchElementException("Kein aktiver Spielstand gefunden."));

        // Speichere den Fortschritt, falls erforderlich
        // Hier musst du möglicherweise den Fortschritt des Spiels speichern, bevor der Benutzer sich abmeldet.
        // Beispiel:
        List<QuestionProgress> progressList = questionProgressRepository.findByRun_RunId(run.getRunId());
        // Speichere Fortschritte oder führe hier eine spezifische Logik aus.
    }

    private void validateLoginInput(String username, String password)
    {
        if (username == null || username.isBlank())
        {
            throw new IllegalArgumentException("Username darf nicht leer sein.");
        }
        if (password == null || password.isBlank())
        {
            throw new IllegalArgumentException("Passwort darf nicht leer sein.");
        }
    }

    // Regeln für das Passwort: mit sinnvollen Kriterien den Nutzer zu sichereren Passwörtern zwingen
    private void validatePassword(String password)
    {
        if (password == null || password.isBlank())
        {
            throw new IllegalArgumentException("Passwort darf nicht leer sein.");
        }
        if (password.length() < 13)
        {
            throw new IllegalArgumentException("Passwort muss mindestens 13 Zeichen lang sein.");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSign =
            password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch));
        boolean hasWhitespace = password.chars().anyMatch(Character::isWhitespace);

        if (!hasUpper || !hasLower || !hasDigit || !hasSign || hasWhitespace)
        {
            throw new IllegalArgumentException(
                "Passwort muss Groß-, Kleinbuchstaben, eine Zahl und ein Sonderzeichen enthalten. "
                    + "Es darf kein Leerzeichen benutzt werden!");
        }
    }

    // Regeln für den Usernamen
    private void validateUsername(String username)
    {
        if (username == null || username.isBlank())
        {
            throw new IllegalArgumentException("Username darf nicht leer sein.");
        }
        if (username.length() < 3)
        {
            throw new IllegalArgumentException("Username muss mindestens 3 Zeichen lang sein.");
        }
        if (username.length() > 30)
        {
            throw new IllegalArgumentException("Username ist zu lang. Er darf nur 30 Zeichen lang sein.");
        }
    }
}
