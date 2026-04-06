package me.daskabel.dummy2pro.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.session.QuizSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class GameControllerUnitTest
{
    @Mock
    private GameRunRepository gameRunRepository;
    @Mock
    private QuestionProgressRepository questionProgressRepository;
    @Mock
    private RunSelectedAnswerRepository runSelectedAnswerRepository;
    @Mock
    private RunGapAnswerRepository runGapAnswerRepository;
    @Mock
    private QuizSessionManager sessionManager;

    private GameController controller;

    @BeforeEach
    void setUp()
    {
        controller = new GameController(
                gameRunRepository,
                questionProgressRepository,
                runSelectedAnswerRepository,
                runGapAnswerRepository,
                sessionManager
        );
    }

    @Test
    void deleteGameRun_returnsBadRequest_whenOnlyOneRunExists()
    {
        GameRun run = run(55L, 7L);

        when(gameRunRepository.findByRunIdAndUser_UserId(55L, 7L)).thenReturn(Optional.of(run));
        when(gameRunRepository.countByUser_UserId(7L)).thenReturn(1L);

        ResponseEntity<GameController.MessageResponse> response = controller.deleteGameRun(55L, 7L);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Der letzte Spielstand kann nicht gelöscht werden.", response.getBody().getMessage());
    }

    @Test
    void deleteGameRun_deletesRunAndSession_whenMultipleRunsExist()
    {
        GameRun run = run(55L, 7L);

        when(gameRunRepository.findByRunIdAndUser_UserId(55L, 7L)).thenReturn(Optional.of(run));
        when(gameRunRepository.countByUser_UserId(7L)).thenReturn(2L);

        ResponseEntity<GameController.MessageResponse> response = controller.deleteGameRun(55L, 7L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Spielstand erfolgreich gelöscht.", response.getBody().getMessage());
        verify(gameRunRepository).delete(run);
        verify(gameRunRepository).flush();
        verify(sessionManager).removeRunSession(55L);
    }

    @Test
    void deleteGameRun_throwsWhenRunDoesNotExist()
    {
        when(gameRunRepository.findByRunIdAndUser_UserId(99L, 7L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> controller.deleteGameRun(99L, 7L));

        assertEquals("Spielstand nicht gefunden", ex.getMessage());
    }

    @Test
    void handleNotFound_wrapsErrorMessageIn404Response()
    {
        ResponseEntity<GameController.ErrorResponse> response = controller.handleNotFound(new NoSuchElementException("Nicht da"));

        assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getError());
        assertEquals("Nicht da", response.getBody().getMessage());
    }

    @Test
    void responseDtos_exposeConstructorValues()
    {
        GameController.MessageResponse messageResponse = new GameController.MessageResponse("ok");
        GameController.ErrorResponse errorResponse = new GameController.ErrorResponse("BAD_REQUEST", "kaputt");

        assertEquals("ok", messageResponse.getMessage());
        assertEquals("BAD_REQUEST", errorResponse.getError());
        assertEquals("kaputt", errorResponse.getMessage());
    }

    private GameRun run(Long runId, Long userId)
    {
        User user = new User("jan", "hash");
        user.setUserId(userId);

        GameRun run = new GameRun(user, LocalDateTime.now());
        run.setRunId(runId);
        return run;
    }
}