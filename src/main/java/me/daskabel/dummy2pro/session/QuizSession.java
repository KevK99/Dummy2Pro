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
 * Repräsentiert eine laufende Quiz-Sitzung im Arbeitsspeicher.
 *
 * Die Klasse bildet den aktuellen Stand eines Spielers über alle Räume hinweg ab,
 * ohne selbst persistent zu sein. Gespeichert werden unter anderem die
 * Raumstruktur, die Reihenfolge der Fragen, die bisher erzielten Ergebnisse und
 * Metadaten zur Aktivität der Sitzung.
 *
 * Die eigentliche Persistenz des Spielfortschritts liegt in der Datenbank. Diese
 * Klasse ist das zur Laufzeit genutzte Arbeitsmodell, das vom
 * {@code QuizSessionGenerator} vorbereitet und vom {@code QuizSessionManager}
 * verwaltet wird.
 *
 * Aufbau:
 * QuizSession
 * └── Map<roomId, RoomSession>
 *     └── questionSequence
 *     └── questionCache
 *     └── results
 *     └── currentIndex
 *     └── completed
 */
public class QuizSession
{

    /**
     * Kapselt das Ergebnis einer beantworteten Frage innerhalb einer Sitzung.
     *
     * Gespeichert werden nur die für die Laufzeit relevanten Auswertungsdaten,
     * nicht die komplette Frage selbst.
     */
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

    /**
     * Hält den Laufzeitstand eines einzelnen Raums.
     *
     * Ein Raum kennt seine feste Fragenreihenfolge, die bereits geladenen
     * Frageobjekte, die bisherigen Ergebnisse sowie den aktuellen Fortschritt
     * innerhalb dieser Sequenz.
     */
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

        /**
         * Verschiebt den Raum auf die nächste Frage.
         *
         * Gibt {@code true} zurück, wenn noch eine weitere Frage erreicht wurde.
         * Ist keine weitere Frage vorhanden, wird der Raum als abgeschlossen
         * markiert und {@code false} geliefert.
         */
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

        /**
         * Liefert die aktuell aktive Frage des Raums.
         *
         * Vor der Rückgabe werden Index- und Gesamtinformationen in das DTO
         * eingetragen, damit das Frontend Fortschritt und Position direkt
         * anzeigen kann.
         */
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

        /**
         * Berechnet den Bearbeitungsfortschritt des Raums in Prozent.
         *
         * Gerundet wird auf eine Nachkommastelle, damit der Wert im Frontend
         * stabil und gut lesbar dargestellt werden kann.
         */
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

        /**
         * Ermittelt die Medaille des Raums anhand des Anteils korrekter Fragen.
         *
         * Die Schwellenwerte bilden direkt die Fachlogik des Spiels ab.
         */
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

        /**
         * Prüft, ob für die angegebene Frage bereits ein Ergebnis vorliegt.
         */
        public boolean isAnswered(Long questionId)
        {
            return this.results.containsKey(questionId);
        }

        public boolean isCompleted()
        {
            return this.completed;
        }

        /**
         * Kennzeichnet, ob die aktuelle Position bereits auf der letzten Frage liegt.
         */
        public boolean isLastQuestion()
        {
            return this.currentIndex >= this.questionSequence.size() - 1;
        }

        /**
         * Trägt das Ergebnis einer neu beantworteten Frage ein.
         */
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
         *
         * Der Wert wird defensiv auf den gültigen Bereich begrenzt, damit
         * inkonsistente Wiederherstellungsdaten die Sitzung nicht beschädigen.
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

    /**
     * Erzeugt eine neue Laufzeitsitzung für einen Benutzer und einen Spielstand.
     *
     * Die eigentlichen Räume werden anschließend separat ergänzt.
     */
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

    /**
     * Liefert den aktuell aktiven Raum der Sitzung.
     */
    public RoomSession activeRoom()
    {
        return this.rooms.get(this.activeRoomId);
    }

    /**
     * Fügt einen Raum in die Sitzung ein und aktualisiert den Aktivitätszeitpunkt.
     */
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

    public RoomSession getRoom(int roomId)
    {
        return this.rooms.get(roomId);
    }

    /**
     * Ersetzt den Laufzeitstand eines vorhandenen Raums.
     *
     * Das ist vor allem beim Wiederherstellen oder Neuaufbau eines Raums nützlich.
     */
    public void replaceRoom(RoomSession roomSession)
    {
        this.rooms.put(roomSession.getRoomId(), roomSession);
        touch();
    }

    /**
     * Externer Einstiegspunkt, um die Sitzung als benutzt zu markieren.
     */
    public void touchSession()
    {
        touch();
    }

    public String getSessionId()
    {
        return this.sessionId;
    }

    /**
     * Summiert alle korrekt beantworteten Fragen über sämtliche Räume.
     */
    public int getTotalCorrect()
    {
        return this.rooms.values().stream().mapToInt(RoomSession::getCorrectCount).sum();
    }

    /**
     * Summiert alle bisher erreichten Punkte der Sitzung.
     */
    public int getTotalEarnedPoints()
    {
        return this.rooms.values().stream().mapToInt(RoomSession::getEarnedPoints).sum();
    }

    /**
     * Summiert die maximal möglichen Punkte aller Räume.
     */
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

    /**
     * Prüft, ob alle Räume bereits abgeschlossen wurden.
     */
    public boolean isFullyCompleted()
    {
        return this.rooms.values().stream().allMatch(RoomSession::isCompleted);
    }

    /**
     * Wechselt den aktiven Raum.
     *
     * Es werden nur Räume akzeptiert, die tatsächlich in der Sitzung vorhanden sind.
     */
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

    /**
     * Aktualisiert den letzten Aktivitätszeitpunkt der Sitzung.
     */
    private void touch()
    {
        this.lastActivityAt = LocalDateTime.now();
    }
}
