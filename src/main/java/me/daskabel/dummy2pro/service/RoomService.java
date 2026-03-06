package me.daskabel.dummy2pro.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import me.daskabel.dummy2pro.model.Room;

public class RoomService
{
	public class RoomNotFoundException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public RoomNotFoundException(String msg)
		{
			new Exception(msg);
		}
	}

	private List<Room> rooms;

	public Room getRoomById(int roomId)
	{
		return this.rooms.stream().filter(room -> room.getRoomId() == roomId).toList().get(0);
	}

	@PostConstruct
	public void init()
	{
		this.rooms = new ArrayList<>();
		loadRooms();
	}

	private void loadRooms()
	{
		// Beispiel für die statische Definition von Räumen
		this.rooms.add(new Room("Room1", "Theme1"));
		this.rooms.add(new Room("Room2", "Theme2"));
	}
}
