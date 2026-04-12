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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unittests für Profiländerungen und Kontolöschung im {@link UserService}.
 *
 * Die Tests decken Namensänderung, Avatarwechsel, Passwortänderung und das
 * vollständige Entfernen eines Benutzers mitsamt abhängigen Spiel- und
 * Fortschrittsdaten ab.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceProfileUnitTest
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
    void updateUsername_success_savesChangedUser()
    {
        User user = new User("alt", "HASH", "duck.jpg");
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("neu")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUsername(1L, "neu");

        assertEquals("neu", updated.getUsername());
        verify(userRepository).save(user);
    }

    @Test
    void updateUsername_duplicateFromAnotherUser_throwsException()
    {
        User user = new User("alt", "HASH", "duck.jpg");
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("belegt")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUsername(1L, "belegt")
        );

        assertEquals("Username ist bereits vergeben.", ex.getMessage());
    }

    @Test
    void updateAvatar_blankAvatar_resetsToDefault()
    {
        User user = new User("jan", "HASH", "bee.jpg");
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateAvatar(1L, " ");

        assertEquals("duck.jpg", updated.getAvatar());
        verify(userRepository).save(user);
    }

    @Test
    void updateAvatar_invalidAvatar_throwsException()
    {
        User user = new User("jan", "HASH", "bee.jpg");
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateAvatar(1L, "nicht-erlaubt.png")
        );

        assertEquals("Ungültiger Avatar.", ex.getMessage());
    }

    @Test
    void updatePassword_success_updatesHash()
    {
        User user = new User("jan", "OLD_HASH", "duck.jpg");
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(encoder.matches("AktuellesPasswort1!", "OLD_HASH")).thenReturn(true);
        when(encoder.matches("NeuesPasswort123!", "OLD_HASH")).thenReturn(false);
        when(encoder.encode("NeuesPasswort123!")).thenReturn("NEW_HASH");

        userService.updatePassword(1L, "AktuellesPasswort1!", "NeuesPasswort123!", "NeuesPasswort123!");

        assertEquals("NEW_HASH", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void updatePassword_blankCurrentPassword_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updatePassword(1L, " ", "NeuesPasswort123!", "NeuesPasswort123!")
        );

        assertEquals("Aktuelles Passwort darf nicht leer sein.", ex.getMessage());
    }

    @Test
    void updatePassword_confirmationMismatch_throwsException()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updatePassword(1L, "AktuellesPasswort1!", "NeuesPasswort123!", "Anders123!")
        );

        assertEquals("Die neuen Passwörter stimmen nicht überein.", ex.getMessage());
    }

    @Test
    void updatePassword_wrongCurrentPassword_throwsException()
    {
        User user = new User("jan", "OLD_HASH", "duck.jpg");
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(encoder.matches("falsch", "OLD_HASH")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updatePassword(1L, "falsch", "NeuesPasswort123!", "NeuesPasswort123!")
        );

        assertEquals("Das aktuelle Passwort ist falsch.", ex.getMessage());
    }

    @Test
    void updatePassword_sameAsOld_throwsException()
    {
        User user = new User("jan", "OLD_HASH", "duck.jpg");
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(encoder.matches("AktuellesPasswort1!", "OLD_HASH")).thenReturn(true);
        when(encoder.matches("AktuellesPasswort1!", "OLD_HASH")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updatePassword(
                        1L,
                        "AktuellesPasswort1!",
                        "AktuellesPasswort1!",
                        "AktuellesPasswort1!"
                )
        );

        assertEquals("Das neue Passwort muss sich vom alten unterscheiden.", ex.getMessage());
    }

    @Test
    void getUser_unknownUser_throwsException()
    {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> userService.getUser(999L)
        );

        assertEquals("Benutzer nicht gefunden", ex.getMessage());
    }

    @Test
    void deleteUser_deletesRunsAnswersProgressAndUser()
    {
        User user = new User("jan", "HASH", "duck.jpg");
        user.setUserId(1L);

        GameRun run1 = new GameRun();
        run1.setRunId(10L);

        GameRun run2 = new GameRun();
        run2.setRunId(20L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gameRunRepository.findByUser_UserId(1L)).thenReturn(List.of(run1, run2));

        userService.deleteUser(1L);

        verify(runSelectedAnswerRepository).deleteByRun_RunId(10L);
        verify(runSelectedAnswerRepository).deleteByRun_RunId(20L);
        verify(runGapAnswerRepository).deleteByRun_RunId(10L);
        verify(runGapAnswerRepository).deleteByRun_RunId(20L);
        verify(questionProgressRepository).deleteByRun_RunId(10L);
        verify(questionProgressRepository).deleteByRun_RunId(20L);
        verify(gameRunRepository).deleteAll(List.of(run1, run2));
        verify(userRepository).delete(user);
    }
}