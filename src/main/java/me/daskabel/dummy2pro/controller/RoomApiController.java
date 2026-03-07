package me.daskabel.dummy2pro.controller;

import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.session.QuizSessionManager;
import me.daskabel.dummy2pro.session.QuizSessionManager.SessionOverviewDto;

/**
 * REST API für die Raum/Quiz-Logik — mit QuizSessionManager.
 *
 * Flow für das Frontend:
 *
 * 1. POST /api/session/start?userId=123 → Neue Session anlegen, sessionId
 * zurückbekommen
 *
 * 2. GET /api/session/{sessionId}/room/{roomId} → Raum betreten: aktuelle Frage
 * + Status laden
 *
 * 3. POST /api/session/{sessionId}/room/{roomId}/answer → Antwort schicken,
 * Ergebnis zurück
 *
 * 4. POST /api/session/{sessionId}/room/{roomId}/advance → Nächste Frage
 * anfordern (nach dem Feedback)
 *
 * 5. GET /api/session/{sessionId}/overview → Gesamtübersicht: alle 7 Räume,
 * Punkte, Medaillen
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

	private final QuizSessionManager sessionManager;

	public RoomApiController(QuizSessionManager sessionManager)
	{
		this.sessionManager = sessionManager;
	}

	/**
	 * POST /api/session/{sessionId}/room/{roomId}/advance Nächste Frage anfordern.
	 * Gibt null zurück wenn Raum abgeschlossen.
	 */
	@PostMapping("/{sessionId}/room/{roomId}/advance")
	public ResponseEntity<QuestionDto> advance(@PathVariable String sessionId,
				@PathVariable int roomId)
	{
		return ResponseEntity.ok(this.sessionManager.advance(sessionId, roomId));
	}

	/**
	 * GET /api/session/{sessionId}/overview Alle 7 Räume + Gesamtpunkte +
	 * Medaillen.
	 */
	@GetMapping("/{sessionId}/overview")
	public ResponseEntity<SessionOverviewDto> getOverview(@PathVariable String sessionId)
	{
		return ResponseEntity.ok(this.sessionManager.getOverview(sessionId));
	}

	/**
	 * GET /api/session/{sessionId}/room/{roomId} Raum betreten — liefert aktuelle
	 * Frage + Status.
	 */
	@GetMapping("/{sessionId}/room/{roomId}")
	public ResponseEntity<RoomStartDto> getRoom(@PathVariable String sessionId,
				@PathVariable int roomId)
	{
		return ResponseEntity.ok(this.sessionManager.getRoomState(sessionId, roomId));
	}

	/**
	 * GET /api/session/{sessionId}/room/{roomId}/status
	 */
	@GetMapping("/{sessionId}/room/{roomId}/status")
	public ResponseEntity<RoomStatusDto> getRoomStatus(@PathVariable String sessionId,
				@PathVariable int roomId)
	{
		return ResponseEntity.ok(this.sessionManager.getRoomStatus(sessionId, roomId));
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

	/**
	 * POST /api/session/start?userId=123 Startet eine neue Session (alle 7 Räume
	 * geshuffelt). Gibt sessionId + ersten Raum zurück.
	 */
	@PostMapping("/start")
    public ResponseEntity<SessionStartResponse> startSession(@RequestParam Long userId)
	{
		var session = this.sessionManager.createSession(userId);
		RoomStartDto firstRoom = this.sessionManager.getRoomState(session.getSessionId(), 1);

		SessionStartResponse response = new SessionStartResponse();
		response.setSessionId(session.getSessionId());
		response.setFirstRoom(firstRoom);
		return ResponseEntity.ok(response);
	}

	/**
	 * POST /api/session/{sessionId}/room/{roomId}/answer Antwort auswerten. Body: {
	 * "questionId": 1, "selectedAnswerIds": [5] } oder GAP: { "questionId": 2,
	 * "gapAnswers": [{"gapId": 1, "selectedGapOptionId": 3}] }
	 */
	@PostMapping("/{sessionId}/room/{roomId}/answer")
	public ResponseEntity<AnswerResultDto> submitAnswer(@PathVariable String sessionId,
				@PathVariable int roomId, @RequestBody AnswerRequest request)
	{
		return ResponseEntity.ok(this.sessionManager.submitAnswer(sessionId, roomId, request));
	}
}