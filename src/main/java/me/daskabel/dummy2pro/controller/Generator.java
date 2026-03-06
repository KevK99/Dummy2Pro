package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.Room;

class QuizGenerator
{
	public Quiz generateQuiz()
	{
		Quiz quiz = new Quiz();

		// Erstelle Räume und Fragen
		Room room1 = new Room("Room1", "Math");
		room1.addQuestion(new Question("Was ist 2 + 2?", "4"));
		room1.addQuestion(new Question("Was ist 3 * 3?", "9"));

		Room room2 = new Room("Room2", "Science");
		room2.addQuestion(new Question("Was ist die chemische Formel für Wasser?", "H2O"));
		room2.addQuestion(new Question("Wie viele Planeten hat unser Sonnensystem?", "8"));

		quiz.addRoom(room1);
		quiz.addRoom(room2);

		// Setze den aktuellen Raum
		quiz.setCurrentRoom(room1); // Optional: initialer Raum

		return quiz;
	}
}
