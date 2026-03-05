package me.daskabel.dummy2pro.controller;

import java.util.ArrayList;
import java.util.List;

import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.Room;
import me.daskabel.dummy2pro.model.Theme;

public class RoomController
{
	List<Room> rooms = new ArrayList<>();

	public void addRoom(Room room)
	{
		this.rooms.add(room);
	}

	public List<Room> getAllRooms()
	{
		return this.rooms;
	}

	public Room getRoomById(int id)
	{
		return this.rooms.get(id);
	}

	public Room getRoomByTheme(Theme theme)
	{
		List<Room> filteredRooms = this.rooms.stream().filter(r -> r.getTheme() == theme).toList();
		return filteredRooms.get(0);
	}

	public Room getRoomFromQuestion(Question currentQuestion)
	{
		return this.rooms.stream()
					.filter(r -> r.getQuestion().stream().anyMatch(q -> q.equals(currentQuestion)))
					.toList().get(0);
	}

}
