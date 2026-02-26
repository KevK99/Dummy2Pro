
package me.daskabel.dummy2pro.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import me.daskabel.dummy2pro.model.Questions;

public class QuestionDAO
{

    private final DataSource dataSource;

    public QuestionDAO(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void delete(int id) throws SQLException
    {
        String query = "DELETE FROM question WHERE question_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(query))
        {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public List<Questions> findAll() throws SQLException
    {
        String query = "SELECT * FROM question";
        List<Questions> questions = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                questions.add(mapRowToQuestion(rs));
            }
        }
        return questions;
    }

    public Questions findById(int id) throws SQLException
    {
        String query = "SELECT * FROM question WHERE question_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(query))
        {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    return mapRowToQuestion(rs);
                }
            }
        }
        return null;
    }

    private Questions mapRowToQuestion(ResultSet rs) throws SQLException
    {
        Questions question = new Questions();
        question.setQuestionId(rs.getInt("question_id"));
        question.setQuestionSetId(rs.getInt("question_set_id"));
        question.setQuestionType(rs.getString("question_type"));
        question.setStartText(rs.getString("start_text"));
        question.setImageUrl(rs.getString("image_url"));
        question.setEndText(rs.getString("end_text"));
        question.setAllowsMultiple(rs.getBoolean("allows_multiple"));
        return question;
    }

    public void save(Questions question) throws SQLException
    {
        String query =
            "INSERT INTO question (question_set_id, question_type, start_text, image_url, end_text, allows_multiple) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
        {
            stmt.setInt(1, question.getQuestionSetId());
            stmt.setString(2, question.getQuestionType().toString());
            stmt.setString(3, question.getStartText());
            stmt.setString(4, question.getImageUrl());
            stmt.setString(5, question.getEndText());
            stmt.setBoolean(6, question.isAllowsMultiple());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    question.setQuestionId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void update(Questions question) throws SQLException
    {
        String query =
            "UPDATE question SET question_set_id = ?, question_type = ?, start_text = ?, image_url = ?, end_text = ?, allows_multiple = ? WHERE question_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(query))
        {
            stmt.setInt(1, question.getQuestionSetId());
            stmt.setString(2, question.getQuestionType().toString());
            stmt.setString(3, question.getStartText());
            stmt.setString(4, question.getImageUrl());
            stmt.setString(5, question.getEndText());
            stmt.setBoolean(6, question.isAllowsMultiple());
            stmt.setInt(7, question.getQuestionId());
            stmt.executeUpdate();
        }
    }
}
