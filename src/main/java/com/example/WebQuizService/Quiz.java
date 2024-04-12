package com.example.WebQuizService;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.List;

@Entity
@Table(name = "Quiz")
public class Quiz {
    @JsonPropertyOrder({"id","title","text","options","answer"})

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotBlank(message = "The title cannot be blank")
    @Column(name = "title")
    private String title;

    @NotBlank(message = "The text field cannot be blank")
    @Column(name = "text")
    private String text;

    @NotNull
    @Size(min = 2, message = "There have to be at least 2 options")
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @Column(name = "options")
    private List<String> options;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "answer_index")
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Integer> answer;

    @ManyToOne
    @JoinColumn(name = "appUser_id")
    private AppUser author;

    public Quiz() {}

    public Quiz(String title, String text, List<String> options, List<Integer> answer, AppUser author) {
        this.title = title;
        this.text = text;
        this.options = options;
        this.answer = answer;
        this.author = author;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<Integer> getAnswer() {
        return answer;
    }

    public void setAnswer(List<Integer> answer) {
        this.answer = answer;
    }

    public AppUser getAuthor() {
        return author;
    }

    public void setAuthor(AppUser author) {
        this.author = author;
    }
}
