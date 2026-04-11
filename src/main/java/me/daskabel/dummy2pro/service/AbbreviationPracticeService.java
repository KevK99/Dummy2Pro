package me.daskabel.dummy2pro.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.daskabel.dummy2pro.dto.RoomDtos.AnswerOptionDto;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerRequest;
import me.daskabel.dummy2pro.dto.RoomDtos.AnswerResultDto;
import me.daskabel.dummy2pro.dto.RoomDtos.PracticeRoomDto;
import me.daskabel.dummy2pro.dto.RoomDtos.QuestionDto;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;

@Service
public class AbbreviationPracticeService
{
    private static final long ABBREVIATION_THEME_ID = 17L;

    private final QuestionRepository questionRepository;
    private final ThemeRepository themeRepository;

    public AbbreviationPracticeService(QuestionRepository questionRepository, ThemeRepository themeRepository)
    {
        this.questionRepository = questionRepository;
        this.themeRepository = themeRepository;
    }

    @Transactional(readOnly = true)
    public PracticeRoomDto loadPracticeRoom()
    {
        Theme theme = this.themeRepository.findById(ABBREVIATION_THEME_ID)
                .orElseThrow(() -> new NoSuchElementException("Theme 17 wurde nicht gefunden."));

        List<Long> questionIds = this.questionRepository.findQuestionIdsByThemeId(ABBREVIATION_THEME_ID);
        Collections.shuffle(questionIds);

        if (questionIds.isEmpty())
        {
            throw new NoSuchElementException("Für Theme 17 wurden keine Fragen gefunden.");
        }

        List<Question> questions = loadQuestionsByIdsOrdered(questionIds);
        List<QuestionDto> questionDtos = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++)
        {
            questionDtos.add(toQuestionDto(questions.get(i), i, questions.size()));
        }

        PracticeRoomDto dto = new PracticeRoomDto();
        dto.setThemeName(theme.getName());
        dto.setQuestions(questionDtos);
        dto.setIntroDialog(RoomIntroDialogs.getDialogForRoom(17));
        return dto;
    }

    @Transactional(readOnly = true)
    public AnswerResultDto evaluateAnswer(AnswerRequest request)
    {
        Question question = this.questionRepository.findByQuestionIdWithAnswers(request.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Frage " + request.getQuestionId() + " nicht gefunden."));

        boolean belongsToTheme17 = question.getThemes().stream()
                .map(Theme::getThemeId)
                .anyMatch(themeId -> themeId != null && themeId == ABBREVIATION_THEME_ID);

        if (!belongsToTheme17)
        {
            throw new IllegalArgumentException("Die Frage gehört nicht zu Theme 17.");
        }

        if (question.getQuestionType() != QuestionType.MC && question.getQuestionType() != QuestionType.TF)
        {
            throw new IllegalArgumentException("Theme 17 darf nur MC/TF-Fragen enthalten.");
        }

        return RoomService.evaluateMcTf(question, request);
    }

    @Transactional(readOnly = true)
    protected List<Question> loadQuestionsByIdsOrdered(List<Long> questionIds)
    {
        if (questionIds == null || questionIds.isEmpty())
        {
            return List.of();
        }

        List<Question> questionsWithAnswers = this.questionRepository.findByQuestionIdsWithAnswers(questionIds);

        Map<Long, Question> questionMap = questionsWithAnswers.stream()
                .collect(Collectors.toMap(Question::getQuestionId, question -> question, (left, right) -> left, LinkedHashMap::new));

        return questionIds.stream()
                .map(questionMap::get)
                .filter(question -> question != null)
                .collect(Collectors.toList());
    }

    private static QuestionDto toQuestionDto(Question question, int currentIndex, int totalCount)
    {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(question.getQuestionId());
        dto.setQuestionType(question.getQuestionType());
        dto.setStartText(question.getStartText());
        dto.setImageUrl(question.getImageUrl());
        dto.setEndText(question.getEndText());
        dto.setAllowsMultiple(question.getAllowsMultiple());
        dto.setPoints(question.getPoints());
        dto.setCurrentIndex(currentIndex);
        dto.setTotalCount(totalCount);

        List<AnswerOptionDto> answerOptions = question.getAnswerOptions().stream()
                .sorted(Comparator.comparingInt(AnswerOption::getOptionOrder))
                .map(answer -> {
                    AnswerOptionDto optionDto = new AnswerOptionDto();
                    optionDto.setAnswerId(answer.getAnswerId());
                    optionDto.setOptionText(answer.getOptionText());
                    optionDto.setOptionOrder(answer.getOptionOrder());
                    return optionDto;
                })
                .collect(Collectors.toList());

        dto.setAnswerOptions(answerOptions);
        return dto;
    }
}
