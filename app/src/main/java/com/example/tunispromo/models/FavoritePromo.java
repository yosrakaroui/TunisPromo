package com.example.tunispromo.models;

/**
 * Modèle d'une promotion enregistrée dans Firestore favorites/{uid}/promos/{id}.
 *
 * <p>Les noms des champs correspondent à ceux écrits par {@link com.example.tunispromo.activities.PromoDetailActivity}.</p>
 */
public class FavoritePromo {

    private String id;
    private String titre;
    private double prixPromo;
    private String imageUrl;
    private String siteSource;

    /** Constructeur vide requis par Firestore. */
    public FavoritePromo() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public double getPrixPromo() {
        return prixPromo;
    }

    public void setPrixPromo(double prixPromo) {
        this.prixPromo = prixPromo;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSiteSource() {
        return siteSource;
    }

    public void setSiteSource(String siteSource) {
        this.siteSource = siteSource;
    }
}
