package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.Room;

public class GameProgress
{
    private Long runId;
    private Room currentRoom;

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public Room getCurrentRoom()
    {
        return currentRoom;
    }

    public void setCurrentRoom(Room room)
    {
        this.currentRoom = room;
    }
}
