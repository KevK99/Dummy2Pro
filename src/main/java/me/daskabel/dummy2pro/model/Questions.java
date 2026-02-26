package me.daskabel.dummy2pro.model;

public class Questions
{
    private int id;
    private int questionSetId;
    private QuestionType type;
    private Theme theme;
    private String startText;
    private String endText;
    private String imageUrl;
    private boolean isMultipleChoice;
    private boolean isAnswered;
    private boolean isCorrect;
    private int points;

    public Questions()
    {
    }

    public Questions(QuestionType type, Theme theme, String startText, String endText, String imageUrl,
        boolean isMultipleChoice, int points)
    {
        this.type = type;
        this.theme = theme;
        this.startText = startText;
        this.endText = endText;
        this.imageUrl = imageUrl;
        this.isMultipleChoice = isMultipleChoice;
        this.points = points;
    }

    public String getEndText()
    {
        return endText;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public int getPoints()
    {
        return points;
    }

    public int getQuestionId()
    {
        return id;
    }

    public int getQuestionSetId()
    {
        return questionSetId;
    }

    public QuestionType getQuestionType()
    {
        return type;
    }

    public String getStartText()
    {
        return startText;
    }

    public Theme getTheme()
    {
        return theme;
    }

    public QuestionType getType()
    {
        return type;
    }

    public boolean isAllowsMultiple()
    {
        return isMultipleChoice;
    }

    public boolean isAnswered()
    {
        return isAnswered;
    }

    public boolean isCorrect()
    {
        return isCorrect;
    }

    public boolean isMultipleChoice()
    {
        return isMultipleChoice;
    }

    public void setAllowsMultiple(boolean boolean1)
    {
        this.isMultipleChoice = boolean1;
    }

    public void setAnswered(boolean isAnswered)
    {
        this.isAnswered = isAnswered;
    }

    public void setCorrect(boolean isCorrect)
    {
        this.isCorrect = isCorrect;
    }

    public void setEndText(String string)
    {
        this.endText = string;
    }

    public void setImageUrl(String string)
    {
        this.imageUrl = string;
    }

    public void setQuestionId(int id)
    {
        this.id = id;
    }

    public void setQuestionSetId(int id)
    {
        this.questionSetId = id;
    }

    public void setQuestionType(String string)
    {
        this.type = QuestionType.valueOf(string);
    }

    public void setStartText(String string)
    {
        this.startText = string;
    }

}
