package com.example.tunispromo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle pour le corps JSON envoyé lors de l'inscription et de la connexion.
 *
 * <p>Gson sérialise automatiquement cet objet en JSON grâce aux annotations
 * {@link SerializedName}, qui correspondent exactement aux clés attendues par
 * l'API Flask : {"username": "...", "password": "..."}.</p>
 */
public class AuthRequest {

    /** Nom d'utilisateur envoyé à POST /api/auth/register et POST /api/auth/login. */
    @SerializedName("username")
    private String username;

    /** Mot de passe envoyé en clair (HTTPS conseillé en production). */
    @SerializedName("password")
    private String password;

    /**
     * Construit une requête d'authentification avec les identifiants saisis.
     *
     * @param username Nom d'utilisateur choisi ou existant.
     * @param password Mot de passe saisi dans le formulaire.
     */
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /** @return Le nom d'utilisateur. */
    public String getUsername() { return username; }

    /** @return Le mot de passe. */
    public String getPassword() { return password; }
}
