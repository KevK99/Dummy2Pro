package me.daskabel.dummy2pro.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.repository.UserRepository;

/**
 * Erstellen eines neuen Users, prüft Passwortregeln und erstellt einen Hash (BCrypt, weil das der richtige Standard ist
 * und sicherer als normales Hashen).
 */

@Service
public class UserService
{
    private static final String DEFAULT_AVATAR = "duck.jpg";

    private static final Set<String> ALLOWED_AVATARS = Set.of(
            "Alf.png",
            "bee.jpg",
            "beee.jpg",
            "beesleep.jpg",
            "block.jpg",
            "catBlack.PNG",
            "catRed.PNG",
            "catWhiteShadow.PNG",
            "duck.jpg",
            "flowers.jpg",
            "hase.jpg",
            "hase.PNG",
            "Herberg.PNG",
            "lama.png",
            "maus.jpg",
            "mond.jpg",
            "panda.PNG",
            "pingwin.jpg",
            "subi.PNG",
            "sunflower.PNG",
            "sunflowerCursed.PNG"
    );

    private final BCryptPasswordEncoder encoder;
    private final UserRepository userRepository;
    private final GameRunRepository gameRunRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final RunSelectedAnswerRepository runSelectedAnswerRepository;
    private final RunGapAnswerRepository runGapAnswerRepository;

    public UserService(
            BCryptPasswordEncoder encoder,
            UserRepository userRepository,
            GameRunRepository gameRunRepository,
            QuestionProgressRepository questionProgressRepository,
            RunSelectedAnswerRepository runSelectedAnswerRepository,
            RunGapAnswerRepository runGapAnswerRepository)
    {
        this.encoder = encoder;
        this.userRepository = userRepository;
        this.gameRunRepository = gameRunRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.runSelectedAnswerRepository = runSelectedAnswerRepository;
        this.runGapAnswerRepository = runGapAnswerRepository;
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

        applyDefaultAvatarIfMissing(user);
        return user;
    }

    public User getUser(Long userId)
    {
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Benutzer nicht gefunden"));

        applyDefaultAvatarIfMissing(user);
        return user;
    }

    public void deleteUser(Long userId)
    {
        User user =
                this.userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("Benutzer nicht gefunden"));

        // Lösche alle Spielstände des Benutzers
        List<GameRun> gameRuns = this.gameRunRepository.findByUser_UserId(userId);
        for (GameRun run : gameRuns)
        {
            this.runSelectedAnswerRepository.deleteByRun_RunId(run.getRunId());
            this.runGapAnswerRepository.deleteByRun_RunId(run.getRunId());
            this.questionProgressRepository.deleteByRun_RunId(run.getRunId());
        }
        this.gameRunRepository.deleteAll(gameRuns);

        // Lösche den Benutzer
        this.userRepository.delete(user);
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
        User user = new User(username, passwordHash, DEFAULT_AVATAR);

        return userRepository.save(user);
    }

    public User updateAvatar(Long userId, String avatar)
    {
        if (avatar == null || avatar.isBlank())
        {
            throw new IllegalArgumentException("Es wurde kein Avatar ausgewählt.");
        }

        if (!ALLOWED_AVATARS.contains(avatar))
        {
            throw new IllegalArgumentException("Das ausgewählte Profilbild ist nicht erlaubt.");
        }

        User user = getUser(userId);
        user.setAvatar(avatar);
        return this.userRepository.save(user);
    }

    public User updateUsername(Long userId, String newUsername)
    {
        validateUsername(newUsername);

        User user = getUser(userId);

        boolean usernameAlreadyUsed = this.userRepository.existsByUsername(newUsername);
        if (usernameAlreadyUsed && !newUsername.equals(user.getUsername()))
        {
            throw new IllegalArgumentException("Username ist bereits vergeben.");
        }

        user.setUsername(newUsername);
        return this.userRepository.save(user);
    }

    public void updatePassword(Long userId, String currentPassword, String newPassword, String newPasswordConfirm)
    {
        if (currentPassword == null || currentPassword.isBlank())
        {
            throw new IllegalArgumentException("Aktuelles Passwort darf nicht leer sein.");
        }

        if (newPassword == null || newPasswordConfirm == null || !newPassword.equals(newPasswordConfirm))
        {
            throw new IllegalArgumentException("Die neuen Passwörter stimmen nicht überein.");
        }

        User user = getUser(userId);

        if (!this.encoder.matches(currentPassword, user.getPasswordHash()))
        {
            throw new IllegalArgumentException("Das aktuelle Passwort ist falsch.");
        }

        validatePassword(newPassword);

        if (this.encoder.matches(newPassword, user.getPasswordHash()))
        {
            throw new IllegalArgumentException("Das neue Passwort muss sich vom alten unterscheiden.");
        }

        user.setPasswordHash(this.encoder.encode(newPassword));
        this.userRepository.save(user);
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

    public String resolveAvatarFilename(User user)
    {
        if (user == null || user.getAvatar() == null || user.getAvatar().isBlank())
        {
            return DEFAULT_AVATAR;
        }

        return user.getAvatar();
    }

    private void applyDefaultAvatarIfMissing(User user)
    {
        if (user == null)
        {
            return;
        }

        if (user.getAvatar() == null || user.getAvatar().isBlank())
        {
            user.setAvatar(DEFAULT_AVATAR);
        }
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