package com.example.WebQuizService;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "Completions")
public class Completion {

    @ManyToOne
    @JoinColumn(name = "appUser_id")
    private AppUser author;

    @Column(name = "quiz_id")
    @NotBlank
    private Long id;

    @Column(name = "time_of_completion")
    @NotBlank
    private LocalDateTime completedAt;

    public Completion(AppUser author, Long id) {
        this.author = author;
        this.id = id;
        this.completedAt = LocalDateTime.now();
    }

    public AppUser getAuthor() {
        return author;
    }

    public void setAuthor(AppUser author) {
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
