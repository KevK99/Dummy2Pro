package me.daskabel.dummy2pro.controller;

import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;

@RestController
@RequestMapping("/api/game")
public class GameController
{
    private final GameRunRepository gameRunRepository;
    private QuestionProgressRepository questionProgressRepository;

    public GameController(GameRunRepository gameRunRepository, QuestionProgressRepository questionProgressRepository)
    {
        this.gameRunRepository = gameRunRepository;
        this.questionProgressRepository = questionProgressRepository;
    }

    @DeleteMapping("/{runId}")
    public ResponseEntity<String> deleteGameRun(@PathVariable Long runId)
    {
        GameRun run = gameRunRepository.findById(runId)
            .orElseThrow(() -> new NoSuchElementException("Spielstand nicht gefunden"));

        // Hier kannst du auch alle Fortschritte löschen, falls nötig
        questionProgressRepository.deleteByRun_RunId(runId);
        gameRunRepository.delete(run);

        return ResponseEntity.ok("Spielstand erfolgreich gelöscht.");
    }
}
