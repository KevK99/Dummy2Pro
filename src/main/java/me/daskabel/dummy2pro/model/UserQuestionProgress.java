package me.daskabel.dummy2pro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_question_progress")
public class UserQuestionProgress {

    @EmbeddedId
    private UserQuestionProgressId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "status")
    private String status;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "selected_answer_id")
    private Long selectedAnswerId;

    // Constructors
    public UserQuestionProgress() {}

    public UserQuestionProgress(User user, Question question) {
        this.id = new UserQuestionProgressId(user.getUserId(), question.getQuestionId());
        this.user = user;
        this.question = question;
    }

    // Getters & Setters
    public UserQuestionProgressId getId() { return id; }
    public void setId(UserQuestionProgressId id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }

    public Long getSelectedAnswerId() { return selectedAnswerId; }
    public void setSelectedAnswerId(Long selectedAnswerId) { this.selectedAnswerId = selectedAnswerId; }
}
