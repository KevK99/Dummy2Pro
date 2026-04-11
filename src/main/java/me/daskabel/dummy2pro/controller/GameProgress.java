package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.Room;

/**
 * Einfache Datenklasse für den aktuellen Spielstand im Frontend-Kontext.
 *
 * Enthält die ID des aktiven Spielstands und den aktuell gewählten Raum.
 */
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