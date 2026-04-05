package me.daskabel.dummy2pro.service;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceSaveCurrentGameProgressUnitTest
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
    void saveCurrentGameProgress_readsLatestRunAndItsProgress()
    {
        GameRun run = new GameRun();
        run.setRunId(42L);

        when(gameRunRepository.findTopByUser_UserIdOrderByStartedAtDesc(1L)).thenReturn(Optional.of(run));
        when(questionProgressRepository.findByRun_RunId(42L)).thenReturn(List.of());

        userService.saveCurrentGameProgress(1L);

        verify(gameRunRepository).findTopByUser_UserIdOrderByStartedAtDesc(1L);
        verify(questionProgressRepository).findByRun_RunId(42L);
    }

    @Test
    void saveCurrentGameProgress_withoutActiveRun_throwsException()
    {
        when(gameRunRepository.findTopByUser_UserIdOrderByStartedAtDesc(999L)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> userService.saveCurrentGameProgress(999L)
        );
    }
}