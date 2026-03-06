package me.daskabel.dummy2pro.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerOptionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapFieldDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapOptionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapResultEntry;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.dto.RoomDtos.RoomStatusDto;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.McAnswer;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.model.UserQuestionProgress;
import me.daskabel.dummy2pro.model.UserQuestionProgressId;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.repository.UserQuestionProgressRepository;
import me.daskabel.dummy2pro.repository.UserRepository;

/**
 * Kernlogik für die Raum/Quiz-Mechanik.
 *
 * Zuständigkeiten: - Fragen eines Themas laden und mischen - Antworten
 * auswerten (MC, TF, GAP) - Fortschritt in der DB speichern (wenn User
 * eingeloggt) - Raum-Status berechnen (%, Medal)
 *
 * Raum-ID = Theme-ID (1..7). Räume existieren NICHT in der DB, sie sind nur ein
 * Konzept das Theme + Fragen zusammenbringt.
 *
 * Medal-Logik: - BRONZE: >= 50% der Fragen beantwortet - SILVER: >= 75% der
 * Fragen korrekt beantwortet - GOLD: 100% der Fragen korrekt beantwortet
 */
@Service
public class RoomService
{

	// Schwellwerte für Medaillen (Anteil der KORREKTEN Antworten an Gesamtfragen)
	private static final double BRONZE_THRESHOLD = 0.50;
	private static final double SILVER_THRESHOLD = 0.75;
	private static final double GOLD_THRESHOLD = 1.00;

	/**
	 * GAP auswerten. Jede Lücke wird einzeln bewertet. Die Gesamtfrage gilt als
	 * korrekt, wenn ALLE Lücken richtig ausgefüllt wurden.
	 */
	private static AnswerResultDto evaluateGap(Question question, AnswerRequest request)
	{
		// Gap-Options aus der DB: gap_id -> korrekte gap_option_id + Text
		Map<Long, GapOption> correctByGapId = new HashMap<>();
		if (question.getGapFields() != null)
		{
			for (GapField gf : question.getGapFields())
			{
				if (gf.getGapOptions() != null)
				{
					gf.getGapOptions().stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
								.findFirst().ifPresent(o -> correctByGapId.put(gf.getGapId(), o));
				}
			}
		}

		// Gegebene Antworten als Map: gap_id -> selectedGapOptionId
		Map<Long, Long> selectedByGapId = new HashMap<>();
		if (request.getGapAnswers() != null)
		{
			for (var entry : request.getGapAnswers())
			{
				selectedByGapId.put(entry.getGapId(), entry.getSelectedGapOptionId());
			}
		}

		// Jede Lücke auswerten
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
		result.setPointsEarned(allCorrect ? question.getPoints() : 0);
		return result;
	}

	/**
	 * MC / TF auswerten. Eine Antwort ist korrekt wenn ALLE korrekten Optionen
	 * ausgewählt wurden und KEINE falschen.
	 */
	private static AnswerResultDto evaluateMcTf(Question question, AnswerRequest request)
	{
		// Korrekte IDs aus der DB
		Set<Long> correctIds = question.getMcAnswers().stream()
					.filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).map(McAnswer::getAnswerId)
					.collect(Collectors.toSet());

		Set<Long> selectedIds = new HashSet<>(
					request.getSelectedAnswerIds() != null ? request.getSelectedAnswerIds()
								: Collections.emptyList());

		boolean correct = correctIds.equals(selectedIds);

		AnswerResultDto result = new AnswerResultDto();
		result.setCorrect(correct);
		result.setCorrectAnswerIds(new ArrayList<>(correctIds));
		result.setPointsEarned(correct ? question.getPoints() : 0);
		return result;
	}

	/**
	 * Eine Question-Entity in ein QuestionDto umwandeln (ohne is_correct!).
	 */
	private static QuestionDto toQuestionDto(Question q, int currentIndex, int total)
	{
		QuestionDto dto = new QuestionDto();
		dto.setQuestionId(q.getQuestionId());
		dto.setQuestionType(q.getQuestionType());
		dto.setStartText(q.getStartText());
		dto.setImageUrl(q.getImageUrl());
		dto.setEndText(q.getEndText());
		dto.setAllowsMultiple(q.getAllowsMultiple());
		dto.setPoints(q.getPoints());
		dto.setCurrentIndex(currentIndex);
		dto.setTotalCount(total);

		// MC / TF: Antwortoptionen ohne is_correct
		if (q.getMcAnswers() != null && !q.getMcAnswers().isEmpty())
		{
			List<AnswerOptionDto> options = q.getMcAnswers().stream()
						.sorted(Comparator.comparingInt(
									a -> a.getOptionOrder() != null ? a.getOptionOrder() : 0))
						.map(a ->
						{
							AnswerOptionDto aDto = new AnswerOptionDto();
							aDto.setAnswerId(a.getAnswerId());
							aDto.setOptionText(a.getOptionText());
							aDto.setOptionOrder(a.getOptionOrder());
							return aDto;
						}).collect(Collectors.toList());
			dto.setAnswerOptions(options);
		}

		// GAP: Lücken mit Optionen ohne is_correct
		if (q.getGapFields() != null && !q.getGapFields().isEmpty())
		{
			List<GapFieldDto> gapDtos = q.getGapFields().stream()
						.sorted(Comparator.comparingInt(
									gf -> gf.getGapIndex() != null ? gf.getGapIndex() : 0))
						.map(gf ->
						{
							GapFieldDto gfDto = new GapFieldDto();
							gfDto.setGapId(gf.getGapId());
							gfDto.setGapIndex(gf.getGapIndex());
							gfDto.setTextBefore(gf.getTextBefore());
							gfDto.setTextAfter(gf.getTextAfter());

							if (gf.getGapOptions() != null)
							{
								List<GapOptionDto> gopts = gf.getGapOptions().stream()
											.sorted(Comparator.comparingInt(
														o -> o.getOptionOrder() != null
																	? o.getOptionOrder()
																	: 0))
											.map(go ->
											{
												GapOptionDto goDto = new GapOptionDto();
												goDto.setGapOptionId(go.getGapOptionId());
												goDto.setOptionText(go.getOptionText());
												goDto.setOptionOrder(go.getOptionOrder());
												return goDto;
											}).collect(Collectors.toList());
								gfDto.setGapOptions(gopts);
							}
							return gfDto;
						}).collect(Collectors.toList());
			dto.setGapFields(gapDtos);
		}

		return dto;
	}

	private final QuestionRepository questionRepo;

	private final UserQuestionProgressRepository progressRepo;

	// =====================================================================
	// START: Raum starten / Fragen laden und mischen
	// =====================================================================

	private final UserRepository userRepo;

	private final ThemeRepository themeRepo;

	// =====================================================================
	// ANSWER: Antwort auswerten und optional Fortschritt speichern
	// =====================================================================

	public RoomService(QuestionRepository questionRepo, UserQuestionProgressRepository progressRepo,
				UserRepository userRepo, ThemeRepository themeRepo)
	{
		this.questionRepo = questionRepo;
		this.progressRepo = progressRepo;
		this.userRepo = userRepo;
		this.themeRepo = themeRepo;
	}

	// =====================================================================
	// STATUS: Raum-Status berechnen
	// =====================================================================

	/**
	 * Raum-Status aus DB-Einträgen berechnen.
	 */
	private RoomStatusDto buildStatus(int roomId, Theme theme, List<Question> allQuestions,
				Long userId)
	{
		int total = allQuestions.size();
		int totalPoints = allQuestions.stream()
					.mapToInt(q -> q.getPoints() != null ? q.getPoints() : 1).sum();

		int correct = 0;
		int wrong = 0;
		int earnedPoints = 0;

		if (userId != null)
		{
			List<UserQuestionProgress> progressList = this.progressRepo
						.findByUserIdAndThemeId(userId, (long) roomId);

			Map<Long, UserQuestionProgress> progressMap = progressList.stream()
						.collect(Collectors.toMap(p -> p.getQuestion().getQuestionId(), p -> p));

			for (Question q : allQuestions)
			{
				UserQuestionProgress p = progressMap.get(q.getQuestionId());
				if (p != null)
				{
					if ("CORRECT".equals(p.getStatus()))
					{
						correct++;
						earnedPoints += q.getPoints() != null ? q.getPoints() : 1;
					} else if ("WRONG".equals(p.getStatus()))
					{
						wrong++;
					}
				}
			}
		}

		int answered = correct + wrong;
		int open = total - answered;
		double pct = total > 0 ? (double) answered / total * 100.0 : 0.0;
		double correctRatio = total > 0 ? (double) correct / total : 0.0;

		String medal;
		if (correctRatio >= GOLD_THRESHOLD)
			medal = "GOLD";
		else if (correctRatio >= SILVER_THRESHOLD)
			medal = "SILVER";
		else if (correctRatio >= BRONZE_THRESHOLD)
			medal = "BRONZE";
		else
			medal = "NONE";

		RoomStatusDto s = new RoomStatusDto();
		s.setRoomId(roomId);
		s.setThemeName(theme.getName());
		s.setTotalQuestions(total);
		s.setAnsweredQuestions(answered);
		s.setCorrectAnswers(correct);
		s.setWrongAnswers(wrong);
		s.setOpenQuestions(open);
		s.setTotalPoints(totalPoints);
		s.setEarnedPoints(earnedPoints);
		s.setCompletionPercent(Math.round(pct * 10.0) / 10.0);
		s.setMedal(medal);
		return s;
	}

	// =====================================================================
	// Private Hilfsmethoden
	// =====================================================================

	/**
	 * Gibt eine bestimmte Frage aus der Sequenz zurück. Der Client schickt die
	 * question_id und den Index in der Sequenz.
	 */
	@Transactional(readOnly = true)
	public QuestionDto getQuestion(Long questionId, int indexInSequence, int totalInSequence)
	{
		Question q = this.questionRepo.findById(questionId).orElseThrow(
					() -> new NoSuchElementException("Frage " + questionId + " nicht gefunden."));
		return toQuestionDto(q, indexInSequence, totalInSequence);
	}

	/**
	 * Berechnet den aktuellen Status eines Raums für einen User. Für anonyme User
	 * werden alle Fragen als OPEN gezählt.
	 */
	@Transactional(readOnly = true)
	public RoomStatusDto getRoomStatus(int roomId, Long userId)
	{
		Theme theme = getTheme(roomId);
		List<Question> allQuestions = loadAllQuestionsForTheme(roomId);
		return buildStatus(roomId, theme, allQuestions, userId);
	}

	private Theme getTheme(int roomId)
	{
		if (roomId < 1 || roomId > 7)
		{
			throw new IllegalArgumentException("Raum-ID muss zwischen 1 und 7 liegen.");
		}
		return this.themeRepo.findById((long) roomId).orElseThrow(() -> new NoSuchElementException(
					"Theme für Raum " + roomId + " nicht gefunden."));
	}

	/**
	 * Lädt alle Fragen eines Themas — MC/TF-Antworten und GAP-Felder in einer
	 * einzigen DB-Runde (verhindert N+1 durch FETCH JOIN).
	 *
	 * Problem: zwei separate FETCH JOINs (Answers + Gaps) können nicht in einer
	 * Query kombiniert werden (MultipleBagFetchException). Lösung: erst Fragen
	 * laden, dann in zwei separaten Queries die Details ergänzen und in-memory
	 * zusammenführen.
	 */
	private List<Question> loadAllQuestionsForTheme(int roomId)
	{
		long themeId = roomId;

		// 1. Alle Fragen mit McAnswers (für MC + TF)
		List<Question> withAnswers = this.questionRepo.findByThemeIdWithAnswers(themeId);

		// 2. Alle Fragen mit GapFields+Options (für GAP)
		List<Question> withGaps = this.questionRepo.findByThemeIdWithGaps(themeId);

		// 3. In-memory mergen: GAP-Daten in die withAnswers-Liste einbauen
		Map<Long, Question> gapMap = withGaps.stream()
					.collect(Collectors.toMap(Question::getQuestionId, q -> q));

		for (Question q : withAnswers)
		{
			if (QuestionType.GAP.equals(q.getQuestionType()))
			{
				Question gapVersion = gapMap.get(q.getQuestionId());
				if (gapVersion != null)
				{
					q.setGapFields(gapVersion.getGapFields());
				}
			}
		}

		return withAnswers;
	}

	/**
	 * Fortschritt für eine Frage speichern oder aktualisieren. Wenn der User
	 * dieselbe Frage erneut beantwortet, wird der Eintrag überschrieben.
	 */
	private void saveProgress(Long userId, Question question, boolean correct,
				List<Long> selectedAnswerIds)
	{
		User user = this.userRepo.findById(userId).orElseThrow(
					() -> new NoSuchElementException("User " + userId + " nicht gefunden."));

		UserQuestionProgressId id = new UserQuestionProgressId(userId, question.getQuestionId());

		UserQuestionProgress progress = this.progressRepo.findById(id).orElseGet(() ->
		{
			UserQuestionProgress p = new UserQuestionProgress(user, question);
			return p;
		});

		progress.setStatus(correct ? "CORRECT" : "WRONG");
		progress.setAnsweredAt(LocalDateTime.now());

		// Erste gewählte Antwort speichern (für MC mit einer Antwort ausreichend)
		if (selectedAnswerIds != null && !selectedAnswerIds.isEmpty())
		{
			progress.setSelectedAnswerId(selectedAnswerIds.get(0));
		}

		this.progressRepo.save(progress);
	}

	/**
	 * Startet (oder setzt fort) einen Raum für einen User.
	 *
	 * - Lädt alle Fragen des Themas - Mischt die Reihenfolge zufällig (neuer
	 * Spielstand = neue Reihenfolge) - Berechnet den aktuellen Status - Gibt die
	 * erste Frage + die Sequenz (alle IDs) zurück
	 *
	 * @param roomId 1..7 (entspricht theme_id)
	 * @param userId null = anonymer Modus (kein Fortschritt gespeichert)
	 */
	@Transactional(readOnly = true)
	public RoomStartDto startRoom(int roomId, Long userId)
	{
		Theme theme = getTheme(roomId);

		// Alle Fragen des Themas laden (mit Antworten und Gaps vorgeladen)
		List<Question> questions = loadAllQuestionsForTheme(roomId);

		if (questions.isEmpty())
		{
			throw new IllegalStateException("Keine Fragen für Raum " + roomId + " gefunden.");
		}

		// Mischen — jeder Spielstart hat eine andere Reihenfolge
		List<Question> shuffled = new ArrayList<>(questions);
		Collections.shuffle(shuffled);

		// Fragen-Sequenz als ID-Liste für den Client
		List<Long> sequence = shuffled.stream().map(Question::getQuestionId)
					.collect(Collectors.toList());

		// Erste Frage vollständig als DTO
		QuestionDto firstQuestion = toQuestionDto(shuffled.get(0), 0, shuffled.size());

		// Status berechnen (für anonyme User: alles offen)
		RoomStatusDto status = buildStatus(roomId, theme, questions, userId);

		RoomStartDto result = new RoomStartDto();
		result.setStatus(status);
		result.setFirstQuestion(firstQuestion);
		result.setQuestionSequence(sequence);
		return result;
	}

	/**
	 * Wertet eine Antwort aus und speichert den Fortschritt (falls User
	 * eingeloggt).
	 *
	 * @param roomId  1..7
	 * @param request Antwort des Users
	 * @return Ergebnis: richtig/falsch + korrekte Antworten
	 */
	@Transactional
	public AnswerResultDto submitAnswer(int roomId, AnswerRequest request)
	{
		Question question = this.questionRepo.findById(request.getQuestionId())
					.orElseThrow(() -> new NoSuchElementException(
								"Frage " + request.getQuestionId() + " nicht gefunden."));

		// Antwort auswerten je nach Fragetyp
		AnswerResultDto result = switch (question.getQuestionType()) {
		case MC, TF -> evaluateMcTf(question, request);
		case GAP -> evaluateGap(question, request);
		default -> throw new IllegalArgumentException(
					"Unbekannter Fragetyp: " + question.getQuestionType());
		};

		// Fortschritt nur speichern wenn User eingeloggt
		if (request.getUserId() != null)
		{
			saveProgress(request.getUserId(), question, result.isCorrect(),
						request.getSelectedAnswerIds());
		}

		return result;
	}
}