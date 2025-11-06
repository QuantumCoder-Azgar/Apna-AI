package com.example.apnaai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText fullName, emailOrMobile, password;
    private Button registerButton, googleSignInButton;
    private ProgressBar loadingIndicator;
    private TextView loginLink;
    private DBHelper dbHelper;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Log.d("RegisterActivity", "onCreate: Activity created");

        dbHelper = new DBHelper(this);

        fullName = findViewById(R.id.fullName);
        emailOrMobile = findViewById(R.id.emailOrMobile);
        password = findViewById(R.id.password);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInButton.setOnClickListener(v -> signIn());

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = fullName.getText().toString().trim();
                String contact = emailOrMobile.getText().toString().trim();
                String pass = password.getText().toString().trim();

                if (name.isEmpty() || contact.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                } else {
                    boolean isInserted = dbHelper.insertData(name, contact, pass);
                    if (isInserted) {
                        Toast.makeText(RegisterActivity.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void signIn() {
        Log.d("RegisterActivity", "signIn: Attempting Google Sign-In");
        loadingIndicator.setVisibility(View.VISIBLE);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("RegisterActivity", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("RegisterActivity", "onActivityResult: Google Sign-In successful, proceeding with Firebase auth");
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e("RegisterActivity", "onActivityResult: Google Sign-In failed", e);
                loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(this, "Google sign-in failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d("RegisterActivity", "firebaseAuthWithGoogle: Authenticating with Firebase");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("RegisterActivity", "firebaseAuthWithGoogle: Firebase authentication successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String email = user.getEmail();
                            String name = user.getDisplayName();
                            String uid = user.getUid();

                            if (!dbHelper.checkUser(email)) {
                                dbHelper.insertData(name, email, "");
                            }

                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RegisterActivity.this);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("isLoggedIn", true);
                            if (name != null && !name.isEmpty()) {
                                editor.putString("userInitial", String.valueOf(name.charAt(0)));
                            }
                            editor.apply();

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Log.e("RegisterActivity", "firebaseAuthWithGoogle: Firebase authentication failed", task.getException());
                        loadingIndicator.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "Google sign-in failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
