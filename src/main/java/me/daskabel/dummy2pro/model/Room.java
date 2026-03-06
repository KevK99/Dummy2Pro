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

	public Room(String roomName, String themeName)
	{
		this.theme = new Theme(themeName);
		this.roomName = roomName;
		this.description = null;
		this.User = null;
		this.Question = null;
	}

	public Room(Theme theme, String roomName, String description, User User,
				List<Question> Question)
	{
		this.theme = theme;
		this.roomName = roomName;
		this.description = description;
		this.User = User;
		this.Question = Question;
	}

	public void addQuestion(Question question)
	{
		this.Question.add(question);
	}

	public String getDescription()
	{
		return this.description;
	}

	public String getName()
	{
		return this.roomName;
	}

	public User getOwner()
	{
		return this.User;
	}

	public List<Question> getQuestion()
	{
		return this.Question;
	}

	public int getRoomId()
	{
		return this.roomId;
	}

	public String getRoomName()
	{
		return this.roomName;
	}

	public Theme getTheme()
	{
		return this.theme;
	}

	public User getUsers()
	{
		return this.User;
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
