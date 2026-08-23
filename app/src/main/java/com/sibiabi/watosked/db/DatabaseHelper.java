package com.sibiabi.watosked.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sibiabi.watosked.model.MessageTemplate;
import com.sibiabi.watosked.model.ScheduledMessage;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME    = "watosked.db";
    private static final int    DATABASE_VERSION = 2;

    // ---------- schedules table ----------
    public static final String TABLE_SCHEDULES   = "schedules";
    public static final String COL_ID            = "id";
    public static final String COL_RECIPIENT     = "recipient";
    public static final String COL_CONTACT_NAME  = "contact_name";
    public static final String COL_MESSAGE       = "message";
    public static final String COL_TIMESTAMP     = "timestamp";
    public static final String COL_STATUS        = "status";
    public static final String COL_REPEAT_TYPE   = "repeat_type";
    public static final String COL_REPEAT_DAYS   = "repeat_days";
    public static final String COL_WA_TYPE       = "whatsapp_type";
    public static final String COL_TEMPLATE_ID   = "template_id";

    // ---------- templates table ----------
    public static final String TABLE_TEMPLATES   = "templates";
    public static final String COL_T_ID          = "id";
    public static final String COL_T_TITLE       = "title";
    public static final String COL_T_BODY        = "body";

    private static final String CREATE_SCHEDULES =
            "CREATE TABLE " + TABLE_SCHEDULES + " (" +
            COL_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_RECIPIENT   + " TEXT, " +
            COL_CONTACT_NAME+ " TEXT DEFAULT '', " +
            COL_MESSAGE     + " TEXT, " +
            COL_TIMESTAMP   + " INTEGER, " +
            COL_STATUS      + " TEXT, " +
            COL_REPEAT_TYPE + " TEXT DEFAULT 'NONE', " +
            COL_REPEAT_DAYS + " TEXT DEFAULT '', " +
            COL_WA_TYPE     + " TEXT DEFAULT 'WHATSAPP', " +
            COL_TEMPLATE_ID + " INTEGER DEFAULT -1)";

    private static final String CREATE_TEMPLATES =
            "CREATE TABLE " + TABLE_TEMPLATES + " (" +
            COL_T_ID    + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_T_TITLE + " TEXT, " +
            COL_T_BODY  + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SCHEDULES);
        db.execSQL(CREATE_TEMPLATES);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Migrate v1 -> v2: add new columns without dropping data
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COL_CONTACT_NAME + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COL_REPEAT_TYPE + " TEXT DEFAULT 'NONE'");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COL_REPEAT_DAYS + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COL_WA_TYPE + " TEXT DEFAULT 'WHATSAPP'");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COL_TEMPLATE_ID + " INTEGER DEFAULT -1");
            db.execSQL(CREATE_TEMPLATES);
        }
    }

    // ========== Schedule CRUD ==========

    public long insertSchedule(ScheduledMessage s) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = buildScheduleValues(s);
        long id = db.insert(TABLE_SCHEDULES, null, v);
        s.setId(id);
        return id;
    }

    public void updateSchedule(ScheduledMessage s) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = buildScheduleValues(s);
        db.update(TABLE_SCHEDULES, v, COL_ID + "=?", new String[]{String.valueOf(s.getId())});
    }

    public void updateStatus(long id, String status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_STATUS, status);
        db.update(TABLE_SCHEDULES, v, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteSchedule(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SCHEDULES, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public ScheduledMessage getScheduleById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SCHEDULES, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        if (c != null && c.moveToFirst()) {
            ScheduledMessage s = cursorToSchedule(c);
            c.close();
            return s;
        }
        return null;
    }

    public List<ScheduledMessage> getPendingSchedules() {
        return queryByStatus(ScheduledMessage.STATUS_PENDING, COL_TIMESTAMP + " ASC");
    }

    public List<ScheduledMessage> getHistorySchedules() {
        SQLiteDatabase db = getReadableDatabase();
        List<ScheduledMessage> list = new ArrayList<>();
        Cursor c = db.query(TABLE_SCHEDULES, null,
                COL_STATUS + " IN (?,?)",
                new String[]{ScheduledMessage.STATUS_SENT, ScheduledMessage.STATUS_FAILED},
                null, null, COL_TIMESTAMP + " DESC");
        if (c != null && c.moveToFirst()) {
            do { list.add(cursorToSchedule(c)); } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public List<ScheduledMessage> getAllSchedules() {
        SQLiteDatabase db = getReadableDatabase();
        List<ScheduledMessage> list = new ArrayList<>();
        Cursor c = db.query(TABLE_SCHEDULES, null, null, null, null, null, COL_TIMESTAMP + " DESC");
        if (c != null && c.moveToFirst()) {
            do { list.add(cursorToSchedule(c)); } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    private List<ScheduledMessage> queryByStatus(String status, String orderBy) {
        SQLiteDatabase db = getReadableDatabase();
        List<ScheduledMessage> list = new ArrayList<>();
        Cursor c = db.query(TABLE_SCHEDULES, null, COL_STATUS + "=?",
                new String[]{status}, null, null, orderBy);
        if (c != null && c.moveToFirst()) {
            do { list.add(cursorToSchedule(c)); } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    private ContentValues buildScheduleValues(ScheduledMessage s) {
        ContentValues v = new ContentValues();
        v.put(COL_RECIPIENT,    s.getRecipient());
        v.put(COL_CONTACT_NAME, s.getContactName() != null ? s.getContactName() : "");
        v.put(COL_MESSAGE,      s.getMessage());
        v.put(COL_TIMESTAMP,    s.getTimestamp());
        v.put(COL_STATUS,       s.getStatus());
        v.put(COL_REPEAT_TYPE,  s.getRepeatType() != null ? s.getRepeatType() : ScheduledMessage.REPEAT_NONE);
        v.put(COL_REPEAT_DAYS,  s.getRepeatDays() != null ? s.getRepeatDays() : "");
        v.put(COL_WA_TYPE,      s.getWhatsappType() != null ? s.getWhatsappType() : ScheduledMessage.WA_WHATSAPP);
        v.put(COL_TEMPLATE_ID,  s.getTemplateId());
        return v;
    }

    private ScheduledMessage cursorToSchedule(Cursor c) {
        int idxContactName  = c.getColumnIndex(COL_CONTACT_NAME);
        int idxRepeatType   = c.getColumnIndex(COL_REPEAT_TYPE);
        int idxRepeatDays   = c.getColumnIndex(COL_REPEAT_DAYS);
        int idxWaType       = c.getColumnIndex(COL_WA_TYPE);
        int idxTemplateId   = c.getColumnIndex(COL_TEMPLATE_ID);

        return new ScheduledMessage(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_RECIPIENT)),
                idxContactName  >= 0 ? c.getString(idxContactName)  : "",
                c.getString(c.getColumnIndexOrThrow(COL_MESSAGE)),
                c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP)),
                c.getString(c.getColumnIndexOrThrow(COL_STATUS)),
                idxRepeatType   >= 0 ? c.getString(idxRepeatType)   : ScheduledMessage.REPEAT_NONE,
                idxRepeatDays   >= 0 ? c.getString(idxRepeatDays)   : "",
                idxWaType       >= 0 ? c.getString(idxWaType)       : ScheduledMessage.WA_WHATSAPP,
                idxTemplateId   >= 0 ? c.getLong(idxTemplateId)     : -1L
        );
    }

    // ========== Template CRUD ==========

    public long insertTemplate(MessageTemplate t) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_T_TITLE, t.getTitle());
        v.put(COL_T_BODY,  t.getBody());
        long id = db.insert(TABLE_TEMPLATES, null, v);
        t.setId(id);
        return id;
    }

    public void deleteTemplate(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_TEMPLATES, COL_T_ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<MessageTemplate> getAllTemplates() {
        SQLiteDatabase db = getReadableDatabase();
        List<MessageTemplate> list = new ArrayList<>();
        Cursor c = db.query(TABLE_TEMPLATES, null, null, null, null, null, COL_T_TITLE + " ASC");
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new MessageTemplate(
                        c.getLong(c.getColumnIndexOrThrow(COL_T_ID)),
                        c.getString(c.getColumnIndexOrThrow(COL_T_TITLE)),
                        c.getString(c.getColumnIndexOrThrow(COL_T_BODY))
                ));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }
}