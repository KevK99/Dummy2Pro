package me.daskabel.dummy2pro.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import me.daskabel.dummy2pro.session.QuizSession;
import me.daskabel.dummy2pro.session.QuizSessionManager;
import me.daskabel.dummy2pro.session.QuizSessionManager.SessionOverviewDto;

/**
 * REST API für die Raum/Quiz-Logik — mit QuizSessionManager.
 *
 * Aktueller Flow für das Frontend:
 *
 * 1. GET /api/session/runs?userId=123 → vorhandene Spielstände des Users laden
 *
 * 2. POST /api/session/load?userId=123&runId=7 → vorhandenen Spielstand laden
 *
 * 3. POST /api/session/new?userId=123 → neuen Spielstand anlegen
 *
 * 4. GET /api/session/{sessionId}/room/{roomId} → Raum betreten: aktuelle Frage + Status laden
 *
 * 5. POST /api/session/{sessionId}/room/{roomId}/answer → Antwort schicken, Ergebnis zurück
 *
 * 6. POST /api/session/{sessionId}/room/{roomId}/advance → nächste Frage anfordern
 *
 * 7. GET /api/session/{sessionId}/overview → Gesamtübersicht: alle Räume, Punkte, Medaillen
 *
 * ALT: POST /api/session/start?userId=123 → startet ausdrücklich einen neuen Spielstand
 */

@RestController
@RequestMapping("/api/session")
public class RoomApiController
{

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

    public static class RunListEntryResponse
    {
        private Long runId;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private boolean finished;
        private String displayName;

        public LocalDateTime getFinishedAt()
        {
            return this.finishedAt;
        }

        public Long getRunId()
        {
            return this.runId;
        }

        public LocalDateTime getStartedAt()
        {
            return this.startedAt;
        }

        public boolean isFinished()
        {
            return this.finished;
        }

        public String getDisplayName()
        {
            return this.displayName;
        }

        public void setFinished(boolean finished)
        {
            this.finished = finished;
        }

        public void setFinishedAt(LocalDateTime finishedAt)
        {
            this.finishedAt = finishedAt;
        }

        public void setRunId(Long runId)
        {
            this.runId = runId;
        }

        public void setStartedAt(LocalDateTime startedAt)
        {
            this.startedAt = startedAt;
        }

        public void setDisplayName(String displayName)
        {
            this.displayName = displayName;
        }
    }

    public static class RenameRunRequest
    {
        private String displayName;

        public String getDisplayName()
        {
            return this.displayName;
        }

        public void setDisplayName(String displayName)
        {
            this.displayName = displayName;
        }
    }

    public static class SessionStartResponse
    {
        private String sessionId;
        private RoomStartDto firstRoom;

        public RoomStartDto getFirstRoom()
        {
            return this.firstRoom;
        }

        public String getSessionId()
        {
            return this.sessionId;
        }

        public void setFirstRoom(RoomStartDto v)
        {
            this.firstRoom = v;
        }

        public void setSessionId(String v)
        {
            this.sessionId = v;
        }
    }

    public static class DashboardRoomResponse
    {
        private int roomId;
        private String themeName;
        private int totalQuestions;
        private int answeredQuestions;
        private int correctAnswers;
        private int completionPercent;

        public int getAnsweredQuestions()
        {
            return this.answeredQuestions;
        }

        public int getCompletionPercent()
        {
            return this.completionPercent;
        }

        public int getCorrectAnswers()
        {
            return this.correctAnswers;
        }

        public int getRoomId()
        {
            return this.roomId;
        }

        public String getThemeName()
        {
            return this.themeName;
        }

        public int getTotalQuestions()
        {
            return this.totalQuestions;
        }

        public void setAnsweredQuestions(int answeredQuestions)
        {
            this.answeredQuestions = answeredQuestions;
        }

        public void setCompletionPercent(int completionPercent)
        {
            this.completionPercent = completionPercent;
        }

        public void setCorrectAnswers(int correctAnswers)
        {
            this.correctAnswers = correctAnswers;
        }

        public void setRoomId(int roomId)
        {
            this.roomId = roomId;
        }

        public void setThemeName(String themeName)
        {
            this.themeName = themeName;
        }

        public void setTotalQuestions(int totalQuestions)
        {
            this.totalQuestions = totalQuestions;
        }
    }

    public static class DashboardOverviewResponse
    {
        private Long runId;
        private String username;
        private List<DashboardRoomResponse> rooms;

        public List<DashboardRoomResponse> getRooms()
        {
            return this.rooms;
        }

        public Long getRunId()
        {
            return this.runId;
        }

        public String getUsername()
        {
            return this.username;
        }

        public void setRooms(List<DashboardRoomResponse> rooms)
        {
            this.rooms = rooms;
        }

        public void setRunId(Long runId)
        {
            this.runId = runId;
        }

        public void setUsername(String username)
        {
            this.username = username;
        }
    }

    private static final int QUESTIONS_PER_ROOM = 40;

    private final QuizSessionManager sessionManager;

    private final GameRunRepository gameRunRepository;
    private final ThemeRepository themeRepository;
    private final QuestionRepository questionRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final UserRepository userRepository;

    public RoomApiController(
            QuizSessionManager sessionManager,
            GameRunRepository gameRunRepository,
            ThemeRepository themeRepository,
            QuestionRepository questionRepository,
            QuestionProgressRepository questionProgressRepository,
            UserRepository userRepository)
    {
        this.sessionManager = sessionManager;
        this.gameRunRepository = gameRunRepository;
        this.themeRepository = themeRepository;
        this.questionRepository = questionRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/session/{sessionId}/room/{roomId}/advance Nächste Frage anfordern. Gibt null zurück wenn Raum
     * abgeschlossen.
     */
    @PostMapping("/{sessionId}/room/{roomId}/advance")
    public ResponseEntity<QuestionDto> advance(@PathVariable String sessionId, @PathVariable int roomId)
    {
        return ResponseEntity.ok(this.sessionManager.advance(sessionId, roomId));
    }

    @PostMapping("/new")
    public Map<String, Object> createNewRun(@RequestParam Long userId)
    {
        QuizSession session = this.sessionManager.createNewRunSession(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("runId", session.getRunId());
        response.put("activeRoomId", session.getActiveRoomId());
        response.put("overview", this.sessionManager.getOverview(session.getSessionId()));

        return response;
    }

    /**
     * GET /api/session/{sessionId}/overview Alle 7 Räume + Gesamtpunkte + Medaillen.
     */
    @GetMapping("/{sessionId}/overview")
    public ResponseEntity<SessionOverviewDto> getOverview(@PathVariable String sessionId)
    {
        return ResponseEntity.ok(this.sessionManager.getOverview(sessionId));
    }

    /**
     * GET /api/session/{sessionId}/room/{roomId} Raum betreten — liefert aktuelle Frage + Status.
     */
    @GetMapping("/{sessionId}/room/{roomId}")
    public ResponseEntity<RoomStartDto> getRoom(@PathVariable String sessionId, @PathVariable int roomId)
    {
        return ResponseEntity.ok(this.sessionManager.getRoomState(sessionId, roomId));
    }

    /**
     * GET /api/session/{sessionId}/room/{roomId}/status
     */
    @GetMapping("/{sessionId}/room/{roomId}/status")
    public ResponseEntity<RoomStatusDto> getRoomStatus(@PathVariable String sessionId, @PathVariable int roomId)
    {
        RoomStatusDto status = this.sessionManager.getRoomStatus(sessionId, roomId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/runs")
    public ResponseEntity<List<RunListEntryResponse>> getRunsForUser(@RequestParam Long userId)
    {
        List<GameRun> runs = this.gameRunRepository.findByUser_UserIdOrderByFinishedAtAscStartedAtDesc(userId);

        List<RunListEntryResponse> response = runs.stream().map(run -> {
            RunListEntryResponse dto = new RunListEntryResponse();
            dto.setRunId(run.getRunId());
            dto.setStartedAt(run.getStartedAt());
            dto.setFinishedAt(run.getFinishedAt());
            dto.setFinished(run.getFinishedAt() != null);
            dto.setDisplayName(run.getDisplayName());
            return dto;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{runId}/name")
    public ResponseEntity<RunListEntryResponse> renameRun(
            @PathVariable Long runId,
            @RequestParam Long userId,
            @RequestBody RenameRunRequest request)
    {
        GameRun run = this.gameRunRepository.findByRunIdAndUser_UserId(runId, userId)
                .orElseThrow(() -> new NoSuchElementException("Spielstand nicht gefunden"));

        run.setDisplayName(normalizeDisplayName(request != null ? request.getDisplayName() : null));

        GameRun savedRun = this.gameRunRepository.save(run);
        return ResponseEntity.ok(toRunListEntryResponse(savedRun));
    }

    // Fehlerbehandlung
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex)
    {
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex)
    {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleServerError(IllegalStateException ex)
    {
        return ResponseEntity.status(500).body(new ErrorResponse("SERVER_ERROR", ex.getMessage()));
    }

    @PostMapping("/load")
    public Map<String, Object> loadRun(@RequestParam Long userId, @RequestParam Long runId)
    {
        this.gameRunRepository.findByRunIdAndUser_UserId(runId, userId)
            .orElseThrow(
                () -> new NoSuchElementException(
                    "Run " + runId + " gehört nicht zu User " + userId + " oder existiert nicht."));

        QuizSession session = this.sessionManager.loadSessionForRun(runId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("runId", session.getRunId());
        response.put("activeRoomId", session.getActiveRoomId());
        response.put("overview", this.sessionManager.getOverview(session.getSessionId()));

        return response;
    }

    /**
     * ALT-Endpoint: POST /api/session/start?userId=123
     *
     * Bedeutet ab jetzt ausdrücklich: neuer Spielstand + neue Session.
     *
     * Für bestehende Spielstände muss /api/session/load verwendet werden.
     */

    @PostMapping("/start")
    public ResponseEntity<SessionStartResponse> startSession(@RequestParam Long userId)
    {
        QuizSession session = this.sessionManager.createNewRunSession(userId);

        SessionStartResponse response = new SessionStartResponse();
        response.setSessionId(session.getSessionId());
        response.setFirstRoom(this.sessionManager.getRoomState(session.getSessionId(), session.getActiveRoomId()));

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/session/{sessionId}/room/{roomId}/answer Antwort auswerten. Body: { "questionId": 1,
     * "selectedAnswerIds": [5] } oder GAP: { "questionId": 2, "gapAnswers": [{"gapId": 1, "selectedGapOptionId": 3}] }
     */
    @PostMapping("/{sessionId}/room/{roomId}/answer")
    public ResponseEntity<AnswerResultDto> submitAnswer(@PathVariable String sessionId, @PathVariable int roomId,
        @RequestBody AnswerRequest request)
    {
        return ResponseEntity.ok(this.sessionManager.submitAnswer(sessionId, roomId, request));
    }

    @PostMapping("/{sessionId}/room/{roomId}/prepare")
    public ResponseEntity<RoomStatusDto> prepareRoom(@PathVariable String sessionId, @PathVariable int roomId)
    {
        return ResponseEntity.ok(this.sessionManager.prepareRoom(sessionId, roomId));
    }

    private String normalizeDisplayName(String displayName)
    {
        if (displayName == null)
        {
            return null;
        }

        String trimmed = displayName.trim();

        if (trimmed.isEmpty())
        {
            return null;
        }

        if (trimmed.length() > 100)
        {
            throw new IllegalArgumentException("Der Spielstandname darf maximal 100 Zeichen lang sein.");
        }

        return trimmed;
    }

    private RunListEntryResponse toRunListEntryResponse(GameRun run)
    {
        RunListEntryResponse dto = new RunListEntryResponse();
        dto.setRunId(run.getRunId());
        dto.setStartedAt(run.getStartedAt());
        dto.setFinishedAt(run.getFinishedAt());
        dto.setFinished(run.getFinishedAt() != null);
        dto.setDisplayName(run.getDisplayName());
        return dto;
    }

}
