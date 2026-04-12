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

import me.daskabel.dummy2pro.dto.AnswerComparisonDto;
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
 * Kernlogik für Räume, Fragen, Antwortauswertung und Fortschrittsspeicherung.
 *
 * Der Service verbindet die fachliche Raumlogik mit der Persistenz:
 * Fragen werden themenbezogen geladen, Antworten je nach Fragetyp ausgewertet
 * und der Bearbeitungsstand inklusive ausgewählter Antworten in der Datenbank
 * abgelegt.
 *
 * Wichtig ist hier vor allem die Unterscheidung zwischen:
 * - Raumstruktur über die Reihenfolge der Themes
 * - Fragen und deren Detaildaten
 * - Fortschritt eines konkreten Spielstands
 */
@Service
public class RoomService
{
    // Schwellwerte für Medaillen (Anteil der KORREKTEN Antworten an Gesamtfragen)
    private static final double BRONZE_THRESHOLD = 0.50;
    private static final double SILVER_THRESHOLD = 0.75;
    private static final double GOLD_THRESHOLD = 1.00;

    /**
     * GAP auswerten. Jede Lücke wird einzeln bewertet. Die Gesamtfrage gilt als korrekt, wenn ALLE Lücken richtig
     * ausgefüllt wurden.
     */
    public static AnswerResultDto evaluateGap(Question question, AnswerRequest request)
    {
        // Gap-Options aus der DB: gap_id -> korrekte gap_option_id + Text
        Map<Long, GapOption> correctByGapId = new HashMap<>();
        if (question.getGapFields() != null)
        {
            for (GapField gf : question.getGapFields())
            {
                if (gf.getGapOptions() != null)
                {
                    gf.getGapOptions()
                            .stream()
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

        List<GapResultEntry> gapResults = new ArrayList<>();
        boolean allCorrect = true;

        for (Map.Entry<Long, GapOption> entry : correctByGapId.entrySet())
        {
            Long gapId = entry.getKey();
            GapOption correctOption = entry.getValue();
            Long selected = selectedByGapId.get(gapId);
            boolean gapCorrect = correctOption.getGapOptionId().equals(selected);

            if (!gapCorrect)
            {
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
     * MC / TF auswerten.
     *
     * Eine Antwort gilt als korrekt, sobald mindestens eine richtige Antwort
     * ausgewählt wurde. Es müssen also nicht alle richtigen Antworten gewählt
     * werden. Zusätzliche falsche Antworten ändern daran nichts.
     */
    public static AnswerResultDto evaluateMcTf(Question question, AnswerRequest request)
    {
        Set<Long> correctIds = question.getAnswerOptions()
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                .map(AnswerOption::getAnswerId)
                .collect(Collectors.toSet());

        Set<Long> selectedIds = new HashSet<>(
                request.getSelectedAnswerIds() != null ? request.getSelectedAnswerIds() : Collections.emptyList());

        boolean correct = !correctIds.isEmpty()
                && selectedIds.stream().anyMatch(correctIds::contains);

        AnswerResultDto result = new AnswerResultDto();
        result.setCorrect(correct);
        result.setCorrectAnswerIds(new ArrayList<>(correctIds));
        result.setPointsEarned(correct ? question.getPoints() : 0);
        return result;
    }

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

        // Korrektheitsinformationen bleiben bewusst draußen, damit das Frontend
        // die Lösung nicht schon vor der Auswertung kennt.
        if (q.getAnswerOptions() != null && !q.getAnswerOptions().isEmpty())
        {
            List<AnswerOptionDto> options = q.getAnswerOptions()
                    .stream()
                    .sorted(Comparator.comparingInt(AnswerOption::getOptionOrder))
                    .map(a -> {
                        AnswerOptionDto aDto = new AnswerOptionDto();
                        aDto.setAnswerId(a.getAnswerId());
                        aDto.setOptionText(a.getOptionText());
                        aDto.setOptionOrder(a.getOptionOrder());
                        return aDto;
                    })
                    .collect(Collectors.toList());
            dto.setAnswerOptions(options);
        }

        if (q.getGapFields() != null && !q.getGapFields().isEmpty())
        {
            List<GapFieldDto> gapDtos = q.getGapFields()
                    .stream()
                    .sorted(Comparator.comparingInt(GapField::getGapIndex))
                    .map(gf -> {
                        GapFieldDto gfDto = new GapFieldDto();
                        gfDto.setGapId(gf.getGapId());
                        gfDto.setGapIndex(gf.getGapIndex());
                        gfDto.setTextBefore(gf.getTextBefore());
                        gfDto.setTextAfter(gf.getTextAfter());

                        if (gf.getGapOptions() != null)
                        {
                            List<GapOptionDto> gopts = gf.getGapOptions()
                                    .stream()
                                    .sorted(Comparator.comparingInt(GapOption::getOptionOrder))
                                    .map(go -> {
                                        GapOptionDto goDto = new GapOptionDto();
                                        goDto.setGapOptionId(go.getGapOptionId());
                                        goDto.setOptionText(go.getOptionText());
                                        goDto.setOptionOrder(go.getOptionOrder());
                                        return goDto;
                                    })
                                    .collect(Collectors.toList());
                            gfDto.setGapOptions(gopts);
                        }
                        return gfDto;
                    })
                    .collect(Collectors.toList());
            dto.setGapFields(gapDtos);
        }

        return dto;
    }

    private final QuestionRepository questionRepo;
    private final ThemeRepository themeRepo;
    private final GameRunRepository gameRunRepo;
    private final QuestionProgressRepository questionProgressRepo;
    private final RunSelectedAnswerRepository runSelectedAnswerRepo;
    private final RunGapAnswerRepository runGapAnswerRepo;

    public RoomService(
            QuestionRepository questionRepo,
            ThemeRepository themeRepo,
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

    /**
     * Baut den fachlichen Status eines Raums aus Fragen und gespeichertem
     * Fortschritt zusammen.
     *
     * Grundlage ist nicht nur die Fragenmenge des Themas, sondern der
     * konkrete Fortschritt des aktuellen Runs. Dadurch lassen sich Punkte,
     * Medaille, offene Fragen und Antwortvergleiche konsistent aus einem
     * gemeinsamen Datenstand ableiten.
     */
    private RoomStatusDto buildStatus(int roomId, Theme theme, List<Question> allQuestions, Long runId)
    {
        int total = allQuestions.size();
        int totalPoints = allQuestions.stream().mapToInt(Question::getPoints).sum();

        int correct = 0;
        int wrong = 0;
        int earnedPoints = 0;

        List<QuestionProgress> progressList =
                this.questionProgressRepo.findByRunIdAndRoomIdOrderByQuestionOrder(runId, roomId);

        // Für die Statusberechnung wird der Fortschritt nach questionId
        // indiziert, damit Fragenbestand und gespeicherter Run-Stand später
        // ohne weitere Repository-Zugriffe zusammengeführt werden können.
        Map<Long, QuestionProgress> progressMap =
                progressList.stream().collect(Collectors.toMap(p -> p.getQuestion().getQuestionId(), p -> p));

        for (Question q : allQuestions)
        {
            QuestionProgress p = progressMap.get(q.getQuestionId());
            if (p != null)
            {
                if (p.getStatus() == ProgressStatus.CORRECT)
                {
                    correct++;
                    earnedPoints += q.getPoints();
                }
                else if (p.getStatus() == ProgressStatus.WRONG)
                {
                    wrong++;
                }
            }
        }

        int answered = correct + wrong;
        int open = total - answered;
        double pct = total > 0 ? (double) answered / total * 100.0 : 0.0;
        double correctRatio = total > 0 ? (double) correct / total : 0.0;

        String medal;
        if (correctRatio >= GOLD_THRESHOLD)
        {
            medal = "GOLD";
        }
        else if (correctRatio >= SILVER_THRESHOLD)
        {
            medal = "SILVER";
        }
        else if (correctRatio >= BRONZE_THRESHOLD)
        {
            medal = "BRONZE";
        }
        else
        {
            medal = "NONE";
        }

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

        List<AnswerComparisonDto> comparisons = new ArrayList<>();

        for (Question q : allQuestions)
        {
            QuestionProgress p = progressMap.get(q.getQuestionId());

            if (p != null)
            {
                AnswerComparisonDto comparison = new AnswerComparisonDto();
                comparison.setQuestionId(q.getQuestionId());
                comparison.setSelectedAnswerIds(new ArrayList<>());

                if (q.getQuestionType() == QuestionType.GAP)
                {
                    // Für GAP-Fragen gibt es hier aktuell noch keine Detailrückgabe
                    // der konkret ausgewählten Optionen.
                }
                else
                {
                    List<Long> selectedIds =
                            runSelectedAnswerRepo.findByRun_RunIdAndQuestion_QuestionId(runId, q.getQuestionId())
                                    .stream()
                                    .map(a -> a.getAnswerOption().getAnswerId())
                                    .collect(Collectors.toList());
                    comparison.setSelectedAnswerIds(selectedIds);
                }

                List<Long> correctAnswers = q.getAnswerOptions()
                        .stream()
                        .filter(AnswerOption::getIsCorrect)
                        .map(AnswerOption::getAnswerId)
                        .collect(Collectors.toList());
                comparison.setCorrectAnswerIds(correctAnswers);

                comparisons.add(comparison);
            }
        }

        s.setAnswerComparisons(comparisons);

        return s;
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestion(Long questionId, int indexInSequence, int totalInSequence)
    {
        Question q = this.questionRepo.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Frage " + questionId + " nicht gefunden."));
        return toQuestionDto(q, indexInSequence, totalInSequence);
    }

    @Transactional(readOnly = true)
    public RoomStatusDto getRoomStatus(int roomId, Long runId)
    {
        getRun(runId);
        Theme theme = getTheme(roomId);
        List<QuestionProgress> progressList =
                this.questionProgressRepo.findByRunIdAndRoomIdOrderByQuestionOrder(runId, roomId);

        List<Question> roomQuestions = progressList.stream().map(QuestionProgress::getQuestion).toList();
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
        if (roomId < 1 || roomId > themes.size())
        {
            throw new IllegalArgumentException("Raum-ID ist ungültig.");
        }
        return themes.get(roomId - 1);
    }

    /**
     * Lädt alle Fragen eines Themas mit ihren Detaildaten.
     *
     * Hintergrund: Antworten und GAP-Strukturen können wegen der JPA/Hibernate-
     * Mehrfach-Bag-Problematik nicht sauber in einer einzigen FETCH-JOIN-Abfrage
     * kombiniert werden. Deshalb werden beide Sichten getrennt geladen und
     * anschließend anhand der questionId zusammengeführt.
     */
    private List<Question> loadAllQuestionsForTheme(int roomId)
    {
        Theme theme = getTheme(roomId);

        List<Long> questionIds = this.questionRepo.findQuestionIdsByThemeId(theme.getThemeId());

        if (questionIds.isEmpty())
        {
            return List.of();
        }

        List<Question> withAnswers = this.questionRepo.findByQuestionIdsWithAnswers(questionIds);
        List<Question> withGaps = this.questionRepo.findByQuestionIdsWithGaps(questionIds);

        Map<Long, Question> mergedMap = new HashMap<>();

        for (Question question : withAnswers)
        {
            mergedMap.put(question.getQuestionId(), question);
        }

        for (Question gapQuestion : withGaps)
        {
            Question existing = mergedMap.get(gapQuestion.getQuestionId());

            if (existing == null)
            {
                mergedMap.put(gapQuestion.getQuestionId(), gapQuestion);
                continue;
            }

            if (gapQuestion.getGapFields() != null && !gapQuestion.getGapFields().isEmpty())
            {
                existing.setGapFields(gapQuestion.getGapFields());
            }
        }

        List<Question> orderedQuestions = new ArrayList<>();

        // Die Reihenfolge wird nicht dem Repository überlassen, sondern explizit
        // über die zuvor geladene ID-Sequenz stabilisiert.
        for (Long questionId : questionIds)
        {
            Question question = mergedMap.get(questionId);

            if (question != null)
            {
                orderedQuestions.add(question);
            }
        }

        return orderedQuestions;
    }

    /**
     * Fortschritt für eine Frage speichern oder aktualisieren. Wenn der User dieselbe Frage erneut beantwortet, wird
     * der Eintrag überschrieben.
     */
    /**
     * Persistiert das Ergebnis einer beantworteten Frage samt konkreter
     * Auswahl.
     *
     * Vorhandene Detailantworten werden dabei pro Frage vollständig ersetzt,
     * damit bei erneutem Submit keine alten MC-/GAP-Auswahlen im Run
     * zurückbleiben.
     */
    private void saveProgress(Long runId, Question question, AnswerResultDto result, AnswerRequest request)
    {
        GameRun run = getRun(runId);

        QuestionProgress progress =
                this.questionProgressRepo.findByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId())
                        .orElseThrow(
                                () -> new NoSuchElementException(
                                        "QuestionProgress für runId="
                                                + runId
                                                + " und questionId="
                                                + question.getQuestionId()
                                                + " nicht gefunden."));

        progress.setStatus(result.isCorrect() ? ProgressStatus.CORRECT : ProgressStatus.WRONG);
        progress.setAnsweredAt(LocalDateTime.now());
        this.questionProgressRepo.save(progress);

        if (question.getQuestionType() == QuestionType.GAP)
        {
            // GAP-Antworten werden vor dem Neuschreiben komplett entfernt,
            // weil jede Lücke als eigener Datensatz gespeichert wird.
            this.runGapAnswerRepo.deleteByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId());

            Map<Long, GapField> gapFieldMap =
                    question.getGapFields().stream().collect(Collectors.toMap(GapField::getGapId, gf -> gf));

            if (request.getGapAnswers() != null)
            {
                for (var entry : request.getGapAnswers())
                {
                    GapField gapField = gapFieldMap.get(entry.getGapId());
                    if (gapField == null)
                    {
                        throw new IllegalArgumentException("Ungültige gapId: " + entry.getGapId());
                    }

                    GapOption selectedOption = gapField.getGapOptions()
                            .stream()
                            .filter(o -> o.getGapOptionId().equals(entry.getSelectedGapOptionId()))
                            .findFirst()
                            .orElseThrow(
                                    () -> new IllegalArgumentException(
                                            "Ungültige selectedGapOptionId: " + entry.getSelectedGapOptionId()));

                    RunGapAnswer runGapAnswer =
                            new RunGapAnswer(run, question, gapField, selectedOption, LocalDateTime.now());

                    this.runGapAnswerRepo.save(runGapAnswer);
                }
            }
        }
        else
        {
            this.runSelectedAnswerRepo.deleteByRun_RunIdAndQuestion_QuestionId(runId, question.getQuestionId());

            List<Long> selectedIds =
                    request.getSelectedAnswerIds() != null ? request.getSelectedAnswerIds() : Collections.emptyList();

            Map<Long, AnswerOption> answerMap =
                    question.getAnswerOptions().stream().collect(Collectors.toMap(AnswerOption::getAnswerId, a -> a));

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

    @Transactional(readOnly = true)
    public RoomStartDto startRoom(int roomId, Long runId)
    {
        getRun(runId);
        Theme theme = getTheme(roomId);

        List<QuestionProgress> progressList =
                this.questionProgressRepo.findByRunIdAndRoomIdOrderByQuestionOrder(runId, roomId);

        List<Question> orderedQuestions = progressList.stream().map(QuestionProgress::getQuestion).toList();

        if (orderedQuestions.isEmpty())
        {
            throw new IllegalStateException("Keine Fragen für Raum " + roomId + " im Run " + runId + " gefunden.");
        }

        List<Long> sequence = orderedQuestions.stream().map(Question::getQuestionId).collect(Collectors.toList());

        QuestionDto firstQuestion = toQuestionDto(orderedQuestions.get(0), 0, orderedQuestions.size());

        RoomStatusDto status = buildStatus(roomId, theme, orderedQuestions, runId);

        RoomStartDto result = new RoomStartDto();
        result.setStatus(status);
        result.setFirstQuestion(firstQuestion);
        result.setQuestionSequence(sequence);
        return result;
    }

    @Transactional
    public AnswerResultDto submitAnswer(int roomId, Long runId, AnswerRequest request)
    {
        getRun(runId);
        Question question = this.questionRepo.findById(request.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException("Frage " + request.getQuestionId() + " nicht gefunden."));

        validateQuestionBelongsToRoom(question, roomId);

        AnswerResultDto result = switch (question.getQuestionType())
        {
            case MC, TF -> evaluateMcTf(question, request);
            case GAP -> evaluateGap(question, request);
            default -> throw new IllegalArgumentException("Unbekannter Fragetyp: " + question.getQuestionType());
        };

        saveProgress(runId, question, result, request);

        return result;
    }

    private void validateQuestionBelongsToRoom(Question question, int roomId)
    {
        Theme theme = getTheme(roomId);
        boolean belongs = question.getThemes() != null
                && question.getThemes().stream().anyMatch(t -> t.getThemeId().equals(theme.getThemeId()));

        if (!belongs)
        {
            throw new IllegalArgumentException(
                    "Frage " + question.getQuestionId() + " gehört nicht zu Raum " + roomId + ".");
        }
    }

    private int getRoomIdForThemeId(Long themeId)
    {
        List<Theme> themes = this.themeRepo.findAllByOrderByThemeIdAsc();

        for (int i = 0; i < themes.size(); i++)
        {
            if (themes.get(i).getThemeId().equals(themeId))
            {
                return i + 1;
            }
        }

        throw new IllegalArgumentException("Kein Raum für themeId " + themeId + " gefunden.");
    }

    private int getRoomIdForQuestion(Question question)
    {
        if (question.getThemes() == null || question.getThemes().isEmpty())
        {
            throw new IllegalStateException(
                    "Frage " + question.getQuestionId() + " hat kein Theme.");
        }

        if (question.getThemes().size() > 1)
        {
            throw new IllegalStateException(
                    "Frage " + question.getQuestionId() + " hat mehrere Themes und ist keinem eindeutigen Raum zuordenbar.");
        }

        Long themeId = question.getThemes().get(0).getThemeId();
        return getRoomIdForThemeId(themeId);
    }
}
