package com.example.tunispromo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle de données représentant une promotion reçue depuis GET /api/promos.
 *
 * <p>Le backend Flask renvoie ce format JSON pour chaque promo :
 * <pre>
 * {
 *   "id": 1,
 *   "title": "Samsung Galaxy A55",
 *   "final_price": 899.0,
 *   "old_price": 1299.0,
 *   "discount_percentage": 30.8,
 *   "image": "https://...",
 *   "currency": "TND",
 *   "product_url": "https://www.mytek.tn/...",
 *   "website_name": "Mytek",
 *   "category_name": "electronics"
 * }
 * </pre>
 * Les annotations {@link SerializedName} font correspondre les champs Java
 * aux clés JSON exactes du backend.</p>
 */
public class Promotion {

    /** Identifiant unique de la promotion dans la base SQLite du backend. */
    @SerializedName("id")
    private int id;

    /** Titre du produit en promotion (champ "title" côté Flask). */
    @SerializedName("title")
    private String titre;

    /** Prix final après réduction (champ "final_price" côté Flask). */
    @SerializedName("final_price")
    private double prixPromo;

    /** Prix original avant réduction (champ "old_price" côté Flask). */
    @SerializedName("old_price")
    private double prixOriginal;

    /** Pourcentage de réduction calculé automatiquement par le backend. */
    @SerializedName("discount_percentage")
    private double reduction;

    /** URL de l'image produit (champ "image" côté Flask). */
    @SerializedName("image")
    private String imageUrl;

    /** Devise (ex: "TND"). */
    @SerializedName("currency")
    private String currency;

    /** URL directe vers la page produit pour le bouton "Voir sur le site". */
    @SerializedName("product_url")
    private String lienPromo;

    /** Nom du site source (ex: "Mytek", "Fatales"). */
    @SerializedName("website_name")
    private String siteSource;

    /** Nom de la catégorie associée (ex: "electronics"). */
    @SerializedName("category_name")
    private String categorie;

    // ─── Constructeur vide requis par Gson pour la désérialisation ─────────────
    public Promotion() {}

    // ─── Getters ───────────────────────────────────────────────────────────────

    /** @return L'ID de la promotion. */
    public int getId() { return id; }

    /** @return L'ID sous forme de String pour les Intent extras. */
    public String getIdAsString() { return String.valueOf(id); }

    /** @return Le titre du produit. */
    public String getTitre() { return titre; }

    /** @return Le prix promo (final_price). */
    public double getPrixPromo() { return prixPromo; }

    /** @return Le prix original avant réduction (old_price). */
    public double getPrixOriginal() { return prixOriginal; }

    /** @return Le pourcentage de réduction. */
    public double getReduction() { return reduction; }

    /** @return L'URL de l'image produit. */
    public String getImageUrl() { return imageUrl; }

    /** @return La devise (ex: TND). */
    public String getCurrency() { return currency; }

    /** @return L'URL de la page produit pour le bouton "Voir sur le site". */
    public String getLienPromo() { return lienPromo; }

    /** @return Le nom du site source. */
    public String getSiteSource() { return siteSource; }

    /** @return La catégorie du produit. */
    public String getCategorie() { return categorie; }
}
