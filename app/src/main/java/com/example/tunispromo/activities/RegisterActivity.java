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
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity d'inscription via Firebase Authentication + document Firestore users/{uid}.
 *
 * <p>Crée le compte dans Firebase Auth, puis enregistre prenom, nom, email, role="client"
 * dans la collection Firestore {@code users}. Pour promouvoir un admin : modifier
 * {@code role} à "admin" (ou admin=true) dans la console Firebase.</p>
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etPrenom, etNom, etEmail, etPassword, etConfirm;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etPrenom = findViewById(R.id.et_prenom);
        etNom = findViewById(R.id.et_nom);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirm = findViewById(R.id.et_confirm);
        progressBar = findViewById(R.id.progress_bar);
        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvLogin = findViewById(R.id.tv_login);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser() {
        String prenom = etPrenom.getText().toString().trim();
        String nom = etNom.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(prenom) || TextUtils.isEmpty(nom)
                || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            setLoading(false);
                            Toast.makeText(RegisterActivity.this,
                                    "Inscription échouée: " + getErrorMessage(task),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser == null) {
                            setLoading(false);
                            Toast.makeText(RegisterActivity.this, "Utilisateur introuvable", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        saveUserInFirestore(firebaseUser, prenom, nom, email, password);
                    }
                });
    }

    /**
     * Crée le document users/{uid} dans Firestore avec le rôle client par défaut.
     */
    private void saveUserInFirestore(final FirebaseUser firebaseUser, String prenom, String nom,
                                     final String email, final String password) {
        User user = new User(prenom, nom, email, "client");

        firestore.collection("users").document(firebaseUser.getUid()).set(user)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        SharedPreferences prefs = getSharedPreferences(FlaskAuthHelper.PREFS_AUTH, MODE_PRIVATE);
                        prefs.edit()
                                .putString("email", email)
                                .putString("uid", firebaseUser.getUid())
                                .putString("role", "client")
                                .putBoolean("is_admin", false)
                                .apply();

                        FlaskAuthHelper.saveFlaskPassword(RegisterActivity.this, password);

                        FlaskAuthHelper.syncFlaskToken(RegisterActivity.this, email, password,
                                new FlaskAuthHelper.SyncCallback() {
                                    @Override
                                    public void onSuccess(String jwtToken) {
                                        setLoading(false);
                                        Toast.makeText(RegisterActivity.this,
                                                "Compte créé ! Bienvenue " + prenom,
                                                Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(String message) {
                                        setLoading(false);
                                        Toast.makeText(RegisterActivity.this,
                                                "Compte Firebase OK mais Flask: " + message,
                                                Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                        finish();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this,
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
