package me.daskabel.dummy2pro.dto;

import java.util.List;

import me.daskabel.dummy2pro.model.QuestionType;

/**
 * Alle DTOs rund um die Raum/Fragen-API.
 *
 * Werden als innere Klassen gesammelt, damit man nicht 10 kleine Dateien hat.
 * Aufbau: RoomDtos.QuestionDto, RoomDtos.AnswerRequest, ...
 */
public class RoomDtos
{

	// =====================================================================
	// OUTBOUND: Was der Server an den Client schickt
	// =====================================================================

	/**
	 * Eine Antwortoption für MC/TF — OHNE is_correct. Die ID wird gebraucht, damit
	 * der Client sie beim Antworten mitschicken kann.
	 */
	public static class AnswerOptionDto
	{
		private Long answerId;
		private String optionText;
		private Integer optionOrder;

		public AnswerOptionDto()
		{
		}

		public Long getAnswerId()
		{
			return this.answerId;
		}

		public Integer getOptionOrder()
		{
			return this.optionOrder;
		}

		public String getOptionText()
		{
			return this.optionText;
		}

		public void setAnswerId(Long answerId)
		{
			this.answerId = answerId;
		}

		public void setOptionOrder(Integer optionOrder)
		{
			this.optionOrder = optionOrder;
		}

		public void setOptionText(String optionText)
		{
			this.optionText = optionText;
		}
	}

	/**
	 * Der Client schickt seine Antwort(en) auf eine Frage.
	 *
	 * Für MC/TF: selectedAnswerIds mit einer (oder mehreren) answer_id(s) Für GAP:
	 * gapAnswers: Map von gap_id -> gap_option_id
	 */
	public static class AnswerRequest
	{
		private Long questionId;
		private List<Long> selectedAnswerIds; // für MC / TF
		private List<GapAnswerEntry> gapAnswers; // für GAP

		public AnswerRequest()
		{
		}

		public List<GapAnswerEntry> getGapAnswers()
		{
			return this.gapAnswers;
		}

		public Long getQuestionId()
		{
			return this.questionId;
		}

		public List<Long> getSelectedAnswerIds()
		{
			return this.selectedAnswerIds;
		}

		public void setGapAnswers(List<GapAnswerEntry> gapAnswers)
		{
			this.gapAnswers = gapAnswers;
		}

		public void setQuestionId(Long questionId)
		{
			this.questionId = questionId;
		}

		public void setSelectedAnswerIds(List<Long> selectedAnswerIds)
		{
			this.selectedAnswerIds = selectedAnswerIds;
		}

	}

	/**
	 * Was der Server nach dem Auswerten einer Antwort zurückschickt.
	 */
	public static class AnswerResultDto
	{
		private boolean correct;
		private List<Long> correctAnswerIds; // für MC/TF: welche wären richtig gewesen
		private List<GapResultEntry> gapResults; // für GAP: pro Lücke richtig/falsch + korrekte
													// Option
		private int pointsEarned;
		private String explanation; // optional: Erklärung warum richtig/falsch

		public AnswerResultDto()
		{
		}

		public List<Long> getCorrectAnswerIds()
		{
			return this.correctAnswerIds;
		}

		public String getExplanation()
		{
			return this.explanation;
		}

		public List<GapResultEntry> getGapResults()
		{
			return this.gapResults;
		}

		public int getPointsEarned()
		{
			return this.pointsEarned;
		}

		public boolean isCorrect()
		{
			return this.correct;
		}

		public void setCorrect(boolean correct)
		{
			this.correct = correct;
		}

		public void setCorrectAnswerIds(List<Long> correctAnswerIds)
		{
			this.correctAnswerIds = correctAnswerIds;
		}

		public void setExplanation(String explanation)
		{
			this.explanation = explanation;
		}

		public void setGapResults(List<GapResultEntry> gapResults)
		{
			this.gapResults = gapResults;
		}

		public void setPointsEarned(int pointsEarned)
		{
			this.pointsEarned = pointsEarned;
		}
	}

	/**
	 * Eine einzelne Lücken-Antwort: welche Option wurde für welche Lücke gewählt.
	 */
	public static class GapAnswerEntry
	{
		private Long gapId;
		private Long selectedGapOptionId;

		public GapAnswerEntry()
		{
		}

		public Long getGapId()
		{
			return this.gapId;
		}

		public Long getSelectedGapOptionId()
		{
			return this.selectedGapOptionId;
		}

		public void setGapId(Long gapId)
		{
			this.gapId = gapId;
		}

		public void setSelectedGapOptionId(Long selectedGapOptionId)
		{
			this.selectedGapOptionId = selectedGapOptionId;
		}
	}

	// =====================================================================
	// INBOUND: Was der Client an den Server schickt
	// =====================================================================

	/**
	 * Ein Gap-Feld (Lücke) für GAP-Fragen. Enthält den Text davor/danach und die
	 * Auswahloptionen (ebenfalls ohne is_correct).
	 */
	public static class GapFieldDto
	{
		private Long gapId;
		private Integer gapIndex;
		private String textBefore;
		private String textAfter;
		private List<GapOptionDto> gapOptions;

		public GapFieldDto()
		{
		}

		public Long getGapId()
		{
			return this.gapId;
		}

		public Integer getGapIndex()
		{
			return this.gapIndex;
		}

		public List<GapOptionDto> getGapOptions()
		{
			return this.gapOptions;
		}

		public String getTextAfter()
		{
			return this.textAfter;
		}

		public String getTextBefore()
		{
			return this.textBefore;
		}

		public void setGapId(Long gapId)
		{
			this.gapId = gapId;
		}

		public void setGapIndex(Integer gapIndex)
		{
			this.gapIndex = gapIndex;
		}

		public void setGapOptions(List<GapOptionDto> gapOptions)
		{
			this.gapOptions = gapOptions;
		}

		public void setTextAfter(String textAfter)
		{
			this.textAfter = textAfter;
		}

		public void setTextBefore(String textBefore)
		{
			this.textBefore = textBefore;
		}
	}

	/**
	 * Eine Option für eine Lücke — ohne is_correct.
	 */
	public static class GapOptionDto
	{
		private Long gapOptionId;
		private String optionText;
		private Integer optionOrder;

		public GapOptionDto()
		{
		}

		public Long getGapOptionId()
		{
			return this.gapOptionId;
		}

		public Integer getOptionOrder()
		{
			return this.optionOrder;
		}

		public String getOptionText()
		{
			return this.optionText;
		}

		public void setGapOptionId(Long gapOptionId)
		{
			this.gapOptionId = gapOptionId;
		}

		public void setOptionOrder(Integer optionOrder)
		{
			this.optionOrder = optionOrder;
		}

		public void setOptionText(String optionText)
		{
			this.optionText = optionText;
		}
	}

	// =====================================================================
	// OUTBOUND: Ergebnis einer Antwort
	// =====================================================================

	/**
	 * Ergebnis für eine einzelne Lücke nach dem Auswerten.
	 */
	public static class GapResultEntry
	{
		private Long gapId;
		private boolean correct;
		private Long correctGapOptionId;
		private String correctOptionText;

		public GapResultEntry()
		{
		}

		public Long getCorrectGapOptionId()
		{
			return this.correctGapOptionId;
		}

		public String getCorrectOptionText()
		{
			return this.correctOptionText;
		}

		public Long getGapId()
		{
			return this.gapId;
		}

		public boolean isCorrect()
		{
			return this.correct;
		}

		public void setCorrect(boolean correct)
		{
			this.correct = correct;
		}

		public void setCorrectGapOptionId(Long correctGapOptionId)
		{
			this.correctGapOptionId = correctGapOptionId;
		}

		public void setCorrectOptionText(String correctOptionText)
		{
			this.correctOptionText = correctOptionText;
		}

		public void setGapId(Long gapId)
		{
			this.gapId = gapId;
		}
	}

	/**
	 * Eine einzelne Frage mit allen Antwortoptionen. Die korrekten Antworten werden
	 * NICHT mitgeschickt (wäre ja cheaten). is_correct fehlt absichtlich.
	 */
	public static class QuestionDto
	{
		private Long questionId;
		private QuestionType questionType; // "MC", "TF", "GAP"
		private String startText;
		private String imageUrl;
		private String endText;
		private Boolean allowsMultiple;
		private Integer points;

		// Für MC und TF: die Antwortoptionen (ohne is_correct!)
		private List<AnswerOptionDto> answerOptions;

		// Für GAP: die Lücken mit ihren Auswahlmöglichkeiten
		private List<GapFieldDto> gapFields;

		// Position in der aktuellen Sequenz
		private int currentIndex; // 0-basiert
		private int totalCount;

		public QuestionDto()
		{
		}

		public Boolean getAllowsMultiple()
		{
			return this.allowsMultiple;
		}

		public List<AnswerOptionDto> getAnswerOptions()
		{
			return this.answerOptions;
		}

		public int getCurrentIndex()
		{
			return this.currentIndex;
		}

		public String getEndText()
		{
			return this.endText;
		}

		public List<GapFieldDto> getGapFields()
		{
			return this.gapFields;
		}

		public String getImageUrl()
		{
			return this.imageUrl;
		}

		public Integer getPoints()
		{
			return this.points;
		}

		// Getters & Setters
		public Long getQuestionId()
		{
			return this.questionId;
		}

		public QuestionType getQuestionType()
		{
			return this.questionType;
		}

		public String getStartText()
		{
			return this.startText;
		}

		public int getTotalCount()
		{
			return this.totalCount;
		}

		public void setAllowsMultiple(Boolean allowsMultiple)
		{
			this.allowsMultiple = allowsMultiple;
		}

		public void setAnswerOptions(List<AnswerOptionDto> answerOptions)
		{
			this.answerOptions = answerOptions;
		}

		public void setCurrentIndex(int currentIndex)
		{
			this.currentIndex = currentIndex;
		}

		public void setEndText(String endText)
		{
			this.endText = endText;
		}

		public void setGapFields(List<GapFieldDto> gapFields)
		{
			this.gapFields = gapFields;
		}

		public void setImageUrl(String imageUrl)
		{
			this.imageUrl = imageUrl;
		}

		public void setPoints(Integer points)
		{
			this.points = points;
		}

		public void setQuestionId(Long questionId)
		{
			this.questionId = questionId;
		}

		public void setQuestionType(QuestionType questionType)
		{
			this.questionType = questionType;
		}

		public void setStartText(String startText)
		{
			this.startText = startText;
		}

		public void setTotalCount(int totalCount)
		{
			this.totalCount = totalCount;
		}
	}

	// =====================================================================
	// OUTBOUND: Raum-Status
	// =====================================================================

	/**
	 * Antwort auf GET /api/room/{id}/start Enthält die gemischte Fragen-Sequenz
	 * (nur IDs + erste Frage vollständig) und den aktuellen Status.
	 */
	public static class RoomStartDto
	{
		private RoomStatusDto status;
		private QuestionDto firstQuestion; // erste Frage direkt dabei
		private List<Long> questionSequence; // alle question_ids in zufälliger Reihenfolge

		public RoomStartDto()
		{
		}

		public QuestionDto getFirstQuestion()
		{
			return this.firstQuestion;
		}

		public List<Long> getQuestionSequence()
		{
			return this.questionSequence;
		}

		public RoomStatusDto getStatus()
		{
			return this.status;
		}

		public void setFirstQuestion(QuestionDto firstQuestion)
		{
			this.firstQuestion = firstQuestion;
		}

		public void setQuestionSequence(List<Long> questionSequence)
		{
			this.questionSequence = questionSequence;
		}

		public void setStatus(RoomStatusDto status)
		{
			this.status = status;
		}
	}

	/**
	 * Der aktuelle Status eines Raums für einen User. Wird für die
	 * Dashboard-Anzeige und den Status-Panel im Raum genutzt.
	 */
	public static class RoomStatusDto
	{
		private int roomId; // 1..7 (= theme_id)
		private String themeName;
		private int totalQuestions;
		private int answeredQuestions;
		private int correctAnswers;
		private int wrongAnswers;
		private int openQuestions;
		private int totalPoints;
		private int earnedPoints;
		private double completionPercent; // 0..100
		private String medal; // "NONE", "BRONZE", "SILVER", "GOLD"

		public RoomStatusDto()
		{
		}

		public int getAnsweredQuestions()
		{
			return this.answeredQuestions;
		}

		public double getCompletionPercent()
		{
			return this.completionPercent;
		}

		public int getCorrectAnswers()
		{
			return this.correctAnswers;
		}

		public int getEarnedPoints()
		{
			return this.earnedPoints;
		}

		public String getMedal()
		{
			return this.medal;
		}

		public int getOpenQuestions()
		{
			return this.openQuestions;
		}

		public int getRoomId()
		{
			return this.roomId;
		}

		public String getThemeName()
		{
			return this.themeName;
		}

		public int getTotalPoints()
		{
			return this.totalPoints;
		}

		public int getTotalQuestions()
		{
			return this.totalQuestions;
		}

		public int getWrongAnswers()
		{
			return this.wrongAnswers;
		}

		public void setAnsweredQuestions(int answeredQuestions)
		{
			this.answeredQuestions = answeredQuestions;
		}

		public void setCompletionPercent(double completionPercent)
		{
			this.completionPercent = completionPercent;
		}

		public void setCorrectAnswers(int correctAnswers)
		{
			this.correctAnswers = correctAnswers;
		}

		public void setEarnedPoints(int earnedPoints)
		{
			this.earnedPoints = earnedPoints;
		}

		public void setMedal(String medal)
		{
			this.medal = medal;
		}

		public void setOpenQuestions(int openQuestions)
		{
			this.openQuestions = openQuestions;
		}

		public void setRoomId(int roomId)
		{
			this.roomId = roomId;
		}

		public void setThemeName(String themeName)
		{
			this.themeName = themeName;
		}

		public void setTotalPoints(int totalPoints)
		{
			this.totalPoints = totalPoints;
		}

		public void setTotalQuestions(int totalQuestions)
		{
			this.totalQuestions = totalQuestions;
		}

		public void setWrongAnswers(int wrongAnswers)
		{
			this.wrongAnswers = wrongAnswers;
		}
	}
}