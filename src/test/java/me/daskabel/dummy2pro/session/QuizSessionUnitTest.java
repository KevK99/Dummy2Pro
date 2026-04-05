package me.daskabel.dummy2pro.session;

import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.model.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class QuizSessionUnitTest
{
    @Test
    void currentQuestion_returnsCachedQuestionWithIndexAndTotal()
    {
        QuestionDto questionDto = questionDto(101L, QuestionType.MC);

        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(101L, questionDto);

        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(101L),
                cache,
                5
        );

        QuestionDto current = room.currentQuestion();

        assertNotNull(current);
        assertEquals(101L, current.getQuestionId());
        assertEquals(0, current.getCurrentIndex());
        assertEquals(1, current.getTotalCount());
    }

    @Test
    void advance_afterLastQuestion_marksRoomCompleted()
    {
        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(101L, questionDto(101L, QuestionType.MC));
        cache.put(102L, questionDto(102L, QuestionType.MC));

        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(101L, 102L),
                cache,
                10
        );

        assertTrue(room.advance());
        assertEquals(1, room.getCurrentIndex());

        assertFalse(room.advance());
        assertTrue(room.isCompleted());
        assertNull(room.currentQuestion());
    }

    @Test
    void setCurrentIndex_clampsBelowZeroAndAboveMax()
    {
        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(101L, questionDto(101L, QuestionType.MC));
        cache.put(102L, questionDto(102L, QuestionType.MC));

        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(101L, 102L),
                cache,
                10
        );

        room.setCurrentIndex(-5);
        assertEquals(0, room.getCurrentIndex());

        room.setCurrentIndex(999);
        assertEquals(1, room.getCurrentIndex());
    }

    @Test
    void roomResults_updateCountsPointsAndMedal()
    {
        Map<Long, QuestionDto> cache = new HashMap<>();
        cache.put(1L, questionDto(1L, QuestionType.MC));
        cache.put(2L, questionDto(2L, QuestionType.MC));
        cache.put(3L, questionDto(3L, QuestionType.MC));
        cache.put(4L, questionDto(4L, QuestionType.MC));

        QuizSession.RoomSession room = new QuizSession.RoomSession(
                1,
                "Thema 1",
                List.of(1L, 2L, 3L, 4L),
                cache,
                20
        );

        room.recordResult(1L, true, 5);
        room.recordResult(2L, true, 5);
        room.recordResult(3L, true, 5);
        room.recordResult(4L, false, 0);

        assertEquals(4, room.getAnsweredCount());
        assertEquals(3, room.getCorrectCount());
        assertEquals(1, room.getWrongCount());
        assertEquals(15, room.getEarnedPoints());
        assertEquals(100.0, room.getCompletionPercent());
        assertEquals("SILVER", room.getMedal());
    }

    @Test
    void session_aggregatesTotalsAcrossRoomsAndSwitchesActiveRoom()
    {
        QuizSession session = new QuizSession(7L, 70L);

        Map<Long, QuestionDto> room1Cache = new HashMap<>();
        room1Cache.put(1L, questionDto(1L, QuestionType.MC));
        QuizSession.RoomSession room1 = new QuizSession.RoomSession(1, "Thema 1", List.of(1L), room1Cache, 5);
        room1.recordResult(1L, true, 5);
        room1.setCompleted(true);

        Map<Long, QuestionDto> room2Cache = new HashMap<>();
        room2Cache.put(2L, questionDto(2L, QuestionType.MC));
        QuizSession.RoomSession room2 = new QuizSession.RoomSession(2, "Thema 2", List.of(2L), room2Cache, 7);
        room2.recordResult(2L, false, 0);
        room2.setCompleted(true);

        session.addRoom(room1);
        session.addRoom(room2);

        assertEquals(5, session.getTotalEarnedPoints());
        assertEquals(12, session.getTotalMaxPoints());
        assertEquals(1, session.getTotalCorrect());
        assertEquals(1, session.getTotalWrong());
        assertTrue(session.isFullyCompleted());

        session.setActiveRoomId(2);
        assertEquals(2, session.getActiveRoomId());
        assertEquals(room2, session.activeRoom());
    }

    @Test
    void setActiveRoomId_forUnknownRoom_throwsException()
    {
        QuizSession session = new QuizSession(7L, 70L);

        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> session.setActiveRoomId(99)
        );

        assertTrue(ex.getMessage().contains("existiert nicht"));
    }

    private QuestionDto questionDto(Long id, QuestionType questionType)
    {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(id);
        dto.setQuestionType(questionType);
        return dto;
    }
}