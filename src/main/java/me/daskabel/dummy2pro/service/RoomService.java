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

	// Singleton Pattern
	private static RoomService instance;

	public static RoomService getInstance()
	{
		if (instance == null)
		{
			instance = new RoomService();
		}
		return instance;
	}

	private List<Room> rooms;

	private RoomService()
	{
		this.rooms = new ArrayList<>();
		loadRooms();
	}

	public List<Room> getAllRooms()
	{
		return this.rooms;
	}

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

	public String navigateToRoom(int roomId)
	{
		Room room = getRoomById(roomId);
		return "Navigating to " + room.getRoomId() + " with theme " + room.getTheme();
	}
}