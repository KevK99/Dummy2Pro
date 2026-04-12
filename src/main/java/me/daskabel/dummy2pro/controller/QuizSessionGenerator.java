package me.daskabel.dummy2pro.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerOptionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapFieldDto;
import me.daskabel.dummy2pro.dto.RoomDtos.GapOptionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.session.QuizSession;
import me.daskabel.dummy2pro.session.QuizSession.RoomSession;

/**
 * Erzeugt Quiz-Sitzungen und Raum-Sitzungen auf Basis der vorhandenen Themes
 * und Fragen.
 *
 * Der Generator übernimmt ausschließlich den Aufbau der Laufzeitstruktur:
 * Fragen werden themenbezogen geladen, zufällig zusammengestellt und in
 * {@link QuestionDto}-Objekte ohne Korrektheitsinformationen überführt.
 *
 * Die Verwaltung bereits existierender Sitzungen ist nicht Aufgabe dieser
 * Klasse, sondern des {@code QuizSessionManager}.
 */
@Component
public class QuizSessionGenerator
{
    private static final int QUESTIONS_PER_ROOM = 40;

    private static QuestionDto toQuestionDto(Question q, int index, int total)
    {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(q.getQuestionId());
        dto.setQuestionType(q.getQuestionType());
        dto.setStartText(q.getStartText());
        dto.setImageUrl(q.getImageUrl());
        dto.setEndText(q.getEndText());
        dto.setAllowsMultiple(q.getAllowsMultiple());
        dto.setPoints(q.getPoints());
        dto.setCurrentIndex(index);
        dto.setTotalCount(total);

        // Antwortdaten werden ohne Korrektheitsflag ins DTO übertragen.
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

        if (q.getGapFields() != null && !q.getGapFields().isEmpty())
        {
            List<GapFieldDto> gapDtos = q.getGapFields().stream()
                    .sorted(Comparator.comparingInt(gf -> gf.getGapIndex()))
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
                                    .sorted(Comparator.comparingInt(o -> o.getOptionOrder()))
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
    private final ThemeRepository themeRepo;

    public QuizSessionGenerator(QuestionRepository questionRepo, ThemeRepository themeRepo)
    {
        this.questionRepo = questionRepo;
        this.themeRepo = themeRepo;
    }

    @Transactional(readOnly = true)
    public RoomSession buildRoomSession(Theme theme, int roomId)
    {
        List<Long> availableQuestionIds = new ArrayList<>(
                this.questionRepo.findQuestionIdsByThemeId(theme.getThemeId()));

        if (availableQuestionIds.isEmpty())
        {
            throw new IllegalStateException(
                    "Für Raum " + roomId + " sind keine Fragen in der Datenbank vorhanden.");
        }

        Collections.shuffle(availableQuestionIds);

        int questionCountForRun = Math.min(QUESTIONS_PER_ROOM, availableQuestionIds.size());
        List<Long> selectedIds = new ArrayList<>(availableQuestionIds.subList(0, questionCountForRun));

        List<Question> orderedSelectedQuestions = loadQuestionsByIdsOrdered(selectedIds);

        Map<Long, QuestionDto> cache = new LinkedHashMap<>();
        for (int i = 0; i < orderedSelectedQuestions.size(); i++)
        {
            Question q = orderedSelectedQuestions.get(i);
            QuestionDto dto = toQuestionDto(q, i, orderedSelectedQuestions.size());
            cache.put(q.getQuestionId(), dto);
        }

        int maxPoints = orderedSelectedQuestions.stream()
                .mapToInt(Question::getPoints)
                .sum();

        return new RoomSession(roomId, theme.getName(), selectedIds, cache, maxPoints);
    }

    @Transactional(readOnly = true)
    public QuizSession generateSkeleton(Long userId, Long runId)
    {
        QuizSession session = new QuizSession(userId, runId);

        List<Theme> themes = this.themeRepo.findAllByOrderByThemeIdAsc();
        for (int i = 0; i < themes.size(); i++)
        {
            int roomId = i + 1;
            Theme theme = themes.get(i);

            // Beim Skeleton werden Räume zunächst nur als Platzhalter angelegt.
            // Die eigentliche Fragenliste wird später erst beim Vorbereiten
            // bzw. Betreten eines Raums geladen.
            RoomSession placeholderRoom = new RoomSession(
                    roomId,
                    theme.getName(),
                    List.of(),
                    Map.of(),
                    0
            );

            session.addRoom(placeholderRoom);
        }

        return session;
    }

    @Transactional(readOnly = true)
    public List<Theme> getThemesOrdered()
    {
        return this.themeRepo.findAllByOrderByThemeIdAsc();
    }

    @Transactional(readOnly = true)
    public List<Question> loadQuestionsByIdsOrdered(List<Long> questionIds)
    {
        if (questionIds == null || questionIds.isEmpty())
        {
            return List.of();
        }

        List<Question> selectedQuestions = loadSelectedQuestions(questionIds);

        Map<Long, Question> questionMap = selectedQuestions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        // Die Reihenfolge aus der ID-Liste bleibt maßgeblich, weil IN-Abfragen
        // keine für die Anwendung verlässliche Sortierung garantieren.
        List<Question> orderedSelectedQuestions = questionIds.stream()
                .map(questionMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (orderedSelectedQuestions.size() != questionIds.size())
        {
            throw new IllegalStateException("Nicht alle ausgewählten Fragen konnten geladen werden.");
        }

        return orderedSelectedQuestions;
    }

    @Transactional(readOnly = true)
    public QuizSession generate(Long userId, Long runId)
    {
        QuizSession session = new QuizSession(userId, runId);

        List<Theme> themes = themeRepo.findAllByOrderByThemeIdAsc();
        for (int i = 0; i < themes.size(); i++)
        {
            int roomId = i + 1;
            Theme theme = themes.get(i);
            RoomSession roomSession = buildRoomSession(theme, roomId);
            session.addRoom(roomSession);
        }

        return session;
    }

    /**
     * Lädt Antwortdaten und GAP-Daten getrennt und führt sie anschließend
     * in-memory zusammen.
     *
     * Hintergrund ist die Hibernate-/JPA-Problematik bei mehreren gleichzeitig
     * gefetchten Bag-Beziehungen.
     */
    private List<Question> loadSelectedQuestions(List<Long> questionIds)
    {
        List<Question> withAnswers = this.questionRepo.findByQuestionIdsWithAnswers(questionIds);
        List<Question> withGaps = this.questionRepo.findByQuestionIdsWithGaps(questionIds);

        Map<Long, Question> mergedMap = new LinkedHashMap<>();

        for (Question q : withAnswers)
        {
            mergedMap.put(q.getQuestionId(), q);
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

        return new ArrayList<>(mergedMap.values());
    }
}
