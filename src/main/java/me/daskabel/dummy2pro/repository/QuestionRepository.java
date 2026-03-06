package me.daskabel.dummy2pro.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import me.daskabel.dummy2pro.model.Question;

class QuestionRepository
{
	private String jdbcUrl = "jdbc:mysql://localhost:3306/your_db"; // Ersetze mit deinem DB-URL
	private String username = "your_username"; // Ersetze mit deinem DB-Benutzernamen
	private String password = "your_password"; // Ersetze mit deinem DB-Passwort

	public List<Question> getQuestionsForRoom(String roomId)
	{
		List<Question> questions = new ArrayList<>();
		String sql = "SELECT question_text, answer, points FROM questions WHERE room_id = ?";

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
					PreparedStatement statement = connection.prepareStatement(sql))
		{
			statement.setString(1, roomId);
			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next())
			{
				String text = resultSet.getString("question_text");
				String answer = resultSet.getString("answer");
				int points = Integer.parseInt(resultSet.getString("points"));
				questions.add(new Question(text, answer, points));
			}
		} catch (SQLException e)
		{
			e.printStackTrace();
		}

		return questions;
	}
}
