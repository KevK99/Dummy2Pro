package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.PracticeRoomDto;
import me.daskabel.dummy2pro.service.AbbreviationPracticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stellt die Endpunkte für den wiederholbaren Abkürzungsraum bereit.
 *
 * Der Raum 17 arbeitet unabhängig vom Rest und dient
 * nur zum Üben von Abkürzungen.
 */
@RestController
@RequestMapping("/api/practice/abbreviations")
public class AbbreviationPracticeController
{
    private final AbbreviationPracticeService abbreviationPracticeService;

    public AbbreviationPracticeController(AbbreviationPracticeService abbreviationPracticeService)
    {
        this.abbreviationPracticeService = abbreviationPracticeService;
    }

    /**
     * Lädt den Übungsraum mit allen dafür benötigten Daten.
     *
     * @return Daten des Abkürzungsraums
     */
    @GetMapping
    public ResponseEntity<PracticeRoomDto> loadRoom()
    {
        return ResponseEntity.ok(this.abbreviationPracticeService.loadPracticeRoom());
    }

    /**
     * Prüft eine gegebene Antwort und liefert das Ergebnis zurück.
     *
     * @param request Antwortdaten aus dem Frontend
     * @return Auswertung der Antwort
     */
    @PostMapping("/answer")
    public ResponseEntity<AnswerResultDto> evaluateAnswer(@RequestBody AnswerRequest request)
    {
        return ResponseEntity.ok(this.abbreviationPracticeService.evaluateAnswer(request));
    }
}