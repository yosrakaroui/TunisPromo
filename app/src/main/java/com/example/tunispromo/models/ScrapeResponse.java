package com.example.tunispromo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle pour la réponse JSON de POST /api/admin/scrape/{id}.
 *
 * <p>Le backend Flask renvoie un résumé du scraping :
 * <pre>
 * {
 *   "discovered": 10,
 *   "scraped": 10,
 *   "inserted": 3,
 *   "updated": 1,
 *   "skipped_no_promo": 6,
 *   "errors": 0
 * }
 * </pre>
 * Utilisé dans {@link com.example.tunispromo.activities.AdminDashboardActivity}
 * pour afficher un résumé du scraping dans un Toast.</p>
 */
public class ScrapeResponse {

    /** Nombre d'URLs produits découvertes lors de ce scraping. */
    @SerializedName("discovered")
    private int discovered;

    /** Nombre de pages effectivement visitées et analysées. */
    @SerializedName("scraped")
    private int scraped;

    /** Nombre de nouvelles promotions insérées en base. */
    @SerializedName("inserted")
    private int inserted;

    /** Nombre de promotions existantes mises à jour. */
    @SerializedName("updated")
    private int updated;

    /** Nombre de pages sans promotion (old_price == final_price). */
    @SerializedName("skipped_no_promo")
    private int skippedNoPromo;

    /** Nombre d'erreurs rencontrées pendant le scraping. */
    @SerializedName("errors")
    private int errors;

    /** @return Nombre d'URLs découvertes. */
    public int getDiscovered() { return discovered; }

    /** @return Nombre de pages scrapées. */
    public int getScraped() { return scraped; }

    /** @return Nombre de promos insérées. */
    public int getInserted() { return inserted; }

    /** @return Nombre de promos mises à jour. */
    public int getUpdated() { return updated; }

    /** @return Nombre de pages sans promo. */
    public int getSkippedNoPromo() { return skippedNoPromo; }

    /** @return Nombre d'erreurs. */
    public int getErrors() { return errors; }

    /**
     * Génère un résumé lisible du scraping pour l'afficher dans un Toast.
     *
     * @return Chaîne résumant les résultats du scraping.
     */
    public String toSummary() {
        return "Découvertes: " + discovered +
                " | Insérées: " + inserted +
                " | Mises à jour: " + updated +
                " | Erreurs: " + errors;
    }
}
