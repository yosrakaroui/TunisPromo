package com.example.tunispromo.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tunispromo.R;
import com.example.tunispromo.adapters.PromoAdapter;
import com.example.tunispromo.api.ApiClient;
import com.example.tunispromo.models.Promotion;
import com.example.tunispromo.models.User;
import com.example.tunispromo.utils.FlaskAuthHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity principale affichant les promotions dans un RecyclerView.
 *
 * <p>Elle regroupe les notions les plus importantes du projet : Toolbar Menu (TP6),
 * RecyclerView + Adapter/ViewHolder (TP6), Retrofit2 + Gson (TP6),
 * SwipeRefreshLayout, Intent explicite (TP2), startActivityForResult (TP5).
 *
 * <p>Le rôle admin est lu depuis Firestore {@code users/{uid}} (Firebase),
 * comme défini dans la console Firebase (champ role ou admin).</p>
 */
public class MainActivity extends AppCompatActivity {

    /** Code de retour utilisé par startActivityForResult — TP5 (DualIntent). */
    private static final int REQUEST_PROMO_DETAIL = 100;

    /** Adapter qui transforme les objets Promotion en cartes RecyclerView (TP6). */
    private PromoAdapter promoAdapter;

    /** Barre de chargement affichée pendant les appels Retrofit. */
    private ProgressBar progressBar;

    /** SwipeRefreshLayout permettant un rechargement manuel par geste. */
    private SwipeRefreshLayout swipeRefreshLayout;

    /** Bouton flottant visible seulement pour le rôle admin. */
    private FloatingActionButton fabAdmin;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    /**
     * Prépare la Toolbar, le RecyclerView et charge les promotions.
     *
     * @param savedInstanceState État précédent de l'activité si Android la recrée.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // ── Toolbar (TP6 : menu Rafraîchir / Déconnexion) ─────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ── Vues ──────────────────────────────────────────────────────────────
        progressBar       = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        fabAdmin          = findViewById(R.id.fab_admin);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        SearchView searchView     = findViewById(R.id.search_view);

        // ── RecyclerView + Adapter (TP6 : même structure que CurrencyApp du prof) ──
        promoAdapter = new PromoAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(promoAdapter);

        // ── Recherche locale dans la liste (filtre sans appel réseau) ─────────
        configureSearch(searchView);

        // ── Pull-to-refresh : recharge les promos par geste vers le bas ───────
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadPromotions();
            }
        });

        // ── Bouton Admin : rôle lu depuis Firestore users/{uid} (Firebase) ────
        fabAdmin.setVisibility(View.GONE);
        loadAdminRoleFromFirestore();

        // Intent explicite vers AdminDashboardActivity (TP2)
        // startActivityForResult pour recevoir un résultat (TP5 - DualIntent)
        fabAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Communication bidirectionnelle vers AdminDashboardActivity (voir TP5)
                startActivityForResult(
                        new Intent(MainActivity.this, AdminDashboardActivity.class),
                        REQUEST_PROMO_DETAIL
                );
            }
        });

        // Charger les promotions au démarrage
        loadPromotions();
    }

    /**
     * Lit le document Firestore users/{uid} pour afficher le FAB admin si role == admin.
     */
    private void loadAdminRoleFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        if (!document.exists()) {
                            return;
                        }
                        User user = document.toObject(User.class);
                        if (user != null && user.isAdmin()) {
                            fabAdmin.setVisibility(View.VISIBLE);
                            getSharedPreferences("auth", MODE_PRIVATE).edit()
                                    .putBoolean("is_admin", true)
                                    .putString("role", user.getRole())
                                    .apply();
                        }
                    }
                });
    }

    /**
     * Ajoute les actions du menu Toolbar depuis res/menu/menu_main.xml.
     *
     * @param menu Menu Android à remplir.
     * @return true pour afficher le menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Réagit aux clics sur les éléments du menu Toolbar (TP6).
     *
     * @param item Élément de menu sélectionné.
     * @return true si l'action est traitée ici.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_favorites) {
            // Intent explicite vers FavoritesActivity — voir TP2
            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
            return true;
        } else if (id == R.id.action_refresh) {
            // Rafraîchir : recharge les promotions depuis l'API Flask
            loadPromotions();
            Toast.makeText(this, "Rafraîchissement...", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_logout) {
            // Déconnexion avec confirmation AlertDialog (TP2)
            confirmLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Ouvre le détail d'une promotion avec startActivityForResult (TP5 - DualIntent).
     *
     * <p>Appelé par {@link PromoAdapter} lors d'un clic sur une carte.
     *
     * @param intent Intent déjà rempli par PromoAdapter avec les extras de la promotion.
     */
    public void openPromotionDetail(Intent intent) {
        // Intent bidirectionnel (voir TP5 - DualIntent du prof)
        startActivityForResult(intent, REQUEST_PROMO_DETAIL);
    }

    /**
     * Reçoit le résultat renvoyé par PromoDetailActivity ou AdminDashboardActivity (TP5).
     *
     * @param requestCode Code de demande utilisé au départ.
     * @param resultCode  Code de résultat renvoyé par l'activité appelée.
     * @param data        Intent de retour contenant un message optionnel.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Résultat reçu de PromoDetailActivity ou AdminDashboardActivity (TP5)
        if (requestCode == REQUEST_PROMO_DETAIL && resultCode == RESULT_OK && data != null) {
            String message = data.getStringExtra("message");
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
            // Recharger les promos après un scraping admin
            loadPromotions();
        }
    }

    /**
     * Configure le SearchView pour filtrer localement les cartes par mot-clé (TP6).
     *
     * @param searchView SearchView placé dans le layout activity_main.xml.
     */
    private void configureSearch(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /**
             * Filtre quand l'utilisateur valide sa recherche avec le clavier.
             *
             * @param query Texte validé par Entrée.
             * @return true car l'événement est traité.
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                promoAdapter.filter(query);
                return true;
            }

            /**
             * Filtre en temps réel pendant la frappe.
             *
             * @param newText Nouveau texte saisi caractère par caractère.
             * @return true car l'événement est traité.
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                promoAdapter.filter(newText);
                return true;
            }
        });
    }

    /**
     * Charge les promotions depuis GET /api/promos avec le token JWT (TP6 - Retrofit).
     *
     * <p>Le token JWT est récupéré depuis SharedPreferences et préfixé par "Bearer "
     * avant d'être envoyé dans le header Authorization de la requête HTTP.</p>
     */
    private void loadPromotions() {
        setLoading(true);

        // JWT Flask requis — sans token, tenter une resync automatique (IP Wi-Fi / Flask au login)
        String token = FlaskAuthHelper.getBearerHeader(this);
        if (token == null) {
            FlaskAuthHelper.retrySyncIfNeeded(this, new FlaskAuthHelper.SyncCallback() {
                @Override
                public void onSuccess(String jwtToken) {
                    fetchPromotionsWithToken(FlaskAuthHelper.getBearerHeader(MainActivity.this));
                }

                @Override
                public void onFailure(String message) {
                    setLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this,
                            "API promos: " + message
                                    + " Vérifiez Flask (python run.py) et l'IP dans ApiClient.java.",
                            Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        fetchPromotionsWithToken(token);
    }

    /**
     * Appel GET /api/promos avec le header Authorization (TP6 - Retrofit).
     */
    private void fetchPromotionsWithToken(String token) {
        if (token == null) {
            setLoading(false);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        ApiClient.getApiService().getPromos(token).enqueue(new Callback<List<Promotion>>() {

            /**
             * Traite la réponse HTTP réussie contenant la liste des promotions.
             *
             * @param call     Appel Retrofit envoyé.
             * @param response Réponse HTTP avec la liste JSON convertie par Gson.
             */
            @Override
            public void onResponse(@NonNull Call<List<Promotion>> call,
                                   @NonNull Response<List<Promotion>> response) {
                setLoading(false);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    // Succès : les données remplacent la liste actuelle du RecyclerView
                    promoAdapter.setPromotions(response.body());

                    if (response.body().isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                "Aucune promo disponible. Lance un scraping depuis le Dashboard Admin.",
                                Toast.LENGTH_LONG).show();
                    }
                } else if (response.code() == 401 || response.code() == 422) {
                    Toast.makeText(MainActivity.this,
                            "Session API expirée ou invalide (code " + response.code()
                                    + "). Reconnectez-vous.",
                            Toast.LENGTH_LONG).show();
                    logout();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Réponse API invalide. Code: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * Traite une erreur réseau (Flask non démarré ou BASE_URL incorrect).
             *
             * @param call Appel Retrofit qui a échoué.
             * @param t    Exception réseau détaillée.
             */
            @Override
            public void onFailure(@NonNull Call<List<Promotion>> call, @NonNull Throwable t) {
                setLoading(false);
                swipeRefreshLayout.setRefreshing(false);
                // Vérifier que Flask tourne : python run.py dans le dossier backend
                Toast.makeText(MainActivity.this,
                        "Erreur réseau: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Affiche une AlertDialog de confirmation avant la déconnexion (TP2).
     */
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (dialog, which) -> logout())
                .setNegativeButton("Non", null)
                .show();
    }

    /**
     * Déconnecte Firebase Auth, efface le cache local et revient à LoginActivity (TP2).
     */
    private void logout() {
        firebaseAuth.signOut();
        FlaskAuthHelper.clearFlaskPassword(this);
        getSharedPreferences("auth", MODE_PRIVATE).edit().clear().apply();

        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    /**
     * Affiche ou cache le ProgressBar central pendant un appel réseau.
     *
     * @param loading true pendant un appel Retrofit, false après la réponse.
     */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
