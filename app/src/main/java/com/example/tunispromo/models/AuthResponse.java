package com.example.tunispromo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle pour la réponse JSON reçue après login ou register.
 *
 * <p>Le backend Flask renvoie ce format :
 * <pre>
 * {
 *   "access_token": "eyJhbGci...",
 *   "user": { "username": "admin", "is_admin": false }
 * }
 * </pre>
 * Gson convertit automatiquement ce JSON en objet {@link AuthResponse} grâce
 * aux annotations {@link SerializedName}.</p>
 */
public class AuthResponse {

    /** Token JWT à sauvegarder dans SharedPreferences et à envoyer dans chaque requête. */
    @SerializedName("access_token")
    private String accessToken;

    /** Infos de l'utilisateur connecté renvoyées par le backend Flask. */
    @SerializedName("user")
    private UserInfo user;

    /** @return Le token JWT reçu du backend, à préfixer par "Bearer " dans les headers. */
    public String getAccessToken() { return accessToken; }

    /** @return Les informations de l'utilisateur connecté. */
    public UserInfo getUser() { return user; }

    /**
     * Classe interne représentant le champ "user" de la réponse d'authentification.
     *
     * <p>Elle permet de connaître le nom d'utilisateur et le rôle admin directement
     * à la connexion, sans appel Firestore supplémentaire.</p>
     */
    public static class UserInfo {

        /** Nom d'utilisateur tel qu'enregistré dans la base SQLite du backend. */
        @SerializedName("username")
        private String username;

        /** true si l'utilisateur a le rôle admin (défini via le shell Flask). */
        @SerializedName("is_admin")
        private boolean isAdmin;

        /** @return Le nom d'utilisateur. */
        public String getUsername() { return username; }

        /** @return true si l'utilisateur est administrateur. */
        public boolean isAdmin() { return isAdmin; }
    }
}
