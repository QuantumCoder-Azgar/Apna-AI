package com.example.apnaai;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class Conversation {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_message")
    public String userMessage;

    @ColumnInfo(name = "bot_response")
    public String botResponse;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
