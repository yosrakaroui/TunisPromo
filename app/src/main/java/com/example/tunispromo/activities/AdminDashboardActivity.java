package com.example.tunispromo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tunispromo.R;
import com.example.tunispromo.api.ApiClient;
import com.example.tunispromo.models.ScrapeResponse;
import com.example.tunispromo.models.User;
import com.example.tunispromo.utils.FlaskAuthHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tableau de bord réservé aux utilisateurs dont is_admin == true.
 *
 * <p>Cette Activity prouve l'existence du deuxième acteur demandé par le professeur.
 * Elle vérifie le rôle admin depuis Firestore users/{uid} et appelle
 * POST /api/admin/scrape/{id} via Retrofit (TP6).
 *
 * <p>Communication bidirectionnelle avec MainActivity via setResult (TP5 - DualIntent) :
 * après un scraping réussi, MainActivity recharge automatiquement les promotions.</p>
 */
public class AdminDashboardActivity extends AppCompatActivity {

    /** ID du site Mytek dans la base Flask. */
    private static final int WEBSITE_ID = 1;

    /** Nombre de nouvelles URLs à découvrir par appel scraping. */
    private static final int SCRAPE_LIMIT = 10;

    private ProgressBar progressBar;
    private TextView tvResult;
    private Button btnRefresh;
    private Button btnRetour;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    /** true si l'utilisateur est admin Firestore — évite le scrape si accès refusé. */
    private boolean adminVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        progressBar = findViewById(R.id.progress_bar);
        tvResult = findViewById(R.id.tv_result);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnRetour = findViewById(R.id.btn_retour);

        btnRefresh.setEnabled(false);

        verifyAdminAccess();

        // Clic sur "Forcer le rechargement" → POST /api/admin/scrape/{id} (TP6 - Retrofit)
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forceScrape();
            }
        });

        // Clic sur "Retour" → renvoie RESULT_OK à MainActivity (TP5 - DualIntent)
        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("message", "Retour du Dashboard Admin");
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    /**
     * Vérifie que l'utilisateur est admin via Firestore (role == "admin" ou admin == true).
     */
    private void verifyAdminAccess() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        if (isFinishing()) {
                            return;
                        }
                        if (!document.exists()) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Profil utilisateur introuvable", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        User user = document.toObject(User.class);
                        if (user == null || !user.isAdmin()) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Accès administrateur refusé", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        adminVerified = true;
                        btnRefresh.setEnabled(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (isFinishing()) {
                            return;
                        }
                        Toast.makeText(AdminDashboardActivity.this,
                                "Erreur Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    /**
     * Appelle POST /api/admin/scrape/{id} pour déclencher le scraping côté Flask.
     */
    private void forceScrape() {
        if (!adminVerified) {
            Toast.makeText(this, "Accès administrateur non confirmé", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("Scraping en cours...");

        String token = FlaskAuthHelper.getBearerHeader(this);
        if (token != null) {
            executeScrapeRequest(token);
            return;
        }

        // Resync JWT Flask si manquant (même logique que MainActivity)
        FlaskAuthHelper.retrySyncIfNeeded(this, new FlaskAuthHelper.SyncCallback() {
            @Override
            public void onSuccess(String jwtToken) {
                if (isFinishing()) {
                    return;
                }
                String bearer = FlaskAuthHelper.getBearerHeader(AdminDashboardActivity.this);
                if (bearer != null) {
                    executeScrapeRequest(bearer);
                } else {
                    onScrapeTokenMissing();
                }
            }

            @Override
            public void onFailure(String message) {
                if (isFinishing()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                tvResult.setText("Token API manquant.");
                Toast.makeText(AdminDashboardActivity.this,
                        "API promos: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onScrapeTokenMissing() {
        progressBar.setVisibility(View.GONE);
        tvResult.setText("Token API manquant. Reconnectez-vous.");
        Toast.makeText(this, "Token API manquant. Reconnectez-vous.", Toast.LENGTH_LONG).show();
    }

    /**
     * Lance POST /api/admin/scrape/{id}?limit=10 avec le token JWT.
     */
    private void executeScrapeRequest(String token) {
        ApiClient.getApiService().scrape(token, WEBSITE_ID, SCRAPE_LIMIT)
                .enqueue(new Callback<ScrapeResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<ScrapeResponse> call,
                                           @NonNull Response<ScrapeResponse> response) {
                        if (isFinishing()) {
                            return;
                        }
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ScrapeResponse result = response.body();
                            tvResult.setText(result.toSummary());
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Scraping terminé : " + result.getInserted() + " nouvelles promos",
                                    Toast.LENGTH_LONG).show();

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("message",
                                    "Scraping OK : " + result.getInserted() + " insérées");
                            setResult(RESULT_OK, resultIntent);

                        } else if (response.code() == 403) {
                            tvResult.setText("Accès refusé par le serveur (403)");
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Le serveur refuse l'accès admin. Vérifier is_admin dans Flask.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            tvResult.setText("Erreur serveur. Code: " + response.code());
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Réponse API invalide. Code: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ScrapeResponse> call, @NonNull Throwable t) {
                        if (isFinishing()) {
                            return;
                        }
                        progressBar.setVisibility(View.GONE);
                        tvResult.setText("Erreur réseau: " + t.getMessage());
                        Toast.makeText(AdminDashboardActivity.this,
                                "Erreur réseau: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
