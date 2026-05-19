package com.example.tunispromo.models;

/**
 * Modèle Java représentant un utilisateur stocké dans Firebase Firestore.
 *
 * <p>Cette classe répond à l'exigence des deux acteurs du projet: Client et Administrateur.
 * Firestore lit et écrit les champs publics via les getters/setters, d'où le constructeur vide
 * et les méthodes d'accès classiques vues dans les TPs Java/Android.</p>
 *
 * <p>Document Firestore attendu dans {@code users/{uid}} :
 * prenom, nom, email, role ("client" ou "admin"), admin (boolean optionnel).</p>
 */
public class User {

    /** Prénom saisi pendant l'inscription. */
    private String prenom;

    /** Nom saisi pendant l'inscription. */
    private String nom;

    /** Email utilisé aussi par Firebase Authentication. */
    private String email;

    /** Rôle applicatif: "client" par défaut ou "admin" si modifié dans Firestore. */
    private String role;

    /** Flag booléen admin (présent dans certains documents Firestore). */
    private boolean admin;

    /**
     * Constructeur vide obligatoire pour Firebase Firestore.
     */
    public User() {
    }

    /**
     * Constructeur complet pour créer un document utilisateur après l'inscription.
     *
     * @param prenom Prénom de l'utilisateur.
     * @param nom Nom de l'utilisateur.
     * @param email Email de connexion.
     * @param role Rôle applicatif stocké dans Firestore.
     */
    public User(String prenom, String nom, String email, String role) {
        this.prenom = prenom;
        this.nom = nom;
        this.email = email;
        this.role = role;
        this.admin = "admin".equals(role);
    }

    /** @return Prénom stocké dans Firestore. */
    public String getPrenom() {
        return prenom;
    }

    /** @param prenom Nouveau prénom. */
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    /** @return Nom stocké dans Firestore. */
    public String getNom() {
        return nom;
    }

    /** @param nom Nouveau nom. */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /** @return Email de connexion. */
    public String getEmail() {
        return email;
    }

    /** @param email Nouvel email. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return "client" ou "admin". */
    public String getRole() {
        return role;
    }

    /** @param role Nouveau rôle. */
    public void setRole(String role) {
        this.role = role;
    }

    /** @return true si le champ admin est à true dans Firestore. */
    public boolean isAdminFlag() {
        return admin;
    }

    /** @param admin Nouvelle valeur du flag admin. */
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    /**
     * Vérifie si l'utilisateur possède le rôle administrateur.
     *
     * @return true si role == "admin" ou admin == true.
     */
    public boolean isAdmin() {
        return "admin".equals(role) || admin;
    }
}
