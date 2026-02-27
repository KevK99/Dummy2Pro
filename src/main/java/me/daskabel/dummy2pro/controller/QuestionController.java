package me.daskabel.dummy2pro.controller;

import java.util.ArrayList;
import java.util.List;

import me.daskabel.dummy2pro.model.Question;

public class QuestionController
{
    private List<Question> questions = new ArrayList<>();

    public void addQuestion(Question question)
    {
        questions.add(question);
    }

    public List<Question> getQuestions()
    {
        return questions;
    }

}
