package me.daskabel.dummy2pro.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

    // sessionId -> QuizSession
    private final Map<String, QuizSession> sessions = new ConcurrentHashMap<>();

    // runId -> sessionId
    private final Map<Long, String> runSessionMap = new ConcurrentHashMap<>();

    // Optionaler Komfort-Cache:
    // userId -> zuletzt geladene oder neu erzeugte sessionId
    // Fachlich führend ist aber runId -> sessionId.
    private final Map<Long, String> userSessionMap = new ConcurrentHashMap<>();

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

        QuizSession session = this.generator.generate(userId, run.getRunId());
        persistInitialQuestionProgress(run, session);

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

        return restoreSessionFromRun(runId);
    }

    public QuizSession getSession(String sessionId)
    {
        QuizSession session = this.sessions.get(sessionId);
        if (session == null)
        {
            throw new NoSuchElementException(
                    "Session '" + sessionId + "' nicht gefunden oder abgelaufen. Bitte neu starten.");
        }
        return session;
    }

    public int getActiveSessionCount()
    {
        return this.sessions.size();
    }

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
        dto.setFirstQuestion(room.isCompleted() ? null : room.currentQuestion());
        dto.setQuestionSequence(room.getQuestionSequence());
        dto.setIntroDialog(RoomIntroDialogs.getDialogForRoom(roomId));
        return dto;
    }

    public RoomStatusDto getRoomStatus(String sessionId, int roomId)
    {
        QuizSession session = getSession(sessionId);
        RoomSession room = session.getRooms().get(roomId);

        if (room == null)
        {
            throw new NoSuchElementException("Raum " + roomId + " nicht gefunden.");
        }

        return buildRoomStatus(room);
    }

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

    @Transactional
    public AnswerResultDto submitAnswer(String sessionId, int roomId, AnswerRequest request)
    {
        QuizSession session = getSession(sessionId);
        RoomSession room = session.getRooms().get(roomId);

        if (room == null)
        {
            throw new NoSuchElementException("Raum " + roomId + " in Session nicht gefunden.");
        }

        Question question = this.questionRepo.findById(request.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Frage " + request.getQuestionId() + " nicht gefunden."));

        if (room.getQuestion(request.getQuestionId()) == null)
        {
            throw new IllegalArgumentException(
                    "Frage " + request.getQuestionId() + " gehört nicht zu Raum " + roomId + ".");
        }

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

    private void persistInitialQuestionProgress(GameRun run, QuizSession session)
    {
        List<Long> allQuestionIds = session.getRooms().values().stream()
                .flatMap(room -> room.getQuestionSequence().stream())
                .distinct()
                .toList();

        Map<Long, Question> questionMap = this.questionRepo.findAllById(allQuestionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        List<QuestionProgress> progressEntries = new ArrayList<>();

        for (RoomSession room : session.getRooms().values())
        {
            List<Long> sequence = room.getQuestionSequence();

            for (int i = 0; i < sequence.size(); i++)
            {
                Long questionId = sequence.get(i);

                Question question = questionMap.get(questionId);
                if (question == null)
                {
                    throw new NoSuchElementException("Frage " + questionId + " nicht gefunden.");
                }

                progressEntries.add(new QuestionProgress(
                        run,
                        question,
                        room.getRoomId(),
                        i + 1,
                        ProgressStatus.OPEN,
                        null
                ));
            }
        }

        this.questionProgressRepo.saveAll(progressEntries);
    }

    private QuizSession restoreSessionFromRun(Long runId)
    {
        GameRun run = this.gameRunRepo.findById(runId)
                .orElseThrow(() -> new NoSuchElementException("Run " + runId + " nicht gefunden."));

        List<QuestionProgress> allProgressEntries =
                this.questionProgressRepo.findByRun_RunIdOrderByRoomIdAscQuestionOrderAsc(runId);

        if (allProgressEntries.isEmpty())
        {
            throw new NoSuchElementException(
                    "Für Run " + runId + " wurden keine QuestionProgress-Einträge gefunden.");
        }

        QuizSession restoredSession = new QuizSession(run.getUser().getUserId(), runId);

        int activeRoomId = 1;
        boolean firstOpenRoomFound = false;

        Map<Integer, List<QuestionProgress>> progressByRoom = allProgressEntries.stream()
                .collect(Collectors.groupingBy(
                        QuestionProgress::getRoomId,
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<Integer, List<QuestionProgress>> entry : progressByRoom.entrySet())
        {
            Integer roomId = entry.getKey();
            List<QuestionProgress> roomProgressEntries = entry.getValue();

            RoomSession restoredRoom = buildRoomSessionFromProgressEntries(roomId, roomProgressEntries);
            restoredSession.addRoom(restoredRoom);

            boolean roomCompleted = restoredRoom.isCompleted();
            if (!roomCompleted && !firstOpenRoomFound)
            {
                activeRoomId = roomId;
                firstOpenRoomFound = true;
            }
        }

        if (!firstOpenRoomFound)
        {
            Integer lastRoomId = restoredSession.getRooms().keySet().stream()
                    .max(Integer::compareTo)
                    .orElse(1);
            activeRoomId = lastRoomId;
        }

        restoredSession.setActiveRoomId(activeRoomId);

        this.sessions.put(restoredSession.getSessionId(), restoredSession);
        this.runSessionMap.put(runId, restoredSession.getSessionId());
        this.userSessionMap.put(run.getUser().getUserId(), restoredSession.getSessionId());

        return restoredSession;
    }

    private RoomSession buildRoomSessionFromProgressEntries(
            int roomId,
            List<QuestionProgress> roomProgressEntries)
    {
        if (roomProgressEntries == null || roomProgressEntries.isEmpty())
        {
            throw new IllegalArgumentException("roomProgressEntries darf nicht leer sein.");
        }

        List<Long> questionSequence = new ArrayList<>();
        Map<Long, QuestionDto> questionCache = new ConcurrentHashMap<>();
        int maxPoints = 0;

        for (QuestionProgress progress : roomProgressEntries)
        {
            Question question = progress.getQuestion();
            Long questionId = question.getQuestionId();

            questionSequence.add(questionId);
            maxPoints += question.getPoints();

            QuestionDto dto = toQuestionDtoForRestore(question);
            questionCache.put(questionId, dto);
        }

        List<Theme> themes = roomProgressEntries.get(0).getQuestion().getThemes();
        String themeName = (themes != null && !themes.isEmpty())
                ? themes.get(0).getName()
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
                Question question = progress.getQuestion();
                Long questionId = question.getQuestionId();
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

}