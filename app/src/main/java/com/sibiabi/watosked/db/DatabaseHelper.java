package com.sibiabi.watosked.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sibiabi.watosked.model.ScheduledMessage;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "watosked.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SCHEDULES = "schedules";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_RECIPIENT = "recipient";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_STATUS = "status";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_SCHEDULES + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_RECIPIENT + " TEXT, "
            + COLUMN_MESSAGE + " TEXT, "
            + COLUMN_TIMESTAMP + " INTEGER, "
            + COLUMN_STATUS + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
        onCreate(db);
    }

    public long insertSchedule(ScheduledMessage schedule) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RECIPIENT, schedule.getRecipient());
        values.put(COLUMN_MESSAGE, schedule.getMessage());
        values.put(COLUMN_TIMESTAMP, schedule.getTimestamp());
        values.put(COLUMN_STATUS, schedule.getStatus());

        long id = db.insert(TABLE_SCHEDULES, null, values);
        schedule.setId(id);
        return id;
    }

    public void updateStatus(long id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, status);
        db.update(TABLE_SCHEDULES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<ScheduledMessage> getAllSchedules() {
        List<ScheduledMessage> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCHEDULES, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ScheduledMessage schedule = new ScheduledMessage(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECIPIENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))
                );
                list.add(schedule);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<ScheduledMessage> getPendingSchedules() {
        List<ScheduledMessage> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCHEDULES, null, COLUMN_STATUS + " = ?", new String[]{"PENDING"}, null, null, COLUMN_TIMESTAMP + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ScheduledMessage schedule = new ScheduledMessage(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECIPIENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))
                );
                list.add(schedule);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }
}
