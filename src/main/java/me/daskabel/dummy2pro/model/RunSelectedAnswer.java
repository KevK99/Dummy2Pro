package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;

/**
 * Speichert eine ausgewählte Antwortoption zu einer Frage innerhalb
 * eines Spielstands.
 *
 * Die Klasse wird für Multiple-Choice- und Richtig/Falsch-Fragen
 * verwendet.
 */
@Entity
@Table(name = "run_selected_answer")
public class RunSelectedAnswer
{
    @EmbeddedId
    private RunSelectedAnswerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("runId")
    @JoinColumn(name = "run_id", nullable = false)
    private GameRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("answerId")
    @JoinColumn(name = "answer_id", nullable = false)
    private AnswerOption answerOption;

    public RunSelectedAnswer()
    {
    }

    public RunSelectedAnswer(GameRun run, Question question, AnswerOption answerOption)
    {
        this.id = new RunSelectedAnswerId(
                run.getRunId(),
                question.getQuestionId(),
                answerOption.getAnswerId()
        );
        this.run = run;
        this.question = question;
        this.answerOption = answerOption;
    }

    public RunSelectedAnswerId getId()
    {
        return id;
    }

    public void setId(RunSelectedAnswerId id)
    {
        this.id = id;
    }

    public GameRun getRun()
    {
        return run;
    }

    public void setRun(GameRun run)
    {
        this.run = run;
    }

    public Question getQuestion()
    {
        return question;
    }

    public void setQuestion(Question question)
    {
        this.question = question;
    }

    public AnswerOption getAnswerOption()
    {
        return answerOption;
    }

    public void setAnswerOption(AnswerOption answerOption)
    {
        this.answerOption = answerOption;
    }
}
