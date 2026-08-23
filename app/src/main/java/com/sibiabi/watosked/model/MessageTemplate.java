package com.sibiabi.watosked.model;

public class MessageTemplate {
    private long   id;
    private String title;
    private String body;

    public MessageTemplate() {}

    public MessageTemplate(long id, String title, String body) {
        this.id    = id;
        this.title = title;
        this.body  = body;
    }

    public MessageTemplate(String title, String body) {
        this.title = title;
        this.body  = body;
    }

    public long getId()             { return id; }
    public void setId(long id)      { this.id = id; }
    public String getTitle()        { return title; }
    public void setTitle(String t)  { this.title = t; }
    public String getBody()         { return body; }
    public void setBody(String b)   { this.body = b; }
}