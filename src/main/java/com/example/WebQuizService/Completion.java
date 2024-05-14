package com.example.WebQuizService;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "Completions")
public class Completion {
    @JsonPropertyOrder({"id","completedAt"})

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long id;

    @ManyToOne
    @JoinColumn(name = "appUser_id")
    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private AppUser author;

    @Column(name = "quiz_id")
    @JsonProperty("id")
    @NotNull
    private Long quizId;

    @Column(name = "time_of_completion")
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    public Completion() {
    }

    public Completion(AppUser author, Long quizId) {
        this.author = author;
        this.quizId = quizId;
        this.completedAt = LocalDateTime.now();
    }

    public AppUser getAuthor() {
        return author;
    }

    public void setAuthor(AppUser author) {
        this.author = author;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
