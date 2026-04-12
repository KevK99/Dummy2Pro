package me.daskabel.dummy2pro.service;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unittests für die Registrierung im {@link UserService}.
 *
 * Geprüft werden erfolgreiche Registrierungen sowie die fachlichen
 * Validierungsregeln für Benutzername und Passwort, darunter Länge,
 * Zeichentypen, Leerzeichen und doppelte Benutzernamen.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceRegistrationUnitTest
{
    @Mock
    private BCryptPasswordEncoder encoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GameRunRepository gameRunRepository;
    @Mock
    private QuestionProgressRepository questionProgressRepository;
    @Mock
    private RunSelectedAnswerRepository runSelectedAnswerRepository;
    @Mock
    private RunGapAnswerRepository runGapAnswerRepository;

    private UserService userService;

    @BeforeEach
    void setUp()
    {
        userService = new UserService(
                encoder,
                userRepository,
                gameRunRepository,
                questionProgressRepository,
                runSelectedAnswerRepository,
                runGapAnswerRepository
        );
    }

    @Test
    void register_success_savesUserAndInitialRun()
    {
        when(userRepository.existsByUsername("jan")).thenReturn(false);
        when(encoder.encode("SehrSicheresPass1!")).thenReturn("HASH");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(11L);
            return user;
        });

        when(gameRunRepository.save(any(GameRun.class))).thenAnswer(invocation -> {
            GameRun run = invocation.getArgument(0);
            run.setRunId(21L);
            return run;
        });

        User saved = userService.register("jan", "SehrSicheresPass1!");

        assertNotNull(saved);
        assertEquals(11L, saved.getUserId());
        assertEquals("jan", saved.getUsername());
        assertEquals("duck.jpg", saved.getAvatar());

        verify(userRepository).save(any(User.class));
        verify(gameRunRepository).save(any(GameRun.class));
    }

    @Test
    void register_duplicateUsername_throwsException()
    {
        when(userRepository.existsByUsername("jan")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("jan", "SehrSicheresPass1!")
        );

        assertEquals("Username ist bereits vergeben.", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(gameRunRepository, never()).save(any());
    }

    @Test
    void register_blankUsername_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(" ", "SehrSicheresPass1!")
        );

        assertEquals("Username darf nicht leer sein.", ex.getMessage());
    }

    @Test
    void register_tooShortUsername_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("ab", "SehrSicheresPass1!")
        );

        assertEquals("Username muss mindestens 3 Zeichen lang sein.", ex.getMessage());
    }

    @Test
    void register_tooLongUsername_throwsException()
    {
        String username = "a".repeat(31);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(username, "SehrSicheresPass1!")
        );

        assertEquals("Username ist zu lang. Er darf nur 30 Zeichen lang sein.", ex.getMessage());
    }

    @Test
    void register_passwordTooShort_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("jan", "Aa1!kurz")
        );

        assertEquals("Passwort muss mindestens 13 Zeichen lang sein.", ex.getMessage());
    }

    @Test
    void register_passwordWithoutUpperCase_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("jan", "kleinundzahl1!")
        );

        assertTrue(ex.getMessage().contains("Passwort muss Groß-, Kleinbuchstaben"));
    }

    @Test
    void register_passwordWithoutLowerCase_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("jan", "NURGROSS1234!")
        );

        assertTrue(ex.getMessage().contains("Passwort muss Groß-, Kleinbuchstaben"));
    }

    @Test
    void register_passwordWithoutDigit_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("jan", "OhneZahlAberMit!")
        );

        assertTrue(ex.getMessage().contains("Passwort muss Groß-, Kleinbuchstaben"));
    }

    @Test
    void register_passwordWithoutSpecialCharacter_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("jan", "OhneSonder123")
        );

        assertTrue(ex.getMessage().contains("Passwort muss Groß-, Kleinbuchstaben"));
    }

    @Test
    void register_passwordWithWhitespace_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("jan", "Mit Leerzeichen1!")
        );

        assertTrue(ex.getMessage().contains("Es darf kein Leerzeichen benutzt werden"));
    }
}