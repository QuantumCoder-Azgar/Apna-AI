package com.example.apnaai;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "chat_memory.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "conversations";
    private static final String COL_ID = "id";
    private static final String COL_MESSAGE = "message";
    private static final String COL_IS_USER = "is_user";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public interface MessagesCallback {
        void onMessagesLoaded(List<ChatMessage> messages);
    }

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MESSAGE + " TEXT, " +
                COL_IS_USER + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertMessage(String message, boolean isUser) {
        executor.execute(() -> {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COL_MESSAGE, message);
            cv.put(COL_IS_USER, isUser ? 1 : 0);
            db.insert(TABLE_NAME, null, cv);
        });
    }

    public void getAllMessages(MessagesCallback callback) {
        executor.execute(() -> {
            final List<ChatMessage> list = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

            if (cursor.moveToFirst()) {
                do {
                    String message = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE));
                    boolean isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_USER)) == 1;
                    list.add(new ChatMessage(message, isUser));
                } while (cursor.moveToNext());
            }
            cursor.close();

            mainThreadHandler.post(() -> callback.onMessagesLoaded(list));
        });
    }
}
