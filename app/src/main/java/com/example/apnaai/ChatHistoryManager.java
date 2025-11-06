package com.example.apnaai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatHistoryManager {
    private Context context;
    private Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public interface ChatLoadCallback {
        void onChatLoaded(ArrayList<ChatModel> chatList);
    }

    public interface TopicsCallback {
        void onTopicsLoaded(List<String> topics);
    }

    public ChatHistoryManager(Context context) {
        this.context = context;
    }

    public void saveChat(String topic, ArrayList<ChatModel> chatList) {
        executor.execute(() -> {
            try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), topic + ".json"))) {
                gson.toJson(chatList, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void loadChat(String topic, ChatLoadCallback callback) {
        executor.execute(() -> {
            File file = new File(context.getFilesDir(), topic + ".json");
            if (!file.exists()) {
                mainThreadHandler.post(() -> callback.onChatLoaded(new ArrayList<>()));
                return;
            }
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<ArrayList<ChatModel>>() {}.getType();
                ArrayList<ChatModel> chatList = gson.fromJson(reader, type);
                mainThreadHandler.post(() -> callback.onChatLoaded(chatList));
            } catch (Exception e) {
                e.printStackTrace();
                mainThreadHandler.post(() -> callback.onChatLoaded(new ArrayList<>()));
            }
        });
    }

    public void getAllTopics(TopicsCallback callback) {
        executor.execute(() -> {
            List<String> topics = new ArrayList<>();
            File[] files = context.getFilesDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".json")) {
                        topics.add(file.getName().replace(".json", ""));
                    }
                }
            }
            mainThreadHandler.post(() -> callback.onTopicsLoaded(topics));
        });
    }
}
