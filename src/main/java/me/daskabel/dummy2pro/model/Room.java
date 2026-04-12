package me.daskabel.dummy2pro.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Repräsentiert einen Raum innerhalb des Spiels.
 *
 * Ein Raum besitzt eine Nummer, einen Namen, eine Beschreibung,
 * ein zugeordnetes Theme, optional einen aktuellen Benutzer und
 * die zugehörigen Fragen.
 */
public class Room
{
    private int roomId;
    private String roomName;
    private String description;
    private Theme theme;
    private User currentUser;
    private List<Question> questions;

    public Room()
    {
        this.questions = new ArrayList<>();
    }

    public Room(String roomName, String themeName)
    {
        this.theme = new Theme(themeName);
        this.roomName = roomName;
        this.description = null;
        this.currentUser = null;
        this.questions = new ArrayList<>();
    }

    public Room(Theme theme, String roomName, String description, User currentUser,
                List<Question> questions)
    {
        this.theme = theme;
        this.roomName = roomName;
        this.description = description;
        this.currentUser = currentUser;
        this.questions = questions != null ? questions : new ArrayList<>();
    }

    /**
     * Fügt dem Raum eine Frage hinzu.
     */
    public void addQuestion(Question question)
    {
        this.questions.add(question);
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getName()
    {
        return this.roomName;
    }

    public List<Question> getQuestion()
    {
        return this.questions;
    }

    public int getRoomId()
    {
        return this.roomId;
    }

    public Theme getTheme()
    {
        return this.theme;
    }

    public User getCurrentUser()
    {
        return this.currentUser;
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

    public void setTheme(Theme theme)
    {
        this.theme = theme;
    }

    public void setCurrentUser(User currentUser)
    {
        this.currentUser = currentUser;
    }
}
