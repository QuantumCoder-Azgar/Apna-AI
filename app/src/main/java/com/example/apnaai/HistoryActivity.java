package com.example.apnaai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ChatHistoryManager historyManager;
    private ListView topicsListView;
    private ArrayAdapter<String> adapter;
    private List<String> topics = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        topicsListView = findViewById(R.id.topicsListView);
        historyManager = new ChatHistoryManager(this);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, topics);
        topicsListView.setAdapter(adapter);

        topicsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTopic = topics.get(position);
            Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
            intent.putExtra("topic", selectedTopic);
            startActivity(intent);
        });

        loadTopics();
    }

    private void loadTopics() {
        historyManager.getAllTopics(loadedTopics -> {
            topics.clear();
            topics.addAll(loadedTopics);
            adapter.notifyDataSetChanged();
        });
    }
}
