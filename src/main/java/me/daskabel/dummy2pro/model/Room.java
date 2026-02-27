package me.daskabel.dummy2pro.model;

import java.util.List;

public class Room
{
    private int roomId;
    private String roomName;
    private String description;
    private Theme theme;
    private User User;
    private List<Question> Question;

    public Room()
    {
    }

    public Room(Theme theme, String roomName, String description, User User, List<Question> Question)
    {
        this.theme = theme;
        this.roomName = roomName;
        this.description = description;
        this.User = User;
        this.Question = Question;
    }

    public String getDescription()
    {
        return description;
    }

    public String getName()
    {
        return roomName;
    }

    public User getOwner()
    {
        return User;
    }

    public List<Question> getQuestion()
    {
        return Question;
    }

    public int getRoomId()
    {
        return roomId;
    }

    public String getRoomName()
    {
        return roomName;
    }

    public Theme getTheme()
    {
        return theme;
    }

    public User getUsers()
    {
        return User;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setName(String name)
    {
        this.roomName = name;
    }

    public void setRoomId(int id)
    {
        this.roomId = id;
    }
}
