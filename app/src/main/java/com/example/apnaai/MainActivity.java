package com.example.apnaai;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;
import android.content.pm.ApplicationInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    EditText etMessage;
    ImageButton btnSend, btnMic, btnMenu;
    LinearLayout chatContainer, chatLayout;
    ScrollView chatScroll;
    OkHttpClient httpClient;
    private TextToSpeech tts;
    private DatabaseHelper db;
    private List<ChatModel> chatList = new ArrayList<>();
    private ChatHistoryManager historyManager;
    private String currentTopic = "default_topic";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private String openAiApiKey;


    public static String normalizeSite(String text) {
        if (text == null) return null;
        String t = text.trim().toLowerCase();
        t = t.replace(" dot ", ".").replace(" point ", ".");
        t = t.replace("https://", "").replace("http://", "").trim();
        if (!t.contains(".")) t = t + ".com";
        return "https://" + t;
    }

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }



    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        openAiApiKey = prefs.getString("openai_api_key", "");

        // Update UI based on login status
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String userInitial = prefs.getString("userInitial", "");
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnNewChat = findViewById(R.id.btnNewChat);
        TextView tvProfileInitial = findViewById(R.id.tvProfileInitial);

        if (isLoggedIn) {
            btnLogin.setVisibility(View.GONE);
            btnNewChat.setVisibility(View.VISIBLE);
            if (!userInitial.isEmpty()) {
                tvProfileInitial.setText(userInitial);
                tvProfileInitial.setVisibility(View.VISIBLE);
                tvProfileInitial.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
            }
        } else {
            btnLogin.setVisibility(View.VISIBLE);
            btnNewChat.setVisibility(View.GONE);
            tvProfileInitial.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        View headerView = navigationView.getHeaderView(0);
        Button navNewChat = headerView.findViewById(R.id.nav_new_chat);
        navNewChat.setOnClickListener(v -> {
            startNewChat();
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // Initialize all views
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        chatContainer = findViewById(R.id.chatContainer);
        chatScroll = findViewById(R.id.chatScroll);
        chatLayout = findViewById(R.id.chatLayout);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnNewChat = findViewById(R.id.btnNewChat);

        btnNewChat.setOnClickListener(v -> startNewChat());

        // Set hint text colors programmatically with null checks
        if (etMessage != null) {
            etMessage.setHintTextColor(Color.parseColor("#777777"));
        }

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        openAiApiKey = prefs.getString("openai_api_key", "");

        if (openAiApiKey.isEmpty()) {
            Intent intent = new Intent(this, ApiKeyActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Please set your API key.", Toast.LENGTH_LONG).show();
            finish(); 
            return;
        } else {
            // Initial setup of the login button click listener
            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            });
        }

        db = new DatabaseHelper(this);
        historyManager = new ChatHistoryManager(this);

        String topicFromIntent = getIntent().getStringExtra("topic");
        if (topicFromIntent != null && !topicFromIntent.isEmpty()) {
            historyManager.loadChat(topicFromIntent, loaded -> {
                if (loaded != null && !loaded.isEmpty()) {
                    chatList.clear();
                    chatList.addAll(loaded);
                    currentTopic = topicFromIntent;
                    updateChatDisplay();
                } else {
                    currentTopic = topicFromIntent;
                    chatList.clear();
                    addMessage("Welcome to Apna AI! How can I help you today?", false);
                }
            });
        } else {
            startNewChat();
        }

        if (chatLayout != null) {
            chatLayout.setVisibility(View.VISIBLE);
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                String msg = etMessage.getText().toString().trim();
                if (msg.isEmpty()) {
                    Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (openAiApiKey.isEmpty()) {
                    Toast.makeText(this, "API key is not set.", Toast.LENGTH_SHORT).show();
                    return;
                }
                addMessage(msg, true);
                etMessage.setText("");
                sendToOpenAI(openAiApiKey, msg);
                historyManager.saveChat(currentTopic, new ArrayList<>(chatList));
            });
        }

    }

    void addMessage(String text, boolean isUser) {
        String sender = isUser ? "user" : "ai";
        ChatModel message = new ChatModel(sender, text, System.currentTimeMillis());
        chatList.add(message);
        db.insertMessage(text, isUser);

        runOnUiThread(() -> {
            LinearLayout msgLayout = new LinearLayout(this);
            msgLayout.setOrientation(LinearLayout.HORIZONTAL);
            msgLayout.setPadding(8, 8, 8, 8);

            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextSize(16f);
            tv.setPadding(20, 14, 20, 14);
            tv.setTextColor(isUser ? Color.WHITE : Color.BLACK);
            tv.setBackgroundResource(isUser ? R.drawable.user_bubble_enhanced : R.drawable.ai_bubble_enhanced);

            LinearLayout.LayoutParams tvParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                              ViewGroup.LayoutParams.WRAP_CONTENT);
            tvParams.weight = 1f;
            tv.setLayoutParams(tvParams);

            msgLayout.addView(tv);

            // only for AI replies → add speak icon
            if (!isUser) {
                ImageView speakIcon = new ImageView(this);
                speakIcon.setImageResource(R.drawable.ic_speaker);
                speakIcon.setColorFilter(Color.CYAN);
                LinearLayout.LayoutParams iconParams =
                    new LinearLayout.LayoutParams(60, 60);
                iconParams.setMargins(12, 0, 0, 0);
                speakIcon.setLayoutParams(iconParams);

                speakIcon.setOnClickListener(v -> speakText(text));
                msgLayout.addView(speakIcon);
            }

            LinearLayout.LayoutParams msgParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                              ViewGroup.LayoutParams.WRAP_CONTENT);
            msgParams.setMargins(0, 12, 0, 12);
            msgParams.gravity = isUser ? Gravity.END : Gravity.START;
            msgLayout.setLayoutParams(msgParams);

            chatScroll.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            chatContainer.addView(msgLayout);
            chatScroll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        });
    }


    void sendToOpenAI(String apiKey, String userMessage) {
        String lowerCaseUserMessage = userMessage.toLowerCase();

        if (lowerCaseUserMessage.startsWith("open ")) {
            String target = lowerCaseUserMessage.replaceFirst("open\\s+", "").trim();
            handleOpenCommand(target);
            return;
        }

        if (lowerCaseUserMessage.contains("what is the time") || lowerCaseUserMessage.contains("live time")) {
            String currentTime = getCurrentTime();
            runOnUiThread(() -> addMessage("The current time is " + currentTime, false));
            return;
        }

        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("model", "gpt-4o-mini");

                JSONArray messages = new JSONArray();
                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content", "You are 'Apna AI', a helpful assistant. prefer English else Hinglish. Keep replies concise.You were made by Azgar Ali! He’s the one who gave me life in the digital world. ");
                messages.put(system);

                for (ChatModel message : chatList) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", "user".equals(message.getSender()) ? "user" : "assistant");
                    msg.put("content", message.getMessage());
                    messages.put(msg);
                }

                payload.put("messages", messages);
                payload.put("max_tokens", 512);
                payload.put("temperature", 0.4);

                RequestBody body = RequestBody.create(payload.toString(), JSON);
                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .post(body)
                        .build();

                Response response = httpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    final String err = "HTTP " + response.code() + ": " + response.message();
                    runOnUiThread(() -> addMessage(err, false));
                    return;
                }
                String respStr = response.body().string();
                JSONObject respJson = new JSONObject(respStr);
                String reply = respJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim();

                runOnUiThread(() -> addMessage(reply, false));

            } catch (java.net.SocketTimeoutException e) {
                runOnUiThread(() -> addMessage("Connection TimeOut. Please check your internet connection.", false));
            } catch (Exception e) {
                runOnUiThread(() -> addMessage("Error: " + e.getMessage(), false));
            }
        }).start();
    }

    void handleOpenCommand(String target) {
        PackageManager pm = getPackageManager();
        boolean appFound = false;

        try {
            // सारे installed apps लेंगे
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);

            for (ApplicationInfo appInfo : apps) {
                String label = pm.getApplicationLabel(appInfo).toString().toLowerCase();

                // अगर app का नाम match हो गया
                if (label.contains(target.toLowerCase())) {
                    Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                        appFound = true;
                        runOnUiThread(() -> addMessage("Opening " + label +" Sir..", false));
                        break;
                    }
                }
            }

            // अगर app नहीं मिला → website open करो
            if (!appFound) {
                String url = normalizeSite(target);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
                runOnUiThread(() -> addMessage("opening " + url + "Sir..", false));
            }

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> addMessage("Error: " + e.getMessage(), false));
        }
    }


    private void loadChatHistory() {
        chatList.clear();
        chatContainer.removeAllViews();
        db.getAllMessages(legacyHistory -> {
            for (ChatMessage message : legacyHistory) {
                String sender = message.isUser() ? "user" : "ai";
                chatList.add(new ChatModel(sender, message.getMessage(), System.currentTimeMillis()));
                addMessageToView(message.getMessage(), message.isUser());
            }
        });
    }

    void addMessageToView(String text, boolean isUser) {
        runOnUiThread(() -> {
            LinearLayout msgLayout = new LinearLayout(this);
            msgLayout.setOrientation(LinearLayout.HORIZONTAL);
            msgLayout.setPadding(8, 8, 8, 8);

            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextSize(16f);
            tv.setPadding(20, 14, 20, 14);
            tv.setTextColor(isUser ? Color.WHITE : Color.BLACK);
            tv.setBackgroundResource(isUser ? R.drawable.user_bubble_enhanced : R.drawable.ai_bubble_enhanced);

            LinearLayout.LayoutParams tvParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                              ViewGroup.LayoutParams.WRAP_CONTENT);
            tvParams.weight = 1f;
            tv.setLayoutParams(tvParams);

            msgLayout.addView(tv);

            // only for AI replies → add speak icon
            if (!isUser) {
                ImageView speakIcon = new ImageView(this);
                speakIcon.setImageResource(R.drawable.ic_speaker);
                speakIcon.setColorFilter(Color.CYAN);
                LinearLayout.LayoutParams iconParams =
                    new LinearLayout.LayoutParams(60, 60);
                iconParams.setMargins(12, 0, 0, 0);
                speakIcon.setLayoutParams(iconParams);

                speakIcon.setOnClickListener(v -> speakText(text));
                msgLayout.addView(speakIcon);
            }

            LinearLayout.LayoutParams msgParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                              ViewGroup.LayoutParams.WRAP_CONTENT);
            msgParams.setMargins(0, 12, 0, 12);
            msgParams.gravity = isUser ? Gravity.END : Gravity.START;
            msgLayout.setLayoutParams(msgParams);

            chatScroll.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            chatContainer.addView(msgLayout);
            chatScroll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        });
    }



    private void openApiKeyActivity() {
        Intent intent = new Intent(this, ApiKeyActivity.class);
        startActivity(intent);
    }

    private void updateChatDisplay() {
        runOnUiThread(() -> {
            chatContainer.removeAllViews();
            for (ChatModel message : chatList) {
                addMessageToView(message.getMessage(), "user".equals(message.getSender()));
            }
        });
    }

    void speakText(String text) {
        if (tts == null) {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.getDefault());
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeakNow");
                }
            });
        } else {
            if (tts != null) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeakNow");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void startNewChat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        currentTopic = sdf.format(new Date());
        chatList.clear();
        chatContainer.removeAllViews();
        addMessage("Welcome to Apna AI! How can I help you today?", false);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_logout) {
            // Clear login status
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.putString("userInitial", "");
            editor.apply();

            // Restart the app
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_new_chat) {
            startNewChat();
        } else if (itemId == R.id.nav_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_change_api_key) {
            openApiKeyActivity();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
