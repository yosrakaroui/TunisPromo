package com.example.tunispromo.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tunispromo.R;
import com.example.tunispromo.models.User;
import com.example.tunispromo.utils.FlaskAuthHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity de connexion par email et mot de passe via Firebase Authentication.
 *
 * <p>Elle couvre le TP1 pour les widgets EditText/Button/TextView, le TP2 pour les Intents
 * explicites, et Firebase Auth + Firestore pour lire le rôle dans {@code users/{uid}}.
 *
 * <p>Le token JWT Flask est récupéré en arrière-plan pour charger les promos (Retrofit TP6).</p>
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        progressBar = findViewById(R.id.progress_bar);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_register);

        // Firebase Auth — connexion email/mot de passe (voir TP Firebase du cours)
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Intent explicite — voir TP2
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    /**
     * Connecte l'utilisateur avec Firebase Auth puis lit son profil Firestore users/{uid}.
     */
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    "Connexion échouée: " + getErrorMessage(task),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser == null) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, "Utilisateur introuvable", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        loadUserProfileAndContinue(firebaseUser, password);
                    }
                });
    }

    /**
     * Lit le document Firestore users/{uid} pour obtenir le rôle admin/client.
     */
    private void loadUserProfileAndContinue(final FirebaseUser firebaseUser, final String password) {
        firestore.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        if (!document.exists()) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    "Profil Firestore introuvable pour cet utilisateur",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        User user = document.toObject(User.class);
                        if (user == null) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    "Profil utilisateur invalide",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        boolean isAdmin = user.isAdmin();
                        String role = user.getRole() != null ? user.getRole() : "client";

                        // SharedPreferences — cache local du rôle et de l'email (voir TP6)
                        SharedPreferences prefs = getSharedPreferences(FlaskAuthHelper.PREFS_AUTH, MODE_PRIVATE);
                        prefs.edit()
                                .putString("email", firebaseUser.getEmail())
                                .putString("uid", firebaseUser.getUid())
                                .putString("role", role)
                                .putBoolean("is_admin", isAdmin)
                                .apply();

                        // Mot de passe local pour resync Flask si IP Wi-Fi change (voir FlaskAuthHelper)
                        FlaskAuthHelper.saveFlaskPassword(LoginActivity.this, password);

                        // Retrofit — attendre le JWT Flask avant MainActivity (évite erreur 422)
                        final String email = firebaseUser.getEmail();
                        FlaskAuthHelper.syncFlaskToken(LoginActivity.this, email, password,
                                new FlaskAuthHelper.SyncCallback() {
                                    @Override
                                    public void onSuccess(String jwtToken) {
                                        setLoading(false);
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(String message) {
                                        setLoading(false);
                                        Toast.makeText(LoginActivity.this,
                                                "Connexion OK (Firebase) mais API promos: " + message
                                                        + ". Vérifiez que Flask tourne (python run.py).",
                                                Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Erreur Firestore: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getErrorMessage(Task<?> task) {
        if (task.getException() != null && task.getException().getMessage() != null) {
            return task.getException().getMessage();
        }
        return "Erreur inconnue";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
