package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.Room;

class GameProgress
{
	private Room currentRoom;

	public Room getCurrentRoom()
	{
		return this.currentRoom;
	}

	public void setCurrentRoom(Room room)
	{
		this.currentRoom = room;
	}
}
