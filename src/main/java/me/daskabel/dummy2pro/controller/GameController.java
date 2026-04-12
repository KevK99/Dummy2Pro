package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.session.QuizSessionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * Stellt Endpunkte für die Spielstände bereit.
 */
@RestController
@RequestMapping("/api/game")
public class GameController
{
    /**
     * Einfache Antwort für erfolgreiche oder erwartbare Meldungen.
     */
    public static class MessageResponse
    {
        private final String message;

        public MessageResponse(String message)
        {
            this.message = message;
        }

        public String getMessage()
        {
            return this.message;
        }
    }

    /**
     * Einheitliches Fehlerobjekt für API-Antworten.
     */
    public static class ErrorResponse
    {
        private final String error;
        private final String message;

        public ErrorResponse(String error, String message)
        {
            this.error = error;
            this.message = message;
        }

        public String getError()
        {
            return this.error;
        }

        public String getMessage()
        {
            return this.message;
        }
    }

    private final GameRunRepository gameRunRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final RunSelectedAnswerRepository runSelectedAnswerRepository;
    private final RunGapAnswerRepository runGapAnswerRepository;
    private final QuizSessionManager sessionManager;

    public GameController(
            GameRunRepository gameRunRepository,
            QuestionProgressRepository questionProgressRepository,
            RunSelectedAnswerRepository runSelectedAnswerRepository,
            RunGapAnswerRepository runGapAnswerRepository,
            QuizSessionManager sessionManager)
    {
        this.gameRunRepository = gameRunRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.runSelectedAnswerRepository = runSelectedAnswerRepository;
        this.runGapAnswerRepository = runGapAnswerRepository;
        this.sessionManager = sessionManager;
    }

    /**
     * Löscht einen Spielstand des aktuell angemeldeten Benutzers.
     *
     * Der letzte verbleibende Spielstand darf nicht gelöscht werden.
     *
     * @param runId          ID des zu löschenden Spielstands
     * @param authentication aktuelle Anmeldung
     * @return Erfolgsmeldung oder fachliche Fehlermeldung
     */
    @Transactional
    @DeleteMapping("/{runId}")
    public ResponseEntity<MessageResponse> deleteGameRun(@PathVariable Long runId, Authentication authentication)
    {
        Long userId = AuthController.extractUserId(authentication);

        GameRun run = this.gameRunRepository.findByRunIdAndUser_UserId(runId, userId)
                .orElseThrow(() -> new NoSuchElementException("Spielstand nicht gefunden"));

        long runCount = this.gameRunRepository.countByUser_UserId(userId);
        if (runCount <= 1)
        {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Der letzte Spielstand kann nicht gelöscht werden."));
        }

        // Zuerst abhängige Daten löschen, danach den eigentlichen Spielstand.
        this.runSelectedAnswerRepository.deleteByRun_RunId(runId);
        this.runGapAnswerRepository.deleteByRun_RunId(runId);
        this.questionProgressRepository.deleteByRun_RunId(runId);

        this.gameRunRepository.delete(run);
        this.gameRunRepository.flush();

        // Eine eventuell noch offene Sitzung zu diesem Spielstand ebenfalls entfernen.
        this.sessionManager.removeRunSession(runId);

        return ResponseEntity.ok(new MessageResponse("Spielstand erfolgreich gelöscht."));
    }

    /**
     * Wandelt einen nicht gefundenen Spielstand in eine 404-Antwort um.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex)
    {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }
}
