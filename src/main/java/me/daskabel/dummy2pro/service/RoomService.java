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
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.GapField;
import me.daskabel.dummy2pro.model.GapOption;
import me.daskabel.dummy2pro.model.ProgressStatus;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.RunGapAnswer;
import me.daskabel.dummy2pro.model.RunSelectedAnswer;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.RunGapAnswerRepository;
import me.daskabel.dummy2pro.repository.RunSelectedAnswerRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;

/**
 * Kernlogik für die Raum/Quiz-Mechanik.
 *
 * Zuständigkeiten:
 * - Fragen des Raums dieser Sitzung laden
 * - Antworten auswerten (MC, TF, GAP)
 * - Fortschritt in der DB speichern
 * - Raum-Status berechnen (%, Medal)
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

    private final QuestionRepository questionRepo;
    private final ThemeRepository themeRepo;
    private final GameRunRepository gameRunRepo;
    private final QuestionProgressRepository questionProgressRepo;
    private final RunSelectedAnswerRepository runSelectedAnswerRepo;
    private final RunGapAnswerRepository runGapAnswerRepo;

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
                    gf.getGapOptions().stream()
                            .filter(GapOption::getIsCorrect)
                            .findFirst()
                            .ifPresent(o -> correctByGapId.put(gf.getGapId(), o));
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

			if (!gapCorrect) {
                allCorrect = false;
            }

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
		Set<Long> correctIds = question.getAnswerOptions().stream()
					.filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).map(AnswerOption::getAnswerId)
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
		if (q.getAnswerOptions() != null && !q.getAnswerOptions().isEmpty())
		{
			List<AnswerOptionDto> options = q.getAnswerOptions().stream()
                    .sorted(Comparator.comparingInt(AnswerOption::getOptionOrder))
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
                        .sorted(Comparator.comparingInt(GapField::getGapIndex))
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
                                            .sorted(Comparator.comparingInt(GapOption::getOptionOrder))
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

	// =====================================================================
	// ANSWER: Antwort auswerten und optional Fortschritt speichern
	// =====================================================================

	public RoomService(QuestionRepository questionRepo, ThemeRepository themeRepo,
                       GameRunRepository gameRunRepo,
                       QuestionProgressRepository questionProgressRepo,
                       RunSelectedAnswerRepository runSelectedAnswerRepo,
                       RunGapAnswerRepository runGapAnswerRepo)
	{
		this.questionRepo = questionRepo;
		this.themeRepo = themeRepo;
        this.gameRunRepo = gameRunRepo;
        this.questionProgressRepo = questionProgressRepo;
        this.runSelectedAnswerRepo = runSelectedAnswerRepo;
        this.runGapAnswerRepo = runGapAnswerRepo;
	}

	// =====================================================================
	// STATUS: Raum-Status berechnen
	// =====================================================================

	/**
	 * Raum-Status aus DB-Einträgen berechnen.
	 */
	private RoomStatusDto buildStatus(int roomId, Theme theme, List<Question> allQuestions,
				Long runId)
	{
		int total = allQuestions.size();
		int totalPoints = allQuestions.stream()
                .mapToInt(Question::getPoints)
                .sum();

		int correct = 0;
		int wrong = 0;
		int earnedPoints = 0;


			List<QuestionProgress> progressList = this.questionProgressRepo
						.findByRunIdAndRoomIdOrderByQuestionOrder(runId, roomId);

			Map<Long, QuestionProgress> progressMap = progressList.stream()
						.collect(Collectors.toMap(p -> p.getQuestion().getQuestionId(), p -> p));

			for (Question q : allQuestions)
			{
                QuestionProgress p = progressMap.get(q.getQuestionId());
				if (p != null)
				{
					if (p.getStatus() == ProgressStatus.CORRECT)
					{
						correct++;
                        earnedPoints += q.getPoints();
					} else if (p.getStatus() == ProgressStatus.WRONG)
					{
						wrong++;
					}
				}
			}


		int answered = correct + wrong;
		int open = total - answered;
		double pct = total > 0 ? (double) answered / total * 100.0 : 0.0;
		double correctRatio = total > 0 ? (double) correct / total : 0.0;

        /* Medal-Logik nur für den aktuellen Raum */
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
     * Berechnet den aktuellen Status eines Raums für einen bestimmten Run, also einen bestimmten Spielstand eines Users.
     */
	@Transactional(readOnly = true)
	public RoomStatusDto getRoomStatus(int roomId, Long runId)
	{
        getRun(runId);
		Theme theme = getTheme(roomId);
        List<QuestionProgress> progressList = this.questionProgressRepo
                .findByRunIdAndRoomIdOrderByQuestionOrder(runId, roomId);

        List<Question> roomQuestions = progressList.stream()
                .map(QuestionProgress::getQuestion)
                .toList();
		return buildStatus(roomId, theme, roomQuestions, runId);
	}

    private GameRun getRun(Long runId)
    {
        if (runId == null)
        {
            throw new IllegalArgumentException("runId darf nicht null sein.");
        }

        return this.gameRunRepo.findById(runId)
                .orElseThrow(() -> new NoSuchElementException("Run " + runId + " nicht gefunden."));
    }

	private Theme getTheme(int roomId)
	{
        List<Theme> themes = themeRepo.findAllByOrderByThemeIdAsc();
		if (roomId < 1 || roomId >  themes.size())
		{
			throw new IllegalArgumentException("Raum-ID ist ungültig.");
		}
		return themes.get(roomId - 1);
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
        List<Theme> themes = themeRepo.findAllByOrderByThemeIdAsc();
		long themeId = themes.get(roomId - 1).getThemeId();

		// 1. Alle Fragen mit Antwortoptionen (für MC + TF)
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
	private void saveProgress(Long runId, Question question, AnswerResultDto result,
                              AnswerRequest request)
	{
        GameRun run = getRun(runId);

        QuestionProgress  progress = this.questionProgressRepo
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

                    RunGapAnswer runGapAnswer = new RunGapAnswer(
                            run,
                            question,
                            gapField,
                            selectedOption,
                            LocalDateTime.now()
                    );

                    this.runGapAnswerRepo.save(runGapAnswer);
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

                RunSelectedAnswer selectedAnswer = new RunSelectedAnswer(run, question, answerOption);
                this.runSelectedAnswerRepo.save(selectedAnswer);
            }
        }
    }

	/**
	 * Startet (oder setzt fort) einen Raum für einen bestimmten Run (Spielstand).
	 *
	 * - Lädt alle Fragen des Themas
     * - Mischt die Reihenfolge zufällig (neuer Spielstand = neue Reihenfolge)
     * - Berechnet den aktuellen Status
     * - Gibt die erste Frage + die Sequenz (alle IDs) zurück
	 *
	 * @param roomId 1..7 (entspricht theme_id)
	 * @param runId aktueller Spielstand
	 */
	@Transactional(readOnly = true)
	public RoomStartDto startRoom(int roomId, Long runId)
	{
        getRun(runId);
		Theme theme = getTheme(roomId);

        List<QuestionProgress> progressList = this.questionProgressRepo
                .findByRunIdAndRoomIdOrderByQuestionOrder(runId, roomId);

        List<Question> orderedQuestions = progressList.stream()
                .map(QuestionProgress::getQuestion)
                .toList();

        if (orderedQuestions.isEmpty())
        {
            throw new IllegalStateException("Keine Fragen für Raum " + roomId + " im Run " + runId + " gefunden.");
        }

        List<Long> sequence = orderedQuestions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        QuestionDto firstQuestion = toQuestionDto(orderedQuestions.get(0), 0, orderedQuestions.size());

		// Status berechnen
		RoomStatusDto status = buildStatus(roomId, theme, orderedQuestions, runId);

		RoomStartDto result = new RoomStartDto();
		result.setStatus(status);
		result.setFirstQuestion(firstQuestion);
		result.setQuestionSequence(sequence);
		return result;
	}

	/**
	 * Wertet eine Antwort aus und speichert den Fortschritt im aktuellen Run.
	 *
	 * @param roomId  1..7
     * @param runId aktueller Spielstand
	 * @param request Antwort des Users
	 * @return Ergebnis: richtig/falsch + korrekte Antworten
	 */
	@Transactional
	public AnswerResultDto submitAnswer(int roomId, Long runId, AnswerRequest request)
	{
        getRun(runId);
		Question question = this.questionRepo.findById(request.getQuestionId())
					.orElseThrow(() -> new NoSuchElementException(
								"Frage " + request.getQuestionId() + " nicht gefunden."));

        validateQuestionBelongsToRoom(question, roomId);
		// Antwort auswerten je nach Fragetyp
		AnswerResultDto result = switch (question.getQuestionType()) {
		case MC, TF -> evaluateMcTf(question, request);
		case GAP -> evaluateGap(question, request);
		default -> throw new IllegalArgumentException(
					"Unbekannter Fragetyp: " + question.getQuestionType());
		};

		// Fortschritt speichern, User ist eingeloggt
		saveProgress(runId, question, result, request);

		return result;
	}

    private void validateQuestionBelongsToRoom(Question question, int roomId)
    {
        Theme theme = getTheme(roomId);
        boolean belongs = question.getThemes() != null
                && question.getThemes().stream()
                .anyMatch(t -> t.getThemeId().equals(theme.getThemeId()));

        if (!belongs)
        {
            throw new IllegalArgumentException(
                    "Frage " + question.getQuestionId() + " gehört nicht zu Raum " + roomId + ".");
        }
    }
}