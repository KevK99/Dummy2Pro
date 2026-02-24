package me.daskabel.dummy2pro.model;

public class Player
{
    private static final long serialVersionUID = 1L;
    private String name;
    private int score;

    public Player(String name)
    {
        this.name = name;
        this.score = 0;
    }

    public void addScore(int points)
    {
        this.score += points;
    }

    public String getName()
    {
        return name;
    }

    public int getScore()
    {
        return score;
    }
}
