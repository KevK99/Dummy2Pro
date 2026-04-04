package me.daskabel.dummy2pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest
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

    @InjectMocks
    private UserService userService;

    @Test
    void register_ShouldSaveUser_WhenInputIsValid()
    {
        String username = "TestUser";
        String password = "SicheresPasswort1!";
        String passwordHash = "hashed-password";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(encoder.encode(password)).thenReturn(passwordHash);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(username, password);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(username, result.getUsername());
        assertEquals(passwordHash, result.getPasswordHash());
        assertEquals(username, savedUser.getUsername());
        assertEquals(passwordHash, savedUser.getPasswordHash());
    }

    @Test
    void register_ShouldThrow_WhenUsernameAlreadyExists()
    {
        String username = "SchonDa";
        String password = "SicheresPasswort1!";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(username, password)
        );

        assertEquals("Username ist bereits vergeben.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnTrue_WhenPasswordMatches()
    {
        String username = "TestUser";
        String password = "SicheresPasswort1!";
        User user = new User(username, "hash");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(encoder.matches(password, "hash")).thenReturn(true);

        boolean result = userService.login(username, password);

        assertTrue(result);
    }

    @Test
    void login_ShouldReturnFalse_WhenUserDoesNotExist()
    {
        when(userRepository.findByUsername("Unbekannt")).thenReturn(Optional.empty());

        boolean result = userService.login("Unbekannt", "SicheresPasswort1!");

        assertFalse(result);
    }

    @Test
    void authenticate_ShouldThrow_WhenPasswordDoesNotMatch()
    {
        String username = "TestUser";
        String password = "FalschesPasswort1!";
        User user = new User(username, "hash");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(encoder.matches(password, "hash")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.authenticate(username, password)
        );

        assertEquals("Benutzername oder Passwort falsch.", ex.getMessage());
    }

    @Test
    void deleteUser_ShouldDeleteRunsProgressAndUser()
    {
        Long userId = 7L;
        User user = new User("TestUser", "hash");

        GameRun run1 = new GameRun(user, LocalDateTime.now());
        run1.setRunId(11L);

        GameRun run2 = new GameRun(user, LocalDateTime.now());
        run2.setRunId(12L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(gameRunRepository.findByUser_UserId(userId)).thenReturn(List.of(run1, run2));

        userService.deleteUser(userId);

        verify(questionProgressRepository).deleteByRun_RunId(11L);
        verify(questionProgressRepository).deleteByRun_RunId(12L);
        verify(gameRunRepository).deleteAll(List.of(run1, run2));
        verify(userRepository).delete(user);
    }

    @Test
    void register_ShouldThrow_WhenPasswordIsTooShort()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("TestUser", "Kurz1!")
        );

        assertEquals("Passwort muss mindestens 13 Zeichen lang sein.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}