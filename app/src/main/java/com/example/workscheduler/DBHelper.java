package com.example.workscheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WorkScheduler.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USER = "User";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USER + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USERNAME + " TEXT)";
        db.execSQL(createUserTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

    // Save username
    public void saveUsername(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        db.insert(TABLE_USER, null, values);
        db.close();
    }

    // Get username (only first one)
    public String getUsername() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER,
                new String[]{COLUMN_USERNAME},
                null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String username = cursor.getString(0);
            cursor.close();
            db.close();
            return username;
        }

        if (cursor != null) cursor.close();
        db.close();
        return null;
    }

    // Update username
    public void updateUsername(String newUsername) {
        String current = getUsername(); // Call this BEFORE getting writable db
        
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, newUsername);
        
        if (current != null) {
            db.update(TABLE_USER, values, null, null);
        } else {
            db.insert(TABLE_USER, null, values);
        }
        db.close();
    }
}
