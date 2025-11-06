package com.example.apnaai;

import android.content.Context;

import java.util.List;

public class MemoryManager {
    // private final ConversationDao conversationDao;

    public MemoryManager(Context context) {
        // AppDatabase db = AppDatabase.getDatabase(context);
        // conversationDao = db.conversationDao();
    }

    public void saveConversation(String userMsg, String botReply) {
        // Conversation c = new Conversation();
        // c.userMessage = userMsg;
        // c.botResponse = botReply;
        // c.timestamp = System.currentTimeMillis();
        // conversationDao.insert(c);
    }

    public String getConversationContext() {
        // List<Conversation> all = conversationDao.getAllConversations();
        // StringBuilder context = new StringBuilder();
        // for (Conversation c : all) {
        //     context.append("User: ").append(c.userMessage).append("\n");
        //     context.append("AI: ").append(c.botResponse).append("\n");
        // }
        // return context.toString();
        return "";
    }
    
    public List<Conversation> getAllConversations() {
        // return conversationDao.getAllConversations();
        return new java.util.ArrayList<>();
    }
}
