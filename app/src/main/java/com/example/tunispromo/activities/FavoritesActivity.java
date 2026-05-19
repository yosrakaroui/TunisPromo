package com.example.tunispromo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tunispromo.R;
import com.example.tunispromo.adapters.FavoritesAdapter;
import com.example.tunispromo.models.FavoritePromo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity listant les promotions favorites de l'utilisateur connecté (Firestore).
 *
 * <p>Lit la sous-collection {@code favorites/{uid}/promos} — Firebase Firestore (TP6).</p>
 */
public class FavoritesActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private FavoritesAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView recyclerFavorites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        recyclerFavorites = findViewById(R.id.recycler_favorites);

        adapter = new FavoritesAdapter(this);
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        recyclerFavorites.setAdapter(adapter);

        loadFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Charge tous les documents de favorites/{uid}/promos depuis Firestore.
     */
    private void loadFavorites() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        firestore.collection("favorites").document(user.getUid()).collection("promos")
                .get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        progressBar.setVisibility(View.GONE);

                        List<FavoritePromo> list = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            FavoritePromo fav = doc.toObject(FavoritePromo.class);
                            if (fav != null) {
                                if (fav.getId() == null || fav.getId().isEmpty()) {
                                    fav.setId(doc.getId());
                                }
                                list.add(fav);
                            }
                        }

                        adapter.setFavorites(list);
                        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        Toast.makeText(FavoritesActivity.this,
                                "Erreur Firestore: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
