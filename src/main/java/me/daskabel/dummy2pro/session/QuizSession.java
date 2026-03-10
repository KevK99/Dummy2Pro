package me.daskabel.dummy2pro.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;

/**
 * Eine vollständige Quiz-Instanz für einen Spieler — alle 7 Räume.
 *
 * Lebt im Arbeitsspeicher (kein DB-Schema nötig). Wird vom QuizSessionGenerator
 * erzeugt und vom QuizSessionManager verwaltet.
 *
 * Aufbau:
 * QuizSession
 * └── Map<roomId, RoomSession> (7 Einträge, roomId = 1..7)
 *     └── questionSequence (geshuffelte Liste der Fragen-IDs)
 *     └── Map<questionId, QuestionResult>
 *     └── currentIndex
 *     └── completed
 */
public class QuizSession
{

    public static class QuestionResult
    {
        private final Long questionId;
        private final boolean correct;
        private final int pointsEarned;
        private final LocalDateTime answeredAt;

        public QuestionResult(Long questionId, boolean correct, int pointsEarned)
        {
            this(questionId, correct, pointsEarned, LocalDateTime.now());
        }

        public QuestionResult(Long questionId, boolean correct, int pointsEarned,
                              LocalDateTime answeredAt)
        {
            this.questionId = questionId;
            this.correct = correct;
            this.pointsEarned = pointsEarned;
            this.answeredAt = answeredAt;
        }

        public LocalDateTime getAnsweredAt()
        {
            return this.answeredAt;
        }

        public int getPointsEarned()
        {
            return this.pointsEarned;
        }

        public Long getQuestionId()
        {
            return this.questionId;
        }

        public boolean isCorrect()
        {
            return this.correct;
        }
    }

    public static class RoomSession
    {
        private final int roomId;
        private final String themeName;

        // Geshuffelte Reihenfolge der Fragen-IDs für diesen Raum
        private final List<Long> questionSequence;

        // Gecachte QuestionDtos (questionId -> dto)
        private final Map<Long, QuestionDto> questionCache;

        // Ergebnisse der beantworteten Fragen
        private final Map<Long, QuestionResult> results;

        // 0-basierter Index in questionSequence
        private int currentIndex;

        // Raum abgeschlossen?
        private boolean completed;

        // Maximale Punkte des Raums
        private final int maxPoints;

        public RoomSession(int roomId, String themeName, List<Long> questionSequence,
                           Map<Long, QuestionDto> questionCache, int maxPoints)
        {
            this.roomId = roomId;
            this.themeName = themeName;
            this.questionSequence = new ArrayList<>(questionSequence);
            this.questionCache = new HashMap<>(questionCache);
            this.results = new LinkedHashMap<>();
            this.currentIndex = 0;
            this.completed = false;
            this.maxPoints = maxPoints;
        }

        /** Rückt zur nächsten Frage vor. Gibt false zurück wenn keine mehr da. */
        public boolean advance()
        {
            if (this.currentIndex < this.questionSequence.size() - 1)
            {
                this.currentIndex++;
                return true;
            }

            this.completed = true;
            return false;
        }

        /** Gibt die aktuelle Frage zurück (null wenn Raum leer/fertig). */
        public QuestionDto currentQuestion()
        {
            if (this.completed
                    || this.questionSequence.isEmpty()
                    || this.currentIndex >= this.questionSequence.size())
            {
                return null;
            }

            Long id = this.questionSequence.get(this.currentIndex);
            QuestionDto dto = this.questionCache.get(id);

            if (dto != null)
            {
                dto.setCurrentIndex(this.currentIndex);
                dto.setTotalCount(this.questionSequence.size());
            }

            return dto;
        }

        public int getAnsweredCount()
        {
            return this.results.size();
        }

        public double getCompletionPercent()
        {
            if (this.questionSequence.isEmpty())
            {
                return 0;
            }

            return Math.round((double) getAnsweredCount() / this.questionSequence.size() * 1000.0)
                    / 10.0;
        }

        public int getCorrectCount()
        {
            return (int) this.results.values().stream().filter(QuestionResult::isCorrect).count();
        }

        public int getCurrentIndex()
        {
            return this.currentIndex;
        }

        public int getEarnedPoints()
        {
            return this.results.values().stream().mapToInt(QuestionResult::getPointsEarned).sum();
        }

        public int getMaxPoints()
        {
            return this.maxPoints;
        }

        public String getMedal()
        {
            if (this.questionSequence.isEmpty())
            {
                return "NONE";
            }

            double ratio = (double) getCorrectCount() / this.questionSequence.size();

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

        public QuestionDto getQuestion(Long questionId)
        {
            return this.questionCache.get(questionId);
        }

        public List<Long> getQuestionSequence()
        {
            return Collections.unmodifiableList(this.questionSequence);
        }

        public Map<Long, QuestionResult> getResults()
        {
            return Collections.unmodifiableMap(this.results);
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
            return this.questionSequence.size();
        }

        public int getWrongCount()
        {
            return (int) this.results.values().stream().filter(r -> !r.isCorrect()).count();
        }

        public boolean isAnswered(Long questionId)
        {
            return this.results.containsKey(questionId);
        }

        public boolean isCompleted()
        {
            return this.completed;
        }

        public boolean isLastQuestion()
        {
            return this.currentIndex >= this.questionSequence.size() - 1;
        }

        public void recordResult(Long questionId, boolean correct, int pointsEarned)
        {
            this.results.put(questionId, new QuestionResult(questionId, correct, pointsEarned));
        }

        /**
         * Für Restore aus der DB:
         * Ergebnis mit vorhandenem Zeitstempel eintragen.
         */
        public void restoreResult(Long questionId, boolean correct, int pointsEarned,
                                  LocalDateTime answeredAt)
        {
            this.results.put(questionId,
                    new QuestionResult(questionId, correct, pointsEarned, answeredAt));
        }

        /**
         * Für Restore aus der DB:
         * Aktuellen Fragenindex setzen.
         */
        public void setCurrentIndex(int currentIndex)
        {
            if (currentIndex < 0)
            {
                this.currentIndex = 0;
                return;
            }

            if (this.questionSequence.isEmpty())
            {
                this.currentIndex = 0;
                return;
            }

            if (currentIndex >= this.questionSequence.size())
            {
                this.currentIndex = this.questionSequence.size() - 1;
                return;
            }

            this.currentIndex = currentIndex;
        }

        /**
         * Für Restore aus der DB:
         * Abschlussstatus setzen.
         */
        public void setCompleted(boolean completed)
        {
            this.completed = completed;
        }
    }

    private final String sessionId;
    private final Long userId;
    private final Long runId;

    private final LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    // roomId -> RoomSession
    private final Map<Integer, RoomSession> rooms;

    // aktiver Raum
    private int activeRoomId;

    public QuizSession(Long userId, Long runId)
    {
        this.sessionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.runId = runId;
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        this.rooms = new LinkedHashMap<>();
        this.activeRoomId = 1;
    }

    public RoomSession activeRoom()
    {
        return this.rooms.get(this.activeRoomId);
    }

    public void addRoom(RoomSession roomSession)
    {
        this.rooms.put(roomSession.getRoomId(), roomSession);
        touch();
    }

    public int getActiveRoomId()
    {
        return this.activeRoomId;
    }

    public LocalDateTime getCreatedAt()
    {
        return this.createdAt;
    }

    public LocalDateTime getLastActivityAt()
    {
        return this.lastActivityAt;
    }

    public Map<Integer, RoomSession> getRooms()
    {
        return Collections.unmodifiableMap(this.rooms);
    }

    public String getSessionId()
    {
        return this.sessionId;
    }

    public int getTotalCorrect()
    {
        return this.rooms.values().stream().mapToInt(RoomSession::getCorrectCount).sum();
    }

    public int getTotalEarnedPoints()
    {
        return this.rooms.values().stream().mapToInt(RoomSession::getEarnedPoints).sum();
    }

    public int getTotalMaxPoints()
    {
        return this.rooms.values().stream().mapToInt(RoomSession::getMaxPoints).sum();
    }

    public int getTotalWrong()
    {
        return this.rooms.values().stream().mapToInt(RoomSession::getWrongCount).sum();
    }

    public Long getUserId()
    {
        return this.userId;
    }

    public Long getRunId()
    {
        return this.runId;
    }

    public boolean isFullyCompleted()
    {
        return this.rooms.values().stream().allMatch(RoomSession::isCompleted);
    }

    public void setActiveRoomId(int roomId)
    {
        if (!this.rooms.containsKey(roomId))
        {
            throw new NoSuchElementException(
                    "Raum " + roomId + " existiert nicht in dieser Session.");
        }

        this.activeRoomId = roomId;
        touch();
    }

    private void touch()
    {
        this.lastActivityAt = LocalDateTime.now();
    }
}