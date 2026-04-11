package me.daskabel.dummy2pro.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enthält die Review-Daten eines kompletten Spielstands.
 *
 * Die Struktur bildet den Spielstand hierarchisch ab:
 * Spielstand -> Räume -> Fragen -> Antworten bzw. Lücken.
 */
public class RunReviewDto
{
    private Long runId;
    private String username;
    private List<RoomReviewDto> rooms;

    public Long getRunId()
    {
        return this.runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public String getUsername()
    {
        return this.username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public List<RoomReviewDto> getRooms()
    {
        return this.rooms;
    }

    public void setRooms(List<RoomReviewDto> rooms)
    {
        this.rooms = rooms;
    }

    /**
     * Review-Daten eines einzelnen Raums.
     */
    public static class RoomReviewDto
    {
        private int roomId;
        private String themeName;
        private String medal;
        private int totalQuestions;
        private int correctAnswers;
        private int wrongAnswers;
        private int openQuestions;
        private List<QuestionReviewDto> questions;

        public int getRoomId()
        {
            return this.roomId;
        }

        public void setRoomId(int roomId)
        {
            this.roomId = roomId;
        }

        public String getThemeName()
        {
            return this.themeName;
        }

        public void setThemeName(String themeName)
        {
            this.themeName = themeName;
        }

        public String getMedal()
        {
            return this.medal;
        }

        public void setMedal(String medal)
        {
            this.medal = medal;
        }

        public int getTotalQuestions()
        {
            return this.totalQuestions;
        }

        public void setTotalQuestions(int totalQuestions)
        {
            this.totalQuestions = totalQuestions;
        }

        public int getCorrectAnswers()
        {
            return this.correctAnswers;
        }

        public void setCorrectAnswers(int correctAnswers)
        {
            this.correctAnswers = correctAnswers;
        }

        public int getWrongAnswers()
        {
            return this.wrongAnswers;
        }

        public void setWrongAnswers(int wrongAnswers)
        {
            this.wrongAnswers = wrongAnswers;
        }

        public int getOpenQuestions()
        {
            return this.openQuestions;
        }

        public void setOpenQuestions(int openQuestions)
        {
            this.openQuestions = openQuestions;
        }

        public List<QuestionReviewDto> getQuestions()
        {
            return this.questions;
        }

        public void setQuestions(List<QuestionReviewDto> questions)
        {
            this.questions = questions;
        }
    }

    /**
     * Review-Daten einer einzelnen Frage.
     */
    public static class QuestionReviewDto
    {
        private Long questionId;
        private int questionOrder;
        private String questionType;
        private String questionText;
        private String imageUrl;
        private int points;
        private String status;
        private LocalDateTime answeredAt;
        private List<ChoiceReviewDto> choices;
        private List<GapReviewDto> gaps;

        public Long getQuestionId()
        {
            return this.questionId;
        }

        public void setQuestionId(Long questionId)
        {
            this.questionId = questionId;
        }

        public int getQuestionOrder()
        {
            return this.questionOrder;
        }

        public void setQuestionOrder(int questionOrder)
        {
            this.questionOrder = questionOrder;
        }

        public String getQuestionType()
        {
            return this.questionType;
        }

        public void setQuestionType(String questionType)
        {
            this.questionType = questionType;
        }

        public String getQuestionText()
        {
            return this.questionText;
        }

        public void setQuestionText(String questionText)
        {
            this.questionText = questionText;
        }

        public String getImageUrl()
        {
            return this.imageUrl;
        }

        public void setImageUrl(String imageUrl)
        {
            this.imageUrl = imageUrl;
        }

        public int getPoints()
        {
            return this.points;
        }

        public void setPoints(int points)
        {
            this.points = points;
        }

        public String getStatus()
        {
            return this.status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }

        public LocalDateTime getAnsweredAt()
        {
            return this.answeredAt;
        }

        public void setAnsweredAt(LocalDateTime answeredAt)
        {
            this.answeredAt = answeredAt;
        }

        public List<ChoiceReviewDto> getChoices()
        {
            return this.choices;
        }

        public void setChoices(List<ChoiceReviewDto> choices)
        {
            this.choices = choices;
        }

        public List<GapReviewDto> getGaps()
        {
            return this.gaps;
        }

        public void setGaps(List<GapReviewDto> gaps)
        {
            this.gaps = gaps;
        }
    }

    /**
     * Review-Daten einer Antwortoption bei MC- oder TF-Fragen.
     */
    public static class ChoiceReviewDto
    {
        private Long answerId;
        private String optionText;
        private boolean selected;
        private boolean correct;

        public Long getAnswerId()
        {
            return this.answerId;
        }

        public void setAnswerId(Long answerId)
        {
            this.answerId = answerId;
        }

        public String getOptionText()
        {
            return this.optionText;
        }

        public void setOptionText(String optionText)
        {
            this.optionText = optionText;
        }

        public boolean isSelected()
        {
            return this.selected;
        }

        public void setSelected(boolean selected)
        {
            this.selected = selected;
        }

        public boolean isCorrect()
        {
            return this.correct;
        }

        public void setCorrect(boolean correct)
        {
            this.correct = correct;
        }
    }

    /**
     * Review-Daten einer einzelnen Lücke bei GAP-Fragen.
     */
    public static class GapReviewDto
    {
        private Long gapId;
        private int gapIndex;
        private String label;
        private String selectedText;
        private String correctText;
        private boolean correct;

        public Long getGapId()
        {
            return this.gapId;
        }

        public void setGapId(Long gapId)
        {
            this.gapId = gapId;
        }

        public int getGapIndex()
        {
            return this.gapIndex;
        }

        public void setGapIndex(int gapIndex)
        {
            this.gapIndex = gapIndex;
        }

        public String getLabel()
        {
            return this.label;
        }

        public void setLabel(String label)
        {
            this.label = label;
        }

        public String getSelectedText()
        {
            return this.selectedText;
        }

        public void setSelectedText(String selectedText)
        {
            this.selectedText = selectedText;
        }

        public String getCorrectText()
        {
            return this.correctText;
        }

        public void setCorrectText(String correctText)
        {
            this.correctText = correctText;
        }

        public boolean isCorrect()
        {
            return this.correct;
        }

        public void setCorrect(boolean correct)
        {
            this.correct = correct;
        }
    }
}