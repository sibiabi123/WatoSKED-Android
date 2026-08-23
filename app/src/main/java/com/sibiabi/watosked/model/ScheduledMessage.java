package com.sibiabi.watosked.model;

public class ScheduledMessage {
    private long id;
    private String recipient;
    private String message;
    private long timestamp;
    private String status; // PENDING, SENT, FAILED

    public ScheduledMessage() {
    }

    public ScheduledMessage(long id, String recipient, String message, long timestamp, String status) {
        this.id = id;
        this.recipient = recipient;
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
    }

    public ScheduledMessage(String recipient, String message, long timestamp, String status) {
        this.recipient = recipient;
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
