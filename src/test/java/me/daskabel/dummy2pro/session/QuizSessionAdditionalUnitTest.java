package me.daskabel.dummy2pro.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.model.QuestionType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizSessionAdditionalUnitTest
{
    @Test
    void currentQuestion_shouldReturnNull_forEmptySequenceAndMissingCacheEntry()
    {
        QuizSession.RoomSession emptyRoom = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(),
                Map.of(),
                0
        );

        assertNull(emptyRoom.currentQuestion());

        QuizSession.RoomSession missingCacheRoom = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(101L),
                Map.of(),
                5
        );

        assertNull(missingCacheRoom.currentQuestion());
    }

    @Test
    void getMedal_shouldCoverNoneBronzeAndGold()
    {
        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(1L, questionDto(1L));
        cache.put(2L, questionDto(2L));
        cache.put(3L, questionDto(3L));
        cache.put(4L, questionDto(4L));

        QuizSession.RoomSession noneRoom = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(1L, 2L, 3L, 4L),
                cache,
                20
        );
        noneRoom.recordResult(1L, true, 5);

        QuizSession.RoomSession bronzeRoom = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(1L, 2L, 3L, 4L),
                cache,
                20
        );
        bronzeRoom.recordResult(1L, true, 5);
        bronzeRoom.recordResult(2L, true, 5);

        QuizSession.RoomSession goldRoom = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(1L, 2L, 3L, 4L),
                cache,
                20
        );
        goldRoom.recordResult(1L, true, 5);
        goldRoom.recordResult(2L, true, 5);
        goldRoom.recordResult(3L, true, 5);
        goldRoom.recordResult(4L, true, 5);

        assertEquals("NONE", noneRoom.getMedal());
        assertEquals("BRONZE", bronzeRoom.getMedal());
        assertEquals("GOLD", goldRoom.getMedal());
    }

    @Test
    void restoreResult_shouldKeepAnsweredAtTimestamp()
    {
        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(101L, questionDto(101L));

        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(101L),
                cache,
                5
        );

        LocalDateTime answeredAt = LocalDateTime.of(2026, 4, 6, 14, 0);

        room.restoreResult(101L, true, 5, answeredAt);

        assertEquals(answeredAt, room.getResults().get(101L).getAnsweredAt());
        assertEquals(5, room.getEarnedPoints());
        assertEquals(1, room.getCorrectCount());
    }

    @Test
    void setCurrentIndex_shouldStayZero_whenQuestionSequenceIsEmpty()
    {
        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(),
                Map.of(),
                0
        );

        room.setCurrentIndex(99);

        assertEquals(0, room.getCurrentIndex());
    }

    @Test
    void replaceRoom_shouldOverwriteExistingRoomAndTouchSession()
    {
        QuizSession session = new QuizSession(7L, 70L);

        Map<Long, QuestionDto> cache1 = new HashMap<>();
        cache1.put(1L, questionDto(1L));
        QuizSession.RoomSession oldRoom = new QuizSession.RoomSession(1, "Alt", List.of(1L), cache1, 5);
        session.addRoom(oldRoom);

        LocalDateTime oldLastActivity = session.getLastActivityAt();
        ReflectionTestUtils.setField(session, "lastActivityAt", oldLastActivity.minusHours(5));

        Map<Long, QuestionDto> cache2 = new HashMap<>();
        cache2.put(2L, questionDto(2L));
        QuizSession.RoomSession newRoom = new QuizSession.RoomSession(1, "Neu", List.of(2L), cache2, 7);

        session.replaceRoom(newRoom);

        assertEquals("Neu", session.getRoom(1).getThemeName());
        assertEquals(7, session.getRoom(1).getMaxPoints());
        assertTrue(session.getLastActivityAt().isAfter(oldLastActivity.minusHours(5)));
    }

    private QuestionDto questionDto(Long id)
    {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(id);
        dto.setQuestionType(QuestionType.MC);
        return dto;
    }
}