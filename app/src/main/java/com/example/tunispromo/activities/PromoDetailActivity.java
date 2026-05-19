package com.example.tunispromo.activities;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.tunispromo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity de détail d'une promotion ouverte par Intent explicite depuis la liste.
 *
 * <p>Elle couvre le TP2 avec les extras d'Intent, le TP3 avec ACTION_VIEW pour ouvrir
 * le navigateur, et Firebase Firestore pour enregistrer les favoris de l'utilisateur.</p>
 */
public class PromoDetailActivity extends AppCompatActivity {

    /** Données de la promotion reçues depuis l'Intent. */
    private String id, titre, imageUrl, siteSource, lienPromo, categorie;

    /** Prix et réduction reçus depuis l'Intent. */
    private double prixOriginal, prixPromo, reduction;

    /** Firebase Auth + Firestore pour favorites/{uid}/promos/{promo_id}. */
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    /**
     * Initialise l'écran de détail, lit les extras et configure les boutons.
     *
     * @param savedInstanceState État précédent de l'activité si Android la recrée.
     * @return Rien, Android appelle cette méthode pour créer l'écran.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Détail promotion");
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        readIntentExtras();
        bindViews();
    }

    /**
     * Gère la flèche retour de l'ActionBar.
     *
     * @return true si le retour est traité.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Lit toutes les valeurs envoyées par PromoAdapter dans l'Intent explicite.
     *
     * @return Rien, les valeurs sont stockées dans les attributs de l'activité.
     */
    private void readIntentExtras() {
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        titre = intent.getStringExtra("titre");
        prixOriginal = intent.getDoubleExtra("prix_original", 0);
        prixPromo = intent.getDoubleExtra("prix_promo", 0);
        reduction = intent.getDoubleExtra("reduction", 0);
        imageUrl = intent.getStringExtra("image_url");
        siteSource = intent.getStringExtra("site_source");
        lienPromo = intent.getStringExtra("lien_promo");
        categorie = intent.getStringExtra("categorie");
    }

    /**
     * Relie les vues XML aux données de la promotion et configure les clics.
     *
     * @return Rien, la méthode remplit directement les vues.
     */
    private void bindViews() {
        ImageView ivPromo = findViewById(R.id.iv_promo);
        TextView tvSite = findViewById(R.id.tv_site);
        TextView tvTitre = findViewById(R.id.tv_titre);
        TextView tvPrixPromo = findViewById(R.id.tv_prix_promo);
        TextView tvPrixOriginal = findViewById(R.id.tv_prix_original);
        TextView tvReduction = findViewById(R.id.tv_reduction);
        TextView tvCategorie = findViewById(R.id.tv_categorie);
        Button btnVoirSite = findViewById(R.id.btn_voir_site);
        Button btnFavoris = findViewById(R.id.btn_favoris);

        tvSite.setText(siteSource);
        tvTitre.setText(titre);
        tvPrixPromo.setText(String.format(Locale.FRANCE, "%.2f TND", prixPromo));
        tvPrixOriginal.setText(String.format(Locale.FRANCE, "%.2f TND", prixOriginal));
        tvPrixOriginal.setPaintFlags(tvPrixOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tvReduction.setText(String.format(Locale.FRANCE, "-%.0f%%", reduction));
        tvCategorie.setText("Catégorie: " + (categorie == null ? "" : categorie));

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_promo_placeholder)
                .error(R.drawable.ic_broken_image)
                .centerCrop()
                .into(ivPromo);

        btnVoirSite.setOnClickListener(v -> openPromotionInBrowser());
        btnFavoris.setOnClickListener(v -> addToFavorites());
    }

    /**
     * Ouvre le lien de la promotion dans le navigateur avec un Intent implicite ACTION_VIEW.
     *
     * @return Rien, Android choisit l'application capable d'ouvrir l'URL.
     */
    private void openPromotionInBrowser() {
        if (lienPromo == null || lienPromo.trim().isEmpty()) {
            Toast.makeText(this, "Lien indisponible", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(lienPromo));
        startActivity(browserIntent);
    }

    /**
     * Enregistre la promotion dans la sous-collection favoris de l'utilisateur connecté.
     *
     * @return Rien, le résultat est traité par les listeners Firestore.
     */
    private void addToFavorites() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();
        Map<String, Object> favorite = new HashMap<>();
        favorite.put("id", id);
        favorite.put("titre", titre);
        favorite.put("prixPromo", prixPromo);
        favorite.put("imageUrl", imageUrl);
        favorite.put("siteSource", siteSource);

        /*
         * Structure Firestore attendue:
         * Collection "favorites" -> Document {uid}
         * Sous-collection "promos" -> Document {promo_id}
         * champs: id, titre, prixPromo, imageUrl, siteSource
         */
        firestore.collection("favorites").document(uid)
                .collection("promos").document(id == null ? "promo_sans_id" : id)
                .set(favorite)
                .addOnSuccessListener(unused -> {
                    // Succès: le favori est sauvegardé et un résultat est renvoyé à MainActivity.
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("message", "Ajouté aux favoris !");
                    setResult(RESULT_OK, resultIntent);
                    Toast.makeText(PromoDetailActivity.this, "Ajouté aux favoris !", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Échec Firestore: permissions, connexion réseau ou configuration Firebase.
                    Toast.makeText(PromoDetailActivity.this, "Erreur favoris: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
