package me.daskabel.dummy2pro.controller;

import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.session.QuizSessionManager;

@RestController
@RequestMapping("/api/game")
public class GameController
{
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

    @Transactional
    @DeleteMapping("/{runId}")
    public ResponseEntity<MessageResponse> deleteGameRun(@PathVariable Long runId, @RequestParam Long userId)
    {
        GameRun run = this.gameRunRepository.findByRunIdAndUser_UserId(runId, userId)
                .orElseThrow(() -> new NoSuchElementException("Spielstand nicht gefunden"));

        long runCount = this.gameRunRepository.countByUser_UserId(userId);
        if (runCount <= 1)
        {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Der letzte Spielstand kann nicht gelöscht werden."));
        }

        this.gameRunRepository.delete(run);
        this.gameRunRepository.flush();

        this.sessionManager.removeRunSession(runId);

        return ResponseEntity.ok(new MessageResponse("Spielstand erfolgreich gelöscht."));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex)
    {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }
}