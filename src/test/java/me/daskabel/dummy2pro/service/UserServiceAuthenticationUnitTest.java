package me.daskabel.dummy2pro.service;

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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unittests für Anmeldung, Authentifizierung und Avatar-Auflösung im
 * {@link UserService}.
 *
 * Geprüft werden erfolgreiche und fehlerhafte Login-Szenarien sowie die
 * Behandlung von Standard-Avataren und vorhandenen Avatar-Dateinamen.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceAuthenticationUnitTest
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
    void authenticate_success_returnsUserAndAppliesDefaultAvatar()
    {
        User user = new User("jan", "HASH", " ");
        user.setUserId(1L);

        when(userRepository.findByUsername("jan")).thenReturn(Optional.of(user));
        when(encoder.matches("SehrSicheresPass1!", "HASH")).thenReturn(true);

        User authenticated = userService.authenticate("jan", "SehrSicheresPass1!");

        assertEquals(1L, authenticated.getUserId());
        assertEquals("jan", authenticated.getUsername());
        assertEquals("duck.jpg", authenticated.getAvatar());
    }

    @Test
    void authenticate_unknownUser_throwsException()
    {
        when(userRepository.findByUsername("jan")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.authenticate("jan", "SehrSicheresPass1!")
        );

        assertEquals("Benutzername oder Passwort falsch.", ex.getMessage());
    }

    @Test
    void authenticate_wrongPassword_throwsException()
    {
        User user = new User("jan", "HASH", "duck.jpg");

        when(userRepository.findByUsername("jan")).thenReturn(Optional.of(user));
        when(encoder.matches("falsch", "HASH")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.authenticate("jan", "falsch")
        );

        assertEquals("Benutzername oder Passwort falsch.", ex.getMessage());
    }

    @Test
    void login_success_returnsTrue()
    {
        User user = new User("jan", "HASH", "duck.jpg");

        when(userRepository.findByUsername("jan")).thenReturn(Optional.of(user));
        when(encoder.matches("SehrSicheresPass1!", "HASH")).thenReturn(true);

        assertTrue(userService.login("jan", "SehrSicheresPass1!"));
    }

    @Test
    void login_wrongPassword_returnsFalse()
    {
        User user = new User("jan", "HASH", "duck.jpg");

        when(userRepository.findByUsername("jan")).thenReturn(Optional.of(user));
        when(encoder.matches("falsch", "HASH")).thenReturn(false);

        assertFalse(userService.login("jan", "falsch"));
    }

    @Test
    void login_blankUsername_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(" ", "abc")
        );

        assertEquals("Username darf nicht leer sein.", ex.getMessage());
    }

    @Test
    void login_blankPassword_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login("jan", " ")
        );

        assertEquals("Passwort darf nicht leer sein.", ex.getMessage());
    }

    @Test
    void resolveAvatarFilename_returnsDefaultForNullUser()
    {
        assertEquals("duck.jpg", userService.resolveAvatarFilename(null));
    }

    @Test
    void resolveAvatarFilename_returnsDefaultForBlankAvatar()
    {
        User user = new User("jan", "HASH", " ");

        assertEquals("duck.jpg", userService.resolveAvatarFilename(user));
    }

    @Test
    void resolveAvatarFilename_returnsExistingAvatar()
    {
        User user = new User("jan", "HASH", "bee.jpg");

        assertEquals("bee.jpg", userService.resolveAvatarFilename(user));
    }
}