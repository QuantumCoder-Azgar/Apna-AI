package com.example.apnaai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ApiKeyActivity extends AppCompatActivity {
    private EditText etApiKey;
    private Button btnSaveKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_key);

        etApiKey = findViewById(R.id.etApiKey);
        btnSaveKey = findViewById(R.id.btnSaveKey);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentApiKey = prefs.getString("openai_api_key", "");
        etApiKey.setText(currentApiKey);

        btnSaveKey.setOnClickListener(v -> {
            String newApiKey = etApiKey.getText().toString().trim();
            if (newApiKey.isEmpty()) {
                Toast.makeText(this, "API key cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("openai_api_key", newApiKey);
            editor.apply();

            Toast.makeText(this, "API key saved.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
