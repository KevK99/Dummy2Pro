package me.daskabel.dummy2pro.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class User
{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "username", nullable = false, unique = true)
	private String username;

	@Column(name = "passwort_hash", nullable = false)
	private String passwordHash;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserQuestionProgress> questionProgress;

	// Constructors
	public User()
	{
	}

	public User(String username, String passwordHash)
	{
		this.username = username;
		this.passwordHash = passwordHash;
	}

	public String getPasswordHash()
	{
		return passwordHash;
	}

	public List<UserQuestionProgress> getQuestionProgress()
	{
		return questionProgress;
	}

	// Getters & Setters
	public Long getUserId()
	{
		return userId;
	}

	public String getUsername()
	{
		return username;
	}

	public void setPasswordHash(String passwordHash)
	{
		this.passwordHash = passwordHash;
	}

	public void setQuestionProgress(List<UserQuestionProgress> questionProgress)
	{
		this.questionProgress = questionProgress;
	}

	public void setUserId(Long userId)
	{
		this.userId = userId;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}
}
