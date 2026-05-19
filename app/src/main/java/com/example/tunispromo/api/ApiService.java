package com.example.tunispromo.api;

import com.example.tunispromo.models.AuthRequest;
import com.example.tunispromo.models.AuthResponse;
import com.example.tunispromo.models.Promotion;
import com.example.tunispromo.models.ScrapeResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface Retrofit2 décrivant les endpoints REST du backend Flask.
 *
 * <p>Cette interface vient directement du TP6 : Retrofit transforme chaque méthode
 * annotée en requête HTTP, puis Gson convertit le JSON reçu en objets Java.
 *
 * <p>Ce backend utilise JWT (JSON Web Token) pour l'authentification. Chaque endpoint
 * protégé reçoit le token via le header {@code Authorization: Bearer <token>}.
 * Le token est obtenu à la connexion (POST /api/auth/login) puis sauvegardé
 * dans SharedPreferences par {@link com.example.tunispromo.activities.LoginActivity}.</p>
 */
public interface ApiService {

    // ══════════════════════════════════════════════════════════════════════════
    // AUTHENTIFICATION — endpoints publics (pas de token requis)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Crée un nouveau compte utilisateur.
     *
     * <p>Endpoint Flask : POST /api/auth/register
     * Corps JSON : {"username": "...", "password": "..."}
     * Réponse   : {"access_token": "eyJ...", "user": {"username": "...", "is_admin": false}}</p>
     *
     * @param body Objet {@link AuthRequest} sérialisé en JSON par Gson.
     * @return Call contenant {@link AuthResponse} avec le token JWT et les infos utilisateur.
     */
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body AuthRequest body);

    /**
     * Connecte un utilisateur existant et retourne son token JWT.
     *
     * <p>Endpoint Flask : POST /api/auth/login
     * Corps JSON : {"username": "...", "password": "..."}
     * Réponse   : {"access_token": "eyJ...", "user": {"username": "...", "is_admin": true/false}}</p>
     *
     * @param body Objet {@link AuthRequest} sérialisé en JSON par Gson.
     * @return Call contenant {@link AuthResponse} avec le token JWT et le flag is_admin.
     */
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest body);

    // ══════════════════════════════════════════════════════════════════════════
    // PROMOTIONS — endpoints protégés (token JWT requis)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Récupère toutes les promotions disponibles.
     *
     * <p>Endpoint Flask : GET /api/promos
     * Header requis  : Authorization: Bearer eyJ...
     * Réponse        : liste JSON de promotions avec discount_percentage calculé.</p>
     *
     * <p>Usage dans MainActivity :
     * <pre>
     *     String token = "Bearer " + prefs.getString("jwt_token", "");
     *     apiService.getPromos(token).enqueue(...);
     * </pre></p>
     *
     * @param token Token JWT préfixé par "Bearer " récupéré depuis SharedPreferences.
     * @return Call contenant la liste de {@link Promotion}.
     */
    @GET("api/promos")
    Call<List<Promotion>> getPromos(@Header("Authorization") String token);

    /**
     * Récupère les promotions filtrées par catégorie.
     *
     * <p>Endpoint Flask : GET /api/promos?category_id=1</p>
     *
     * @param token      Token JWT préfixé par "Bearer ".
     * @param categoryId Identifiant de la catégorie à filtrer.
     * @return Call contenant la liste de {@link Promotion} de cette catégorie.
     */
    @GET("api/promos")
    Call<List<Promotion>> getPromosByCategory(
            @Header("Authorization") String token,
            @Query("category_id") int categoryId
    );

    /**
     * Récupère les promotions filtrées par site source.
     *
     * <p>Endpoint Flask : GET /api/promos?website_id=1</p>
     *
     * @param token     Token JWT préfixé par "Bearer ".
     * @param websiteId Identifiant du site web source.
     * @return Call contenant la liste de {@link Promotion} de ce site.
     */
    @GET("api/promos")
    Call<List<Promotion>> getPromosByWebsite(
            @Header("Authorization") String token,
            @Query("website_id") int websiteId
    );

    // ══════════════════════════════════════════════════════════════════════════
    // SCRAPING ADMIN — endpoints réservés à is_admin == true
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Déclenche le scraping d'un site et retourne un résumé des résultats.
     *
     * <p>Endpoint Flask : POST /api/admin/scrape/{website_id}?limit=10
     * Réponse        : {"discovered":10,"scraped":10,"inserted":3,"updated":1,"errors":0}
     *
     * <p>Le scraper :
     * 1. Découvre jusqu'à {@code limit} nouvelles URLs produits non encore suivies.
     * 2. Scrape chaque page et extrait titre, prix, image selon selector_config.
     * 3. Sauvegarde une Promo seulement si old_price > final_price (vraie promo).
     * 4. Supprime les Promo dont la réduction a pris fin.</p>
     *
     * @param token     Token JWT préfixé par "Bearer " (admin requis).
     * @param websiteId Identifiant du site à scraper (obtenu lors de la création du site).
     * @param limit     Nombre maximum de nouvelles URLs à découvrir (max 50).
     * @return Call contenant {@link ScrapeResponse} avec le résumé du scraping.
     */
    @POST("api/admin/scrape/{id}")
    Call<ScrapeResponse> scrape(
            @Header("Authorization") String token,
            @Path("id") int websiteId,
            @Query("limit") int limit
    );
}
