package me.daskabel.dummy2pro.controller;

import jakarta.servlet.http.HttpSession;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.dto.RunReviewDto;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import me.daskabel.dummy2pro.session.QuizSession;
import me.daskabel.dummy2pro.session.QuizSessionManager;
import me.daskabel.dummy2pro.session.QuizSessionManager.SessionOverviewDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stellt die REST-Endpunkte für Spielstände, Sitzungen und Räume bereit.
 *
 * Der Controller verbindet das Frontend mit dem QuizSessionManager und den
 * benötigten Repositories. Er kümmert sich vor allem um Laden, Starten,
 * Betreten und Auswerten von Räumen sowie um spielstandsbezogene Aktionen.
 */
@RestController
@RequestMapping("/api/session")
public class RoomApiController
{
    /**
     * Erlaubte Zeichen für einen frei gesetzten Spielstandnamen.
     */
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N} _.-]{1,40}$");

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

    /**
     * Daten eines Spielstands für die Listenansicht im Frontend.
     */
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

    /**
     * Anfragedaten zum Umbenennen eines Spielstands.
     */
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

    /**
     * Antwort beim Start einer neuen Sitzung.
     */
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

    /**
     * Raumdaten für die Übersicht im Dashboard.
     */
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

    /**
     * Gesamtdaten für das Dashboard.
     */
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
     * Fordert nach einer beantworteten Frage die nächste Frage des Raums an.
     *
     * Ist der Raum abgeschlossen, liefert der Manager null zurück.
     */
    @PostMapping("/{sessionId}/room/{roomId}/advance")
    public ResponseEntity<QuestionDto> advance(
            Authentication authentication,
            @PathVariable String sessionId,
            @PathVariable int roomId)
    {
        assertSessionOwner(sessionId, requireCurrentUserId(authentication));
        return ResponseEntity.ok(this.sessionManager.advance(sessionId, roomId));
    }

    /**
     * Legt einen neuen Spielstand samt neuer Sitzung für den aktuellen Benutzer an.
     */
    @PostMapping("/new")
    public Map<String, Object> createNewRun(Authentication authentication)
    {
        Long userId = requireCurrentUserId(authentication);
        QuizSession session = this.sessionManager.createNewRunSession(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("runId", session.getRunId());
        response.put("activeRoomId", session.getActiveRoomId());
        response.put("overview", this.sessionManager.getOverview(session.getSessionId()));

        return response;
    }

    /**
     * Liefert die Gesamtübersicht einer Sitzung über alle Räume.
     */
    @GetMapping("/{sessionId}/overview")
    public ResponseEntity<SessionOverviewDto> getOverview(Authentication authentication, @PathVariable String sessionId)
    {
        assertSessionOwner(sessionId, requireCurrentUserId(authentication));
        return ResponseEntity.ok(this.sessionManager.getOverview(sessionId));
    }

    /**
     * Liefert die Review-Daten eines kompletten Spielstands.
     */
    @GetMapping("/{sessionId}/review")
    public ResponseEntity<RunReviewDto> getRunReview(Authentication authentication, @PathVariable String sessionId)
    {
        assertSessionOwner(sessionId, requireCurrentUserId(authentication));
        return ResponseEntity.ok(this.sessionManager.getRunReview(sessionId));
    }

    /**
     * Lädt den aktuellen Zustand eines Raums, einschließlich aktueller Frage.
     */
    @GetMapping("/{sessionId}/room/{roomId}")
    public ResponseEntity<RoomStartDto> getRoom(
            Authentication authentication,
            @PathVariable String sessionId,
            @PathVariable int roomId)
    {
        assertSessionOwner(sessionId, requireCurrentUserId(authentication));
        return ResponseEntity.ok(this.sessionManager.getRoomState(sessionId, roomId));
    }

    /**
     * Liefert nur den Status eines Raums ohne neue Frage.
     */
    @GetMapping("/{sessionId}/room/{roomId}/status")
    public ResponseEntity<RoomStatusDto> getRoomStatus(
            Authentication authentication,
            @PathVariable String sessionId,
            @PathVariable int roomId)
    {
        assertSessionOwner(sessionId, requireCurrentUserId(authentication));
        RoomStatusDto status = this.sessionManager.getRoomStatus(sessionId, roomId);
        return ResponseEntity.ok(status);
    }

    /**
     * Liefert alle Spielstände des aktuell angemeldeten Benutzers.
     */
    @GetMapping("/runs")
    public ResponseEntity<List<RunListEntryResponse>> getRunsForUser(Authentication authentication)
    {
        Long userId = requireCurrentUserId(authentication);
        List<GameRun> runs = this.gameRunRepository.findByUser_UserIdOrderByFinishedAtAscStartedAtDesc(userId);

        List<RunListEntryResponse> response = runs.stream()
                .map(this::toRunListEntryResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Ändert den Anzeigenamen eines Spielstands.
     */
    @PutMapping("/{runId}/name")
    public ResponseEntity<RunListEntryResponse> renameRun(
            Authentication authentication,
            @PathVariable Long runId,
            @RequestBody RenameRunRequest request)
    {
        Long userId = requireCurrentUserId(authentication);

        GameRun run = this.gameRunRepository.findByRunIdAndUser_UserId(runId, userId)
                .orElseThrow(() -> new NoSuchElementException("Spielstand nicht gefunden"));

        run.setDisplayName(normalizeDisplayName(request != null ? request.getDisplayName() : null));

        GameRun savedRun = this.gameRunRepository.save(run);
        return ResponseEntity.ok(toRunListEntryResponse(savedRun));
    }

    /**
     * Lädt einen vorhandenen Spielstand in eine aktive Sitzung.
     */
    @PostMapping("/load")
    public Map<String, Object> loadRun(Authentication authentication, @RequestParam Long runId)
    {
        Long userId = requireCurrentUserId(authentication);

        this.gameRunRepository.findByRunIdAndUser_UserId(runId, userId)
                .orElseThrow(() -> new NoSuchElementException("Spielstand nicht gefunden."));

        QuizSession session = this.sessionManager.loadSessionForRun(runId);

        if (!Objects.equals(session.getUserId(), userId))
        {
            throw new AccessDeniedException("Zugriff auf fremde Session verweigert.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("runId", session.getRunId());
        response.put("activeRoomId", session.getActiveRoomId());
        response.put("overview", this.sessionManager.getOverview(session.getSessionId()));

        return response;
    }

    /**
     * Startet ausdrücklich einen neuen Spielstand.
     *
     * Bestehende Spielstände werden nicht geladen, sondern es wird immer eine
     * neue Sitzung erzeugt.
     */
    @PostMapping("/start")
    public ResponseEntity<SessionStartResponse> startSession(Authentication authentication)
    {
        Long userId = requireCurrentUserId(authentication);
        QuizSession session = this.sessionManager.createNewRunSession(userId);

        SessionStartResponse response = new SessionStartResponse();
        response.setSessionId(session.getSessionId());
        response.setFirstRoom(this.sessionManager.getRoomState(session.getSessionId(), session.getActiveRoomId()));

        return ResponseEntity.ok(response);
    }

    /**
     * Wertet eine Antwort für einen Raum aus.
     */
    @PostMapping("/{sessionId}/room/{roomId}/answer")
    public ResponseEntity<AnswerResultDto> submitAnswer(
            Authentication authentication,
            @PathVariable String sessionId,
            @PathVariable int roomId,
            @RequestBody AnswerRequest request)
    {
        assertSessionOwner(sessionId, requireCurrentUserId(authentication));
        return ResponseEntity.ok(this.sessionManager.submitAnswer(sessionId, roomId, request));
    }

    /**
     * Bereitet einen Raum vor, bevor er betreten oder fortgesetzt wird.
     */
    @PostMapping("/{sessionId}/room/{roomId}/prepare")
    public ResponseEntity<RoomStatusDto> prepareRoom(
            Authentication authentication,
            @PathVariable String sessionId,
            @PathVariable int roomId)
    {
        assertSessionOwner(sessionId, requireCurrentUserId(authentication));
        return ResponseEntity.ok(this.sessionManager.prepareRoom(sessionId, roomId));
    }

    /**
     * Liest die Benutzer-ID des aktuell angemeldeten Benutzers aus.
     */
    private Long requireCurrentUserId(Authentication authentication)
    {
        return AuthController.extractUserId(authentication);
    }

    /**
     * Prüft, ob die Sitzung dem aktuell angemeldeten Benutzer gehört.
     */
    private void assertSessionOwner(String sessionId, Long currentUserId)
    {
        QuizSession session = this.sessionManager.getSession(sessionId);
        if (!Objects.equals(session.getUserId(), currentUserId))
        {
            throw new AccessDeniedException("Zugriff auf fremde Session verweigert.");
        }
    }

    /**
     * Bereinigt und prüft den Anzeigenamen eines Spielstands.
     *
     * Leere Eingaben werden als null behandelt, damit ein Name auch wieder
     * entfernt werden kann.
     */
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

        if (trimmed.length() > 40)
        {
            throw new IllegalArgumentException("Der Spielstandname darf maximal 40 Zeichen lang sein.");
        }

        if (!DISPLAY_NAME_PATTERN.matcher(trimmed).matches())
        {
            throw new IllegalArgumentException("Der Spielstandname enthält ungültige Zeichen.");
        }

        return trimmed;
    }

    /**
     * Wandelt ein GameRun-Objekt in das Antwortformat für das Frontend um.
     */
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

    /**
     * Wandelt ungültige Eingaben in eine 400-Antwort um.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex)
    {
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    /**
     * Wandelt nicht gefundene Daten in eine 404-Antwort um.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex)
    {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    /**
     * Wandelt interne Zustandsfehler in eine 500-Antwort um.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleServerError(IllegalStateException ex)
    {
        return ResponseEntity.status(500).body(new ErrorResponse("SERVER_ERROR", ex.getMessage()));
    }

    /**
     * Wandelt unzulässige Zugriffe in eine 403-Antwort um.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex)
    {
        return ResponseEntity.status(403).body(new ErrorResponse("FORBIDDEN", ex.getMessage()));
    }
}
