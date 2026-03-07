package me.daskabel.dummy2pro.model;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "avatar")
    private String avatar;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameRun> gameRuns;

    public User()
    {
    }

    public User(String username, String passwordHash)
    {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public User(String username, String passwordHash, String avatar)
    {
        this.username = username;
        this.passwordHash = passwordHash;
        this.avatar = avatar;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPasswordHash()
    {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash)
    {
        this.passwordHash = passwordHash;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public List<GameRun> getGameRuns()
    {
        return gameRuns;
    }

    public void setGameRuns(List<GameRun> gameRuns)
    {
        this.gameRuns = gameRuns;
    }
}