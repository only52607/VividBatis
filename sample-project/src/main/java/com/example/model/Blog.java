package com.example.model;

import java.util.Date;

public class Blog {
    private int id;
    private String title;
    private String content;
    private int authorId;
    private Author author;
    private String state;
    private Date createdOn;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Date getCreatedOn() { return createdOn; }
    public void setCreatedOn(Date createdOn) { this.createdOn = createdOn; }

    @Override
    public String toString() {
        return "Blog{" +
                "id=" + id +
                ", title='" + title + "'" +
                ", author=" + (author != null ? author.getUsername() : authorId) +
                ", state='" + state + "'" +
                ", createdOn=" + createdOn +
                '}';
    }
}
