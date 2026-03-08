package me.daskabel.dummy2pro.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import me.daskabel.dummy2pro.model.*;
import me.daskabel.dummy2pro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapResultEntry;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.session.QuizSession.RoomSession;

/**
 * Verwaltet alle aktiven QuizSessions im Arbeitsspeicher.
 *
 * Verantwortlichkeiten:
 * - Sessions anlegen (über QuizSessionGenerator) und speichern
 * - Aktuelle Frage liefern
 * - Antworten auswerten (MC / TF / GAP) und
 *  Ergebnis in die Session schreiben
 * - Fortschritt in die DB persistieren
 * - Session-Status (Raum-Übersicht, Gesamt-Punkte) liefern
 * - Abgelaufene Sessions aufräumen (einfaches TTL-Konzept)
 *
 * Speicherstruktur: sessionId (String/UUID) → QuizSession
 *
 * Für eingeloggte User wird zusätzlich eine Mapping-Tabelle geführt: userId →
 * sessionId Damit kann ein User seine laufende Session wiederfinden ohne die
 * sessionId manuell im Frontend speichern zu müssen.
 *
 * Thread-Safety: ConcurrentHashMap — reicht für den Schulprojekt-Kontext.
 */
@Service
public class QuizSessionManager
{

	/**
	 * Übersicht über die gesamte Session (alle 7 Räume). Wird für Dashboard und
	 * Finish-Screen genutzt.
	 */
	public static class SessionOverviewDto
	{
		private String sessionId;
		private int totalEarnedPoints;
		private int totalMaxPoints;
		private int totalCorrect;
		private int totalWrong;
		private boolean fullyCompleted;
		private List<RoomStatusDto> rooms;

		public List<RoomStatusDto> getRooms()
		{
			return this.rooms;
		}

		public String getSessionId()
		{
			return this.sessionId;
		}

		public int getTotalCorrect()
		{
			return this.totalCorrect;
		}

		public int getTotalEarnedPoints()
		{
			return this.totalEarnedPoints;
		}

		public int getTotalMaxPoints()
		{
			return this.totalMaxPoints;
		}

		public int getTotalWrong()
		{
			return this.totalWrong;
		}

		public boolean isFullyCompleted()
		{
			return this.fullyCompleted;
		}

		public void setFullyCompleted(boolean v)
		{
			this.fullyCompleted = v;
		}

		public void setRooms(List<RoomStatusDto> v)
		{
			this.rooms = v;
		}

		public void setSessionId(String v)
		{
			this.sessionId = v;
		}

		public void setTotalCorrect(int v)
		{
			this.totalCorrect = v;
		}

		public void setTotalEarnedPoints(int v)
		{
			this.totalEarnedPoints = v;
		}

		public void setTotalMaxPoints(int v)
		{
			this.totalMaxPoints = v;
		}

		public void setTotalWrong(int v)
		{
			this.totalWrong = v;
		}
	}

	// Sessions ohne Aktivität länger als diese Zeit werden beim nächsten
	// Cleanup entfernt (hier: 2 Stunden)
	private static final int SESSION_TTL_HOURS = 2;

	private static RoomStatusDto buildRoomStatus(RoomSession room)
	{
		RoomStatusDto s = new RoomStatusDto();
		s.setRoomId(room.getRoomId());
		s.setThemeName(room.getThemeName());
		s.setTotalQuestions(room.getTotalQuestions());
		s.setAnsweredQuestions(room.getAnsweredCount());
		s.setCorrectAnswers(room.getCorrectCount());
		s.setWrongAnswers(room.getWrongCount());
		s.setOpenQuestions(room.getTotalQuestions() - room.getAnsweredCount());
		s.setTotalPoints(room.getMaxPoints());
		s.setEarnedPoints(room.getEarnedPoints());
		s.setCompletionPercent(room.getCompletionPercent());
		s.setMedal(room.getMedal());
		return s;
	}

	private static AnswerResultDto evaluateGap(Question question, AnswerRequest request)
	{
		Map<Long, GapOption> correctByGapId = new HashMap<>();
		if (question.getGapFields() != null)
		{
			for (GapField gf : question.getGapFields())
			{
				if (gf.getGapOptions() != null)
				{
					gf.getGapOptions().stream()
                            .filter(GapOption::getIsCorrect)
                            .findFirst()
                            .ifPresent(o -> correctByGapId.put(gf.getGapId(), o));
				}
			}
		}

		Map<Long, Long> selectedByGapId = new HashMap<>();
		if (request.getGapAnswers() != null)
		{
			for (var entry : request.getGapAnswers())
			{
				selectedByGapId.put(entry.getGapId(), entry.getSelectedGapOptionId());
			}
		}

		List<GapResultEntry> gapResults = new ArrayList<>();
		boolean allCorrect = true;

		for (Map.Entry<Long, GapOption> entry : correctByGapId.entrySet())
		{
			Long gapId = entry.getKey();
			GapOption correctOption = entry.getValue();
			Long selected = selectedByGapId.get(gapId);
			boolean gapCorrect = correctOption.getGapOptionId().equals(selected);
			if (!gapCorrect)
				allCorrect = false;

			GapResultEntry gre = new GapResultEntry();
			gre.setGapId(gapId);
			gre.setCorrect(gapCorrect);
			gre.setCorrectGapOptionId(correctOption.getGapOptionId());
			gre.setCorrectOptionText(correctOption.getOptionText());
			gapResults.add(gre);
		}

		AnswerResultDto result = new AnswerResultDto();
		result.setCorrect(allCorrect);
		result.setGapResults(gapResults);
		result.setPointsEarned(
					allCorrect ? (question.getPoints()) : 0);
		return result;
	}

	private static AnswerResultDto evaluateMcTf(Question question, AnswerRequest request)
	{
		Set<Long> correctIds = question.getAnswerOptions().stream()
                    .filter(AnswerOption::getIsCorrect)
                    .map(AnswerOption::getAnswerId)
					.collect(Collectors.toSet());

		Set<Long> selectedIds = new HashSet<>(
					request.getSelectedAnswerIds() != null ? request.getSelectedAnswerIds()
								: Collections.emptyList());

		boolean correct = correctIds.equals(selectedIds);

		AnswerResultDto result = new AnswerResultDto();
		result.setCorrect(correct);
		result.setCorrectAnswerIds(new ArrayList<>(correctIds));
		result.setPointsEarned(
					correct ? (question.getPoints()) : 0);
		return result;
	}

	// Sessions: sessionId → QuizSession
	private final Map<String, QuizSession> sessions = new ConcurrentHashMap<>();
	// User → aktive sessionId (für eingeloggte User)
	private final Map<Long, String> userSessionMap = new ConcurrentHashMap<>();

	private final QuizSessionGenerator generator;

	// ================================================================
	// Session anlegen / finden
	// ================================================================

	private final QuestionRepository questionRepo;
    private final GameRunRepository gameRunRepo;
    private final QuestionProgressRepository questionProgressRepo;
    private final RunSelectedAnswerRepository runSelectedAnswerRepo;
    private final RunGapAnswerRepository runGapAnswerRepo;
    private final UserRepository userRepo;

	// ================================================================
	// Navigation
	// ================================================================

	public QuizSessionManager(QuizSessionGenerator generator,
                              QuestionRepository questionRepo,
                              GameRunRepository gameRunRepo,
                              QuestionProgressRepository questionProgressRepo,
                              RunSelectedAnswerRepository runSelectedAnswerRepo,
                              RunGapAnswerRepository runGapAnswerRepo,
                              UserRepository userRepo)
	{
		this.generator = generator;
		this.questionRepo = questionRepo;
        this.gameRunRepo = gameRunRepo;
        this.questionProgressRepo = questionProgressRepo;
        this.runSelectedAnswerRepo = runSelectedAnswerRepo;
        this.runGapAnswerRepo = runGapAnswerRepo;
        this.userRepo = userRepo;
	}

	/**
	 * Rückt im aktiven Raum zur nächsten Frage vor. Gibt null zurück wenn der Raum
	 * abgeschlossen ist.
	 */
	public QuestionDto advance(String sessionId, int roomId)
	{
		QuizSession session = getSession(sessionId);
		RoomSession room = session.getRooms().get(roomId);

		if (room == null || room.isCompleted())
		{
			return null;
		}

		boolean hasNext = room.advance();
		return hasNext ? room.currentQuestion() : null;
	}

	// ================================================================
	// Antwort auswerten
	// ================================================================

	/**
	 * Entfernt Sessions die länger als SESSION_TTL_HOURS inaktiv waren. Kann
	 * per @Scheduled aufgerufen werden oder manuell.
	 *
	 * Beispiel für automatischen Aufruf (in einer Config-Klasse):
	 *
	 * @Scheduled(fixedDelay = 3600000) // jede Stunde public void cleanup() {
	 *                       sessionManager.cleanupExpiredSessions(); }
	 */
	public int cleanupExpiredSessions()
	{
		LocalDateTime cutoff = LocalDateTime.now().minusHours(SESSION_TTL_HOURS);
		int removed = 0;

		Iterator<Map.Entry<String, QuizSession>> it = this.sessions.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, QuizSession> entry = it.next();
			if (entry.getValue().getLastActivityAt().isBefore(cutoff))
			{
				Long userId = entry.getValue().getUserId();
				if (userId != null)
					this.userSessionMap.remove(userId);
				it.remove();
				removed++;
			}
		}
		return removed;
	}

	/**
	 * Startet eine neue QuizSession (alle 7 Räume, Fragen geshuffelt).
	 *
	 * Wenn der User bereits eine aktive Session hat, wird diese überschrieben (=
	 * neuer Spielstart).
	 *
	 * @param userId
	 * @return Die neue Session
	 */
    @Transactional
	public QuizSession createSession(Long userId)
	{
		if (userId == null)
		{
            throw new IllegalArgumentException("userId darf nicht null sein.");
		}

        if (this.userSessionMap.containsKey(userId))
        {
            String oldSessionId = this.userSessionMap.get(userId);
            if (oldSessionId != null) {
                this.sessions.remove(oldSessionId);
            }
        }

        GameRun run = new GameRun();
        run.setUser(this.userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User " + userId + " nicht gefunden.")));
        run.setStartedAt(LocalDateTime.now());
        run.setFinishedAt(null);
        run = this.gameRunRepo.save(run);

		QuizSession session = this.generator.generate(userId, run.getRunId());
        persistInitialQuestionProgress(run, session);
		this.sessions.put(session.getSessionId(), session);
        this.userSessionMap.put(userId, session.getSessionId());

		return session;
	}

	// ================================================================
	// Status-Abfragen
	// ================================================================

	/** Wie viele Sessions sind gerade aktiv? (für Monitoring / Debugging) */
	public int getActiveSessionCount()
	{
		return this.sessions.size();
	}

	/**
	 * Gibt die aktive Session eines eingeloggten Users zurück. Erstellt eine neue
	 * Session wenn noch keine vorhanden.
	 */
	public QuizSession getOrCreateSessionForUser(Long userId)
	{
		String sessionId = this.userSessionMap.get(userId);
		if (sessionId != null && this.sessions.containsKey(sessionId))
		{
			return this.sessions.get(sessionId);
		}
		return createSession(userId);
	}

	// ================================================================
	// Session-Cleanup (abgelaufene Sessions entfernen)
	// ================================================================

	/**
	 * Übersicht über alle 7 Räume + Gesamtpunkte. Wird für Dashboard und
	 * Finish-Screen genutzt.
	 */
	public SessionOverviewDto getOverview(String sessionId)
	{
		QuizSession session = getSession(sessionId);

		List<RoomStatusDto> roomStatuses = new ArrayList<>();
		for (RoomSession room : session.getRooms().values())
		{
				roomStatuses.add(buildRoomStatus(room));
		}

		SessionOverviewDto overview = new SessionOverviewDto();
		overview.setSessionId(sessionId);
		overview.setTotalEarnedPoints(session.getTotalEarnedPoints());
		overview.setTotalMaxPoints(session.getTotalMaxPoints());
		overview.setTotalCorrect(session.getTotalCorrect());
		overview.setTotalWrong(session.getTotalWrong());
		overview.setFullyCompleted(session.isFullyCompleted());
		overview.setRooms(roomStatuses);
		return overview;
	}

	/**
	 * Liefert den aktuellen Stand eines Raums (Status + aktuelle Frage).
	 */
	public RoomStartDto getRoomState(String sessionId, int roomId)
	{
		QuizSession session = getSession(sessionId);
		RoomSession room = session.getRooms().get(roomId);

		if (room == null)
		{
			throw new NoSuchElementException("Raum " + roomId + " in Session nicht gefunden.");
		}

		RoomStartDto dto = new RoomStartDto();
		dto.setStatus(buildRoomStatus(room));
		dto.setFirstQuestion(room.currentQuestion());
		dto.setQuestionSequence(room.getQuestionSequence());
		return dto;
	}

	// ================================================================
	// Private Auswertungs-Methoden
	// ================================================================

	/** Status eines einzelnen Raums. */
	public RoomStatusDto getRoomStatus(String sessionId, int roomId)
	{
		QuizSession session = getSession(sessionId);
		RoomSession room = session.getRooms().get(roomId);
		if (room == null)
			throw new NoSuchElementException("Raum " + roomId + " nicht gefunden.");
		return buildRoomStatus(room);
	}

	/**
	 * Gibt eine bestehende Session per ID zurück. Wirft Exception wenn nicht
	 * gefunden oder abgelaufen.
	 */
	public QuizSession getSession(String sessionId)
	{
		QuizSession session = this.sessions.get(sessionId);
		if (session == null)
		{
			throw new NoSuchElementException("Session '" + sessionId
						+ "' nicht gefunden oder abgelaufen. Bitte neu starten.");
		}
		return session;
	}

	private void persistProgress(Long runId, Question question,  AnswerResultDto result,
                                 AnswerRequest request)
	{
        GameRun run = this.gameRunRepo.findById(runId)
                .orElseThrow(() -> new NoSuchElementException("Run " + runId + " nicht gefunden."));

        QuestionProgress progress = this.questionProgressRepo
                .findByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException(
                        "QuestionProgress für runId=" + runId
                                + " und questionId=" + question.getQuestionId()
                                + " nicht gefunden."));

        progress.setStatus(result.isCorrect() ? ProgressStatus.CORRECT : ProgressStatus.WRONG);
		progress.setAnsweredAt(LocalDateTime.now());
        this.questionProgressRepo.save(progress);

        if (question.getQuestionType() == QuestionType.GAP)
        {
            this.runGapAnswerRepo.deleteByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId());

            Map<Long, GapField> gapFieldMap = question.getGapFields().stream()
                    .collect(Collectors.toMap(GapField::getGapId, gf -> gf));

            if (request.getGapAnswers() != null)
            {
                for (var entry : request.getGapAnswers())
                {
                    GapField gapField = gapFieldMap.get(entry.getGapId());
                    if (gapField == null)
                    {
                        throw new IllegalArgumentException("Ungültige gapId: " + entry.getGapId());
                    }

                    GapOption selectedOption = gapField.getGapOptions().stream()
                            .filter(o -> o.getGapOptionId().equals(entry.getSelectedGapOptionId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Ungültige selectedGapOptionId: " + entry.getSelectedGapOptionId()));

                    this.runGapAnswerRepo.save(
                            new RunGapAnswer(run, question, gapField, selectedOption, LocalDateTime.now()));
                }
            }
        }
        else
        {
            this.runSelectedAnswerRepo.deleteByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId());

            List<Long> selectedIds = request.getSelectedAnswerIds() != null
                    ? request.getSelectedAnswerIds()
                    : Collections.emptyList();

            Map<Long, AnswerOption> answerMap = question.getAnswerOptions().stream()
                    .collect(Collectors.toMap(AnswerOption::getAnswerId, a -> a));

            for (Long selectedId : selectedIds)
            {
                AnswerOption answerOption = answerMap.get(selectedId);
                if (answerOption == null)
                {
                    throw new IllegalArgumentException("Ungültige answerId: " + selectedId);
                }

                this.runSelectedAnswerRepo.save(new RunSelectedAnswer(run, question, answerOption));
            }
        }
	}

	// ================================================================
	// Private Status-Builder
	// ================================================================

	/**
	 * Wertet eine Antwort aus, schreibt das Ergebnis in die Session
     * und persistiert es in der DB.
	 *
	 * @param sessionId Session-ID (aus dem Frontend)
	 * @param roomId    1..7
	 * @param request   Antwort des Users
	 */
	@Transactional
	public AnswerResultDto submitAnswer(String sessionId, int roomId, AnswerRequest request)
	{
		QuizSession session = getSession(sessionId);
		RoomSession room = session.getRooms().get(roomId);

		if (room == null)
		{
			throw new NoSuchElementException("Raum " + roomId + " in Session nicht gefunden.");
		}

		// Frage aus der DB holen (für Auswertung brauchen wir is_correct)
		Question question = this.questionRepo.findById(request.getQuestionId())
					.orElseThrow(() -> new NoSuchElementException(
								"Frage " + request.getQuestionId() + " nicht gefunden."));

        if (room.getQuestion(request.getQuestionId()) == null)
        {
            throw new IllegalArgumentException(
                    "Frage " + request.getQuestionId() + " gehört nicht zu Raum " + roomId + ".");
        }

		// Auswerten je nach Typ
		AnswerResultDto result = switch (question.getQuestionType()) {
		case MC, TF -> evaluateMcTf(question, request);
		case GAP -> evaluateGap(question, request);
		default -> throw new IllegalArgumentException(
					"Unbekannter Fragetyp: " + question.getQuestionType());
		};

		// Ergebnis in die Session schreiben
		room.recordResult(question.getQuestionId(), result.isCorrect(), result.getPointsEarned());

		// DB-Persistierung wenn User eingeloggt
		persistProgress(session.getRunId(), question, result, request);

		return result;
	}

	// ================================================================
	// Innere Klasse: SessionOverviewDto
	// ================================================================

	/**
	 * Wechselt den aktiven Raum in der Session. Gibt den aktuellen Stand des Raums
	 * zurück (erste unbeantwortete Frage).
	 */
	public RoomStartDto switchRoom(String sessionId, int roomId)
	{
		QuizSession session = getSession(sessionId);
		session.setActiveRoomId(roomId);

		RoomSession room = session.activeRoom();
		QuestionDto currentQuestion = room.currentQuestion();

		RoomStartDto dto = new RoomStartDto();
		dto.setStatus(buildRoomStatus(room));
		dto.setFirstQuestion(currentQuestion);
		dto.setQuestionSequence(room.getQuestionSequence());
		return dto;
	}

    // Hilfsmethode um alle Fragen für diesen Run, also aus dieser Session in der Datenbank zu speichern
    private void persistInitialQuestionProgress(GameRun run, QuizSession session)
    {
        for (RoomSession room : session.getRooms().values())
        {
            List<Long> sequence = room.getQuestionSequence();

            for (int i = 0; i < sequence.size(); i++)
            {
                Long questionId = sequence.get(i);

                Question question = this.questionRepo.findById(questionId)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Frage " + questionId + " nicht gefunden."));

                QuestionProgress progress = new QuestionProgress(
                        run,
                        question,
                        room.getRoomId(),
                        i + 1,
                        ProgressStatus.OPEN,
                        null
                );

                this.questionProgressRepo.save(progress);
            }
        }
    }
}