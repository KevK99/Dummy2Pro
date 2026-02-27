package me.daskabel.dummy2pro.controller;

import javax.sql.DataSource;

import me.daskabel.dummy2pro.model.Room;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.model.User;

public class Generator
{
    public Generator()
    {
    }

    public void generate(DataSource ds, User player)
    {
        QuestionController questionController = new QuestionController();
        // for (Question question : questionList)
        // {
        // questionController.addQuestion(question);
        // }

        RoomController roomController = new RoomController();
        Theme[] themes = Theme.values();
        for (Theme theme : themes)
        {
            Room room = new Room(
                theme,
                "Room for " + theme.getName(),
                "Description for " + theme.getName(),
                player,
                questionController.getQuestions());
            roomController.addRoom(room);
        }
    }
}
