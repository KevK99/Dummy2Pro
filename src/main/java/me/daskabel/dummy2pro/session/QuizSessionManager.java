package me.daskabel.dummy2pro.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import me.daskabel.dummy2pro.model.*;
import me.daskabel.dummy2pro.repository.*;
import me.daskabel.dummy2pro.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.daskabel.dummy2pro.controller.QuizSessionGenerator;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.dto.RunReviewDto;
import me.daskabel.dummy2pro.session.QuizSession.RoomSession;
import me.daskabel.dummy2pro.service.RoomIntroDialogs;

/**
 * Verwaltet alle aktiven QuizSessions im Arbeitsspeicher.
 *
 * Verantwortlichkeiten:
 * - Sessions anlegen (über QuizSessionGenerator) und speichern
 * - Aktuelle Frage liefern
 * - Antworten auswerten (MC / TF / GAP) und Ergebnis in die Session schreiben
 * - Fortschritt in die DB persistieren
 * - Session-Status (Raum-Übersicht, Gesamt-Punkte) liefern
 * - Abgelaufene Sessions aufräumen (einfaches TTL-Konzept)
 *
 * Speicherstruktur:
 * - sessionId -> QuizSession
 * - runId -> sessionId
 *
 * Hinweis:
 * Der persistente Spielstand ist fachlich der GameRun.
 * Die QuizSession ist nur die geladene Laufzeit-/RAM-Repräsentation.
 */
@Service
public class QuizSessionManager
{

    public static class SessionOverviewDto
    {
        private String sessionId;
        private String username;
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

        public String getUsername()
        {
            return this.username;
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

        public void setUsername(String username)
        {
            this.username = username;
        }
    }

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

    private List<RoomStatusDto> buildRoomStatusesFromDatabase(Long runId)
    {
        Map<Integer, QuestionProgressRepository.RoomProgressSummary> summaryByRoom =
                this.questionProgressRepo.summarizeRoomProgressByRunId(runId).stream()
                        .collect(Collectors.toMap(
                                QuestionProgressRepository.RoomProgressSummary::getRoomId,
                                summary -> summary
                        ));

        List<Theme> themes = this.generator.getThemesOrdered();
        List<RoomStatusDto> roomStatuses = new ArrayList<>();

        for (int i = 0; i < themes.size(); i++)
        {
            int roomId = i + 1;
            Theme theme = themes.get(i);

            QuestionProgressRepository.RoomProgressSummary summary = summaryByRoom.get(roomId);

            int totalQuestions = summary != null ? Math.toIntExact(summary.getTotalQuestions()) : 0;
            int answeredQuestions = summary != null ? Math.toIntExact(summary.getAnsweredQuestions()) : 0;
            int correctAnswers = summary != null ? Math.toIntExact(summary.getCorrectAnswers()) : 0;
            int wrongAnswers = summary != null ? Math.toIntExact(summary.getWrongAnswers()) : 0;
            int totalPoints = summary != null ? Math.toIntExact(summary.getTotalPoints()) : 0;
            int earnedPoints = summary != null ? Math.toIntExact(summary.getEarnedPoints()) : 0;

            RoomStatusDto status = new RoomStatusDto();
            status.setRoomId(roomId);
            status.setThemeName(theme.getName());
            status.setTotalQuestions(totalQuestions);
            status.setAnsweredQuestions(answeredQuestions);
            status.setCorrectAnswers(correctAnswers);
            status.setWrongAnswers(wrongAnswers);
            status.setOpenQuestions(Math.max(0, totalQuestions - answeredQuestions));
            status.setTotalPoints(totalPoints);
            status.setEarnedPoints(earnedPoints);
            status.setCompletionPercent(
                    totalQuestions > 0
                            ? Math.round(((double) answeredQuestions / totalQuestions) * 1000.0) / 10.0
                            : 0.0
            );
            status.setMedal(calculateMedal(correctAnswers, totalQuestions));

            roomStatuses.add(status);
        }

        return roomStatuses;
    }

    @Transactional
    public RoomStatusDto prepareRoom(String sessionId, int roomId)
    {
        QuizSession session = getSession(sessionId);
        RoomSession preparedRoom = ensureRoomPrepared(session, roomId);
        return buildRoomStatus(preparedRoom);
    }

    private RoomSession ensureRoomPrepared(QuizSession session, int roomId)
    {
        RoomSession existingRoom = session.getRoom(roomId);

        if (existingRoom == null)
        {
            throw new NoSuchElementException("Raum " + roomId + " in Session nicht gefunden.");
        }

        if (!existingRoom.getQuestionSequence().isEmpty())
        {
            return existingRoom;
        }

        String lockKey = session.getRunId() + ":" + roomId;
        Object lock = this.roomPreparationLocks.computeIfAbsent(lockKey, key -> new Object());

        synchronized (lock)
        {
            try
            {
                RoomSession currentRoom = session.getRoom(roomId);

                if (currentRoom == null)
                {
                    throw new NoSuchElementException("Raum " + roomId + " in Session nicht gefunden.");
                }

                if (!currentRoom.getQuestionSequence().isEmpty())
                {
                    return currentRoom;
                }

                List<QuestionProgress> roomProgressEntries =
                        this.questionProgressRepo.findByRunIdAndRoomIdOrderByQuestionOrder(
                                session.getRunId(),
                                roomId
                        );

                if (!roomProgressEntries.isEmpty())
                {
                    RoomSession restoredRoom = buildRoomSessionFromProgressEntries(roomId, roomProgressEntries);
                    session.replaceRoom(restoredRoom);
                    return restoredRoom;
                }

                List<Theme> themes = this.generator.getThemesOrdered();
                if (roomId < 1 || roomId > themes.size())
                {
                    throw new IllegalArgumentException("Ungültige roomId: " + roomId);
                }

                Theme theme = themes.get(roomId - 1);
                RoomSession preparedRoom = this.generator.buildRoomSession(theme, roomId);

                GameRun run = this.gameRunRepo.findById(session.getRunId())
                        .orElseThrow(() -> new NoSuchElementException(
                                "Run " + session.getRunId() + " nicht gefunden."));

                persistInitialQuestionProgressForRoom(run, preparedRoom);

                session.replaceRoom(preparedRoom);
                return preparedRoom;
            }
            finally
            {
                this.roomPreparationLocks.remove(lockKey, lock);
            }
        }
    }

    private void persistInitialQuestionProgressForRoom(GameRun run, RoomSession room)
    {
        List<QuestionProgress> progressEntries = new ArrayList<>();

        for (int i = 0; i < room.getQuestionSequence().size(); i++)
        {
            Long questionId = room.getQuestionSequence().get(i);
            Question questionRef = this.questionRepo.getReferenceById(questionId);

            progressEntries.add(new QuestionProgress(
                    run,
                    questionRef,
                    room.getRoomId(),
                    i + 1,
                    ProgressStatus.OPEN,
                    null
            ));
        }

        if (!progressEntries.isEmpty())
        {
            this.questionProgressRepo.saveAll(progressEntries);
        }
    }

    private static String calculateMedal(int correctAnswers, int totalQuestions)
    {
        if (totalQuestions <= 0)
        {
            return "NONE";
        }

        double ratio = (double) correctAnswers / totalQuestions;

        if (ratio >= 1.00)
        {
            return "GOLD";
        }
        if (ratio >= 0.75)
        {
            return "SILVER";
        }
        if (ratio >= 0.50)
        {
            return "BRONZE";
        }
        return "NONE";
    }

    // sessionId -> QuizSession
    private final Map<String, QuizSession> sessions = new ConcurrentHashMap<>();

    // runId -> sessionId
    private final Map<Long, String> runSessionMap = new ConcurrentHashMap<>();

    // Optionaler Komfort-Cache:
    // userId -> zuletzt geladene oder neu erzeugte sessionId
    // Fachlich führend ist aber runId -> sessionId.
    private final Map<Long, String> userSessionMap = new ConcurrentHashMap<>();

    // Schutz gegen doppelte Raumvorbereitung bei parallelen Requests
    // (z. B. Dashboard-Prepare + echter Raumaufruf gleichzeitig).
    private final Map<String, Object> roomPreparationLocks = new ConcurrentHashMap<>();

    private final QuizSessionGenerator generator;
    private final QuestionRepository questionRepo;
    private final GameRunRepository gameRunRepo;
    private final QuestionProgressRepository questionProgressRepo;
    private final RunSelectedAnswerRepository runSelectedAnswerRepo;
    private final RunGapAnswerRepository runGapAnswerRepo;
    private final UserRepository userRepo;

    public QuizSessionManager(
            QuizSessionGenerator generator,
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
     * Startet ausdrücklich einen neuen Spielstand:
     * - legt GameRun an
     * - erzeugt neue Session
     * - speichert initialen OPEN-Fortschritt
     */
    @Transactional
    public QuizSession createNewRunSession(Long userId)
    {
        if (userId == null)
        {
            throw new IllegalArgumentException("userId darf nicht null sein.");
        }

        GameRun run = new GameRun();
        run.setUser(this.userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User " + userId + " nicht gefunden.")));
        run.setStartedAt(LocalDateTime.now());
        run.setFinishedAt(null);
        run = this.gameRunRepo.save(run);

        QuizSession session = this.generator.generateSkeleton(userId, run.getRunId());

        this.sessions.put(session.getSessionId(), session);
        this.runSessionMap.put(run.getRunId(), session.getSessionId());

        // Cache für den zuletzt geladenen/erzeugten Spielstand des Users
        this.userSessionMap.put(userId, session.getSessionId());

        return session;
    }

    /**
     * ALT-Methode für alten Code.
     * Bedeutet ausdrücklich:
     * neuer Spielstand + neue Session.
     *
     * Für bestehende Spielstände muss loadSessionForRun(runId) verwendet werden.
     */
    @Transactional
    public QuizSession createSession(Long userId)
    {
        return createNewRunSession(userId);
    }

    /**
     * Lädt eine Session zu einem bestehenden Spielstand.
     *
     * Wenn die Session für den Run bereits im RAM liegt, wird sie direkt zurückgegeben.
     * Andernfalls wird sie aus den gespeicherten QuestionProgress-Daten rekonstruiert.
     */

    @Transactional(readOnly = true)
    public QuizSession loadSessionForRun(Long runId)
    {
        String sessionId = this.runSessionMap.get(runId);
        if (sessionId != null && this.sessions.containsKey(sessionId))
        {
            return this.sessions.get(sessionId);
        }

        GameRun run = this.gameRunRepo.findById(runId)
                .orElseThrow(() -> new NoSuchElementException("Run " + runId + " nicht gefunden."));

        QuizSession session = this.generator.generateSkeleton(run.getUser().getUserId(), runId);

        List<RoomStatusDto> roomStatuses = buildRoomStatusesFromDatabase(runId);

        int activeRoomId = roomStatuses.stream()
                .filter(room -> room.getTotalQuestions() == 0
                        || room.getAnsweredQuestions() < room.getTotalQuestions())
                .map(RoomStatusDto::getRoomId)
                .findFirst()
                .orElse(roomStatuses.isEmpty()
                        ? 1
                        : roomStatuses.get(roomStatuses.size() - 1).getRoomId());

        session.setActiveRoomId(activeRoomId);

        this.sessions.put(session.getSessionId(), session);
        this.runSessionMap.put(runId, session.getSessionId());
        this.userSessionMap.put(run.getUser().getUserId(), session.getSessionId());

        return session;
    }

    public QuizSession getSession(String sessionId)
    {
        QuizSession session = this.sessions.get(sessionId);
        if (session == null)
        {
            throw new NoSuchElementException(
                    "Session '" + sessionId + "' nicht gefunden oder abgelaufen. Bitte neu starten.");
        }

        session.touchSession();
        return session;
    }

    public int getActiveSessionCount()
    {
        return this.sessions.size();
    }

    @Transactional
    public QuestionDto advance(String sessionId, int roomId)
    {
        QuizSession session = getSession(sessionId);
        RoomSession room = ensureRoomPrepared(session, roomId);

        if (room.isCompleted())
        {
            return null;
        }

        QuestionDto currentQuestion = room.currentQuestion();
        if (currentQuestion != null && !room.isAnswered(currentQuestion.getQuestionId()))
        {
            throw new IllegalStateException("Die aktuelle Frage muss zuerst beantwortet werden.");
        }

        boolean hasNext = room.advance();
        return hasNext ? room.currentQuestion() : null;
    }

    @Transactional(readOnly = true)
    public SessionOverviewDto getOverview(String sessionId)
    {
        QuizSession session = getSession(sessionId);

        List<RoomStatusDto> roomStatuses = buildRoomStatusesFromDatabase(session.getRunId());

        int totalEarnedPoints = roomStatuses.stream()
                .mapToInt(RoomStatusDto::getEarnedPoints)
                .sum();

        int totalMaxPoints = roomStatuses.stream()
                .mapToInt(RoomStatusDto::getTotalPoints)
                .sum();

        int totalCorrect = roomStatuses.stream()
                .mapToInt(RoomStatusDto::getCorrectAnswers)
                .sum();

        int totalWrong = roomStatuses.stream()
                .mapToInt(RoomStatusDto::getWrongAnswers)
                .sum();

        boolean fullyCompleted = !roomStatuses.isEmpty() && roomStatuses.stream()
                .allMatch(room -> room.getTotalQuestions() > 0
                        && room.getAnsweredQuestions() >= room.getTotalQuestions());

        String username = this.userRepo.findById(session.getUserId())
                .map(User::getUsername)
                .orElse("Unbekannt");

        SessionOverviewDto overview = new SessionOverviewDto();
        overview.setSessionId(sessionId);
        overview.setUsername(username);
        overview.setTotalEarnedPoints(totalEarnedPoints);
        overview.setTotalMaxPoints(totalMaxPoints);
        overview.setTotalCorrect(totalCorrect);
        overview.setTotalWrong(totalWrong);
        overview.setFullyCompleted(fullyCompleted);
        overview.setRooms(roomStatuses);
        return overview;
    }

    @Transactional
    public RoomStartDto getRoomState(String sessionId, int roomId)
    {
        QuizSession session = getSession(sessionId);
        RoomSession room = ensureRoomPrepared(session, roomId);

        RoomStartDto dto = new RoomStartDto();
        dto.setStatus(buildRoomStatus(room));
        dto.setFirstQuestion(room.isCompleted() ? null : room.currentQuestion());
        dto.setQuestionSequence(room.getQuestionSequence());
        dto.setIntroDialog(RoomIntroDialogs.getDialogForRoom(roomId));
        return dto;
    }

    @Transactional
    public RoomStatusDto getRoomStatus(String sessionId, int roomId)
    {
        QuizSession session = getSession(sessionId);
        RoomSession room = ensureRoomPrepared(session, roomId);
        return buildRoomStatus(room);
    }

    @Transactional
    public RoomStartDto switchRoom(String sessionId, int roomId)
    {
        QuizSession session = getSession(sessionId);
        RoomSession room = ensureRoomPrepared(session, roomId);
        session.setActiveRoomId(roomId);

        QuestionDto currentQuestion = room.currentQuestion();

        RoomStartDto dto = new RoomStartDto();
        dto.setStatus(buildRoomStatus(room));
        dto.setFirstQuestion(currentQuestion);
        dto.setQuestionSequence(room.getQuestionSequence());
        dto.setIntroDialog(RoomIntroDialogs.getDialogForRoom(roomId));
        return dto;
    }

    @Transactional
    public AnswerResultDto submitAnswer(String sessionId, int roomId, AnswerRequest request)
    {
        QuizSession session = getSession(sessionId);
        RoomSession room = ensureRoomPrepared(session, roomId);

        QuestionDto currentQuestion = room.currentQuestion();

        if (currentQuestion == null)
        {
            throw new IllegalStateException("Der Raum ist bereits abgeschlossen.");
        }

        if (!currentQuestion.getQuestionId().equals(request.getQuestionId()))
        {
            throw new IllegalArgumentException("Es darf nur die aktuelle Frage beantwortet werden.");
        }

        if (room.isAnswered(request.getQuestionId()))
        {
            throw new IllegalStateException("Diese Frage wurde bereits beantwortet.");
        }

        QuestionDto cachedQuestion = room.getQuestion(request.getQuestionId());
        if (cachedQuestion == null)
        {
            throw new IllegalArgumentException(
                    "Frage " + request.getQuestionId() + " gehört nicht zu Raum " + roomId + ".");
        }

        Question question = switch (cachedQuestion.getQuestionType())
        {
            case MC, TF -> this.questionRepo.findByQuestionIdWithAnswers(request.getQuestionId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Frage " + request.getQuestionId() + " nicht gefunden."));
            case GAP -> this.questionRepo.findByQuestionIdWithGaps(request.getQuestionId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Frage " + request.getQuestionId() + " nicht gefunden."));
            default -> throw new IllegalArgumentException(
                    "Unbekannter Fragetyp: " + cachedQuestion.getQuestionType());
        };

        AnswerResultDto result = switch (question.getQuestionType())
        {
            case MC, TF -> RoomService.evaluateMcTf(question, request);
            case GAP -> RoomService.evaluateGap(question, request);
            default -> throw new IllegalArgumentException(
                    "Unbekannter Fragetyp: " + question.getQuestionType());
        };

        room.recordResult(question.getQuestionId(), result.isCorrect(), result.getPointsEarned());
        markRoomCompletedIfFinished(room);
        persistProgress(session.getRunId(), question, result, request);
        markRunFinishedIfCompleted(session);

        return result;
    }

    public int cleanupExpiredSessions()
    {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(SESSION_TTL_HOURS);
        int removed = 0;

        Iterator<Map.Entry<String, QuizSession>> it = this.sessions.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, QuizSession> entry = it.next();
            QuizSession session = entry.getValue();

            if (session.getLastActivityAt().isBefore(cutoff))
            {
                Long userId = session.getUserId();
                Long runId = session.getRunId();

                if (userId != null)
                {
                    this.userSessionMap.remove(userId);
                }
                if (runId != null)
                {
                    this.runSessionMap.remove(runId);
                }

                it.remove();
                removed++;
            }
        }

        return removed;
    }

    public void removeRunSession(Long runId)
    {
        String sessionId = this.runSessionMap.remove(runId);

        if (sessionId == null)
        {
            return;
        }

        QuizSession removedSession = this.sessions.remove(sessionId);

        if (removedSession != null)
        {
            Long userId = removedSession.getUserId();
            String mappedSessionId = this.userSessionMap.get(userId);

            if (sessionId.equals(mappedSessionId))
            {
                this.userSessionMap.remove(userId);
            }
        }
    }

    private void persistProgress(
            Long runId,
            Question question,
            AnswerResultDto result,
            AnswerRequest request)
    {
        LocalDateTime answeredAt = LocalDateTime.now();

        int updated = this.questionProgressRepo.updateStatusAndAnsweredAt(
                runId,
                question.getQuestionId(),
                result.isCorrect() ? ProgressStatus.CORRECT : ProgressStatus.WRONG,
                answeredAt
        );

        if (updated == 0)
        {
            throw new NoSuchElementException(
                    "QuestionProgress für runId=" + runId
                            + " und questionId=" + question.getQuestionId()
                            + " nicht gefunden.");
        }

        GameRun run = this.gameRunRepo.getReferenceById(runId);

        if (question.getQuestionType() == QuestionType.GAP)
        {
            this.runGapAnswerRepo.deleteByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId());

            Map<Long, GapField> gapFieldMap = question.getGapFields().stream()
                    .collect(Collectors.toMap(GapField::getGapId, gf -> gf));

            List<RunGapAnswer> gapAnswersToSave = new ArrayList<>();

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

                    gapAnswersToSave.add(
                            new RunGapAnswer(run, question, gapField, selectedOption, answeredAt)
                    );
                }
            }

            if (!gapAnswersToSave.isEmpty())
            {
                this.runGapAnswerRepo.saveAll(gapAnswersToSave);
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

            List<RunSelectedAnswer> selectedAnswersToSave = new ArrayList<>();

            for (Long selectedId : selectedIds)
            {
                AnswerOption answerOption = answerMap.get(selectedId);
                if (answerOption == null)
                {
                    throw new IllegalArgumentException("Ungültige answerId: " + selectedId);
                }

                selectedAnswersToSave.add(new RunSelectedAnswer(run, question, answerOption));
            }

            if (!selectedAnswersToSave.isEmpty())
            {
                this.runSelectedAnswerRepo.saveAll(selectedAnswersToSave);
            }
        }
    }

    private RoomSession buildRoomSessionFromProgressEntries(
            int roomId,
            List<QuestionProgress> roomProgressEntries)
    {
        if (roomProgressEntries == null || roomProgressEntries.isEmpty())
        {
            throw new IllegalArgumentException("roomProgressEntries darf nicht leer sein.");
        }

        List<Long> questionSequence = roomProgressEntries.stream()
                .map(progress -> progress.getQuestion().getQuestionId())
                .toList();

        List<Question> orderedQuestions = this.generator.loadQuestionsByIdsOrdered(questionSequence);

        Map<Long, Question> questionMap = orderedQuestions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Map<Long, QuestionDto> questionCache = new ConcurrentHashMap<>();
        int maxPoints = 0;

        for (Question question : orderedQuestions)
        {
            maxPoints += question.getPoints();
            QuestionDto dto = toQuestionDtoForRestore(question);
            questionCache.put(question.getQuestionId(), dto);
        }

        List<Theme> themes = this.generator.getThemesOrdered();
        String themeName = (roomId >= 1 && roomId <= themes.size())
                ? themes.get(roomId - 1).getName()
                : "Unbekannt";

        RoomSession room = new RoomSession(
                roomId,
                themeName,
                questionSequence,
                questionCache,
                maxPoints
        );

        int answeredCount = 0;

        for (QuestionProgress progress : roomProgressEntries)
        {
            if (progress.getStatus() == ProgressStatus.CORRECT
                    || progress.getStatus() == ProgressStatus.WRONG)
            {
                Long questionId = progress.getQuestion().getQuestionId();
                Question question = questionMap.get(questionId);
                if (question == null)
                {
                    throw new NoSuchElementException("Frage " + questionId + " nicht gefunden.");
                }

                boolean correct = progress.getStatus() == ProgressStatus.CORRECT;
                int pointsEarned = correct ? question.getPoints() : 0;

                room.restoreResult(
                        questionId,
                        correct,
                        pointsEarned,
                        progress.getAnsweredAt()
                );

                answeredCount++;
            }
        }

        boolean roomCompleted = answeredCount >= room.getTotalQuestions()
                && room.getTotalQuestions() > 0;

        if (roomCompleted)
        {
            room.setCompleted(true);
            room.setCurrentIndex(Math.max(0, room.getTotalQuestions() - 1));
        }
        else
        {
            room.setCompleted(false);
            room.setCurrentIndex(answeredCount);
        }

        return room;
    }

    private void markRunFinishedIfCompleted(QuizSession session)
    {
        if (!session.isFullyCompleted())
        {
            return;
        }

        GameRun run = this.gameRunRepo.findById(session.getRunId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Run " + session.getRunId() + " nicht gefunden."));

        if (run.getFinishedAt() == null)
        {
            run.setFinishedAt(LocalDateTime.now());
            this.gameRunRepo.save(run);
        }
    }

    private void markRoomCompletedIfFinished(RoomSession room)
    {
        if (room.getAnsweredCount() >= room.getTotalQuestions() && room.getTotalQuestions() > 0)
        {
            room.setCompleted(true);
        }
    }

    private QuestionDto toQuestionDtoForRestore(Question question)
    {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(question.getQuestionId());
        dto.setQuestionType(question.getQuestionType());
        dto.setStartText(question.getStartText());
        dto.setImageUrl(question.getImageUrl());
        dto.setEndText(question.getEndText());
        dto.setAllowsMultiple(question.getAllowsMultiple());
        dto.setPoints(question.getPoints());

        if (question.getAnswerOptions() != null)
        {
            List<me.daskabel.dummy2pro.dto.RoomDtos.AnswerOptionDto> answerOptionDtos =
                    question.getAnswerOptions().stream()
                            .map(answer -> {
                                me.daskabel.dummy2pro.dto.RoomDtos.AnswerOptionDto optionDto =
                                        new me.daskabel.dummy2pro.dto.RoomDtos.AnswerOptionDto();
                                optionDto.setAnswerId(answer.getAnswerId());
                                optionDto.setOptionText(answer.getOptionText());
                                optionDto.setOptionOrder(answer.getOptionOrder());
                                return optionDto;
                            })
                            .toList();

            dto.setAnswerOptions(answerOptionDtos);
        }

        if (question.getGapFields() != null)
        {
            List<me.daskabel.dummy2pro.dto.RoomDtos.GapFieldDto> gapFieldDtos =
                    question.getGapFields().stream()
                            .map(gapField -> {
                                me.daskabel.dummy2pro.dto.RoomDtos.GapFieldDto gapFieldDto =
                                        new me.daskabel.dummy2pro.dto.RoomDtos.GapFieldDto();
                                gapFieldDto.setGapId(gapField.getGapId());
                                gapFieldDto.setGapIndex(gapField.getGapIndex());
                                gapFieldDto.setTextBefore(gapField.getTextBefore());
                                gapFieldDto.setTextAfter(gapField.getTextAfter());

                                if (gapField.getGapOptions() != null)
                                {
                                    List<me.daskabel.dummy2pro.dto.RoomDtos.GapOptionDto> gapOptionDtos =
                                            gapField.getGapOptions().stream()
                                                    .map(option -> {
                                                        me.daskabel.dummy2pro.dto.RoomDtos.GapOptionDto optionDto =
                                                                new me.daskabel.dummy2pro.dto.RoomDtos.GapOptionDto();
                                                        optionDto.setGapOptionId(option.getGapOptionId());
                                                        optionDto.setOptionText(option.getOptionText());
                                                        optionDto.setOptionOrder(option.getOptionOrder());
                                                        return optionDto;
                                                    })
                                                    .toList();

                                    gapFieldDto.setGapOptions(gapOptionDtos);
                                }

                                return gapFieldDto;
                            })
                            .toList();

            dto.setGapFields(gapFieldDtos);
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public RunReviewDto getRunReview(String sessionId)
    {
        QuizSession session = getSession(sessionId);
        Long runId = session.getRunId();

        List<QuestionProgress> progressEntries =
                this.questionProgressRepo.findDetailedByRunIdOrderByRoomIdAscQuestionOrderAsc(runId);

        RunReviewDto dto = new RunReviewDto();
        dto.setRunId(runId);
        dto.setUsername(this.userRepo.findById(session.getUserId())
                .map(User::getUsername)
                .orElse("Unbekannt"));

        if (progressEntries.isEmpty())
        {
            dto.setRooms(List.of());
            return dto;
        }

        Set<Long> choiceQuestionIds = progressEntries.stream()
                .map(QuestionProgress::getQuestion)
                .filter(question -> question.getQuestionType() == QuestionType.MC || question.getQuestionType() == QuestionType.TF)
                .map(Question::getQuestionId)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> gapQuestionIds = progressEntries.stream()
                .map(QuestionProgress::getQuestion)
                .filter(question -> question.getQuestionType() == QuestionType.GAP)
                .map(Question::getQuestionId)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Long, Question> questionsById = new HashMap<>();

        if (!choiceQuestionIds.isEmpty())
        {
            this.questionRepo.findByQuestionIdsWithAnswers(new ArrayList<>(choiceQuestionIds))
                    .forEach(question -> questionsById.put(question.getQuestionId(), question));
        }

        if (!gapQuestionIds.isEmpty())
        {
            this.questionRepo.findByQuestionIdsWithGaps(new ArrayList<>(gapQuestionIds))
                    .forEach(question -> questionsById.put(question.getQuestionId(), question));
        }

        Map<Long, Set<Long>> selectedChoiceIdsByQuestionId = this.runSelectedAnswerRepo.findDetailedByRunId(runId).stream()
                .collect(Collectors.groupingBy(
                        answer -> answer.getQuestion().getQuestionId(),
                        Collectors.mapping(
                                answer -> answer.getAnswerOption().getAnswerId(),
                                Collectors.toSet()
                        )
                ));

        Map<Long, Map<Long, RunGapAnswer>> gapAnswersByQuestionId = new HashMap<>();

        for (RunGapAnswer answer : this.runGapAnswerRepo.findDetailedByRunId(runId))
        {
            gapAnswersByQuestionId
                    .computeIfAbsent(answer.getQuestion().getQuestionId(), ignored -> new HashMap<>())
                    .put(answer.getGapField().getGapId(), answer);
        }

        Map<Integer, String> themeNameByRoomId = new HashMap<>();
        List<Theme> themes = this.generator.getThemesOrdered();

        for (int i = 0; i < themes.size(); i++)
        {
            themeNameByRoomId.put(i + 1, themes.get(i).getName());
        }

        Map<Integer, List<QuestionProgress>> progressByRoom = progressEntries.stream()
                .collect(Collectors.groupingBy(
                        QuestionProgress::getRoomId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RunReviewDto.RoomReviewDto> rooms = new ArrayList<>();

        for (Map.Entry<Integer, List<QuestionProgress>> roomEntry : progressByRoom.entrySet())
        {
            int roomId = roomEntry.getKey();
            List<QuestionProgress> roomProgressEntries = roomEntry.getValue();

            int totalQuestions = roomProgressEntries.size();
            int correctAnswers = (int) roomProgressEntries.stream()
                    .filter(progress -> progress.getStatus() == ProgressStatus.CORRECT)
                    .count();
            int wrongAnswers = (int) roomProgressEntries.stream()
                    .filter(progress -> progress.getStatus() == ProgressStatus.WRONG)
                    .count();
            int openQuestions = totalQuestions - correctAnswers - wrongAnswers;

            RunReviewDto.RoomReviewDto roomDto = new RunReviewDto.RoomReviewDto();
            roomDto.setRoomId(roomId);
            roomDto.setThemeName(themeNameByRoomId.getOrDefault(roomId, "Raum " + roomId));
            roomDto.setMedal(calculateMedal(correctAnswers, totalQuestions));
            roomDto.setTotalQuestions(totalQuestions);
            roomDto.setCorrectAnswers(correctAnswers);
            roomDto.setWrongAnswers(wrongAnswers);
            roomDto.setOpenQuestions(openQuestions);

            List<RunReviewDto.QuestionReviewDto> questionDtos = new ArrayList<>();

            for (QuestionProgress progress : roomProgressEntries)
            {
                Question question = questionsById.get(progress.getQuestion().getQuestionId());

                if (question == null)
                {
                    throw new NoSuchElementException(
                            "Frage " + progress.getQuestion().getQuestionId() + " für Review nicht gefunden.");
                }

                RunReviewDto.QuestionReviewDto questionDto = new RunReviewDto.QuestionReviewDto();
                questionDto.setQuestionId(question.getQuestionId());
                questionDto.setQuestionOrder(progress.getQuestionOrder());
                questionDto.setQuestionType(question.getQuestionType().name());
                questionDto.setQuestionText(buildQuestionReviewText(question));
                questionDto.setImageUrl(question.getImageUrl());
                questionDto.setPoints(question.getPoints());
                questionDto.setStatus(progress.getStatus().name());
                questionDto.setAnsweredAt(progress.getAnsweredAt());

                if (question.getQuestionType() == QuestionType.GAP)
                {
                    questionDto.setChoices(List.of());
                    questionDto.setGaps(buildGapReviews(
                            question,
                            gapAnswersByQuestionId.getOrDefault(question.getQuestionId(), Collections.emptyMap())
                    ));
                }
                else
                {
                    questionDto.setChoices(buildChoiceReviews(
                            question,
                            selectedChoiceIdsByQuestionId.getOrDefault(question.getQuestionId(), Collections.emptySet())
                    ));
                    questionDto.setGaps(List.of());
                }

                questionDtos.add(questionDto);
            }

            roomDto.setQuestions(questionDtos);
            rooms.add(roomDto);
        }

        dto.setRooms(rooms);
        return dto;
    }

    private List<RunReviewDto.ChoiceReviewDto> buildChoiceReviews(Question question, Set<Long> selectedIds)
    {
        if (question.getAnswerOptions() == null)
        {
            return List.of();
        }

        return question.getAnswerOptions().stream()
                .sorted((left, right) -> Integer.compare(left.getOptionOrder(), right.getOptionOrder()))
                .map(option -> {
                    RunReviewDto.ChoiceReviewDto dto = new RunReviewDto.ChoiceReviewDto();
                    dto.setAnswerId(option.getAnswerId());
                    dto.setOptionText(option.getOptionText());
                    dto.setSelected(selectedIds.contains(option.getAnswerId()));
                    dto.setCorrect(option.getIsCorrect());
                    return dto;
                })
                .toList();
    }

    private List<RunReviewDto.GapReviewDto> buildGapReviews(Question question, Map<Long, RunGapAnswer> selectedAnswersByGapId)
    {
        if (question.getGapFields() == null)
        {
            return List.of();
        }

        return question.getGapFields().stream()
                .sorted((left, right) -> Integer.compare(left.getGapIndex(), right.getGapIndex()))
                .map(gapField -> {
                    GapOption correctOption = gapField.getGapOptions().stream()
                            .filter(GapOption::getIsCorrect)
                            .findFirst()
                            .orElse(null);

                    RunGapAnswer selectedAnswer = selectedAnswersByGapId.get(gapField.getGapId());

                    String selectedText = selectedAnswer != null && selectedAnswer.getSelectedGapOption() != null
                            ? selectedAnswer.getSelectedGapOption().getOptionText()
                            : null;

                    String correctText = correctOption != null
                            ? correctOption.getOptionText()
                            : null;

                    boolean correct = selectedAnswer != null
                            && selectedAnswer.getSelectedGapOption() != null
                            && correctOption != null
                            && selectedAnswer.getSelectedGapOption().getGapOptionId().equals(correctOption.getGapOptionId());

                    RunReviewDto.GapReviewDto dto = new RunReviewDto.GapReviewDto();
                    dto.setGapId(gapField.getGapId());
                    dto.setGapIndex(gapField.getGapIndex());
                    dto.setLabel(buildGapLabel(gapField));
                    dto.setSelectedText(selectedText);
                    dto.setCorrectText(correctText);
                    dto.setCorrect(correct);
                    return dto;
                })
                .toList();
    }

    private String buildQuestionReviewText(Question question)
    {
        StringBuilder builder = new StringBuilder();

        appendQuestionPart(builder, question.getStartText());

        if (question.getQuestionType() == QuestionType.GAP && question.getGapFields() != null)
        {
            question.getGapFields().stream()
                    .sorted((left, right) -> Integer.compare(left.getGapIndex(), right.getGapIndex()))
                    .forEach(gapField -> {
                        appendQuestionPart(builder, gapField.getTextBefore());
                        appendQuestionPart(builder, "_____");
                        appendQuestionPart(builder, gapField.getTextAfter());
                    });
        }

        appendQuestionPart(builder, question.getEndText());

        return builder.toString().trim().replaceAll("\\s+", " ");
    }

    private String buildGapLabel(GapField gapField)
    {
        StringBuilder builder = new StringBuilder();

        appendQuestionPart(builder, gapField.getTextBefore());
        appendQuestionPart(builder, "_____");
        appendQuestionPart(builder, gapField.getTextAfter());

        String result = builder.toString().trim().replaceAll("\\s+", " ");
        return result.isBlank() ? "Lücke " + (gapField.getGapIndex() + 1) : result;
    }

    private void appendQuestionPart(StringBuilder builder, String part)
    {
        if (part == null || part.isBlank())
        {
            return;
        }

        if (builder.length() > 0)
        {
            builder.append(' ');
        }

        builder.append(part.trim());
    }
}