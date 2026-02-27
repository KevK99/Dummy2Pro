package me.daskabel.dummy2pro.controller;

import java.util.ArrayList;
import java.util.List;

import me.daskabel.dummy2pro.model.Room;

public class RoomController
{
    List<Room> rooms = new ArrayList<>();

    public void addRoom(Room room)
    {
        rooms.add(room);
    }

}
