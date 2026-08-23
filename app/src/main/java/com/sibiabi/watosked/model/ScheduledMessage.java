package com.sibiabi.watosked.model;

public class ScheduledMessage {
    // Repeat types
    public static final String REPEAT_NONE    = "NONE";
    public static final String REPEAT_DAILY   = "DAILY";
    public static final String REPEAT_WEEKLY  = "WEEKLY";
    public static final String REPEAT_CUSTOM  = "CUSTOM";

    // WhatsApp account types
    public static final String WA_WHATSAPP    = "WHATSAPP";
    public static final String WA_BUSINESS    = "WHATSAPP_BUSINESS";

    // Status values
    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_SENT      = "SENT";
    public static final String STATUS_FAILED    = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private long   id;
    private String recipient;
    private String contactName;
    private String message;
    private long   timestamp;
    private String status;
    private String repeatType;
    private String repeatDays;
    private String whatsappType;
    private long   templateId;

    public ScheduledMessage() {
        this.repeatType   = REPEAT_NONE;
        this.whatsappType = WA_WHATSAPP;
        this.templateId   = -1;
        this.contactName  = "";
    }

    public ScheduledMessage(long id, String recipient, String contactName, String message,
                            long timestamp, String status, String repeatType,
                            String repeatDays, String whatsappType, long templateId) {
        this.id           = id;
        this.recipient    = recipient;
        this.contactName  = contactName;
        this.message      = message;
        this.timestamp    = timestamp;
        this.status       = status;
        this.repeatType   = repeatType != null ? repeatType : REPEAT_NONE;
        this.repeatDays   = repeatDays;
        this.whatsappType = whatsappType != null ? whatsappType : WA_WHATSAPP;
        this.templateId   = templateId;
    }

    public ScheduledMessage(String recipient, String message, long timestamp, String status) {
        this.recipient    = recipient;
        this.message      = message;
        this.timestamp    = timestamp;
        this.status       = status;
        this.repeatType   = REPEAT_NONE;
        this.whatsappType = WA_WHATSAPP;
        this.templateId   = -1;
        this.contactName  = "";
    }

    public long getId()                          { return id; }
    public void setId(long id)                   { this.id = id; }
    public String getRecipient()                 { return recipient; }
    public void setRecipient(String r)           { this.recipient = r; }
    public String getContactName()               { return contactName; }
    public void setContactName(String n)         { this.contactName = n; }
    public String getMessage()                   { return message; }
    public void setMessage(String m)             { this.message = m; }
    public long getTimestamp()                   { return timestamp; }
    public void setTimestamp(long t)             { this.timestamp = t; }
    public String getStatus()                    { return status; }
    public void setStatus(String s)              { this.status = s; }
    public String getRepeatType()                { return repeatType; }
    public void setRepeatType(String r)          { this.repeatType = r; }
    public String getRepeatDays()                { return repeatDays; }
    public void setRepeatDays(String d)          { this.repeatDays = d; }
    public String getWhatsappType()              { return whatsappType; }
    public void setWhatsappType(String w)        { this.whatsappType = w; }
    public long getTemplateId()                  { return templateId; }
    public void setTemplateId(long t)            { this.templateId = t; }

    public boolean isRepeating() {
        return repeatType != null && !repeatType.equals(REPEAT_NONE);
    }

    public boolean isWhatsAppBusiness() {
        return WA_BUSINESS.equals(whatsappType);
    }

    public String getWhatsAppPackage() {
        return isWhatsAppBusiness() ? "com.whatsapp.w4b" : "com.whatsapp";
    }

    public String getDisplayName() {
        if (contactName != null && !contactName.isEmpty()) return contactName;
        return recipient;
    }
}