package com.example.tunispromo.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * Singleton Retrofit qui fournit une unique instance du client HTTP.
 *
 * <p>Vient directement du TP6 : un seul objet Retrofit est créé et réutilisé
 * dans toute l'application pour éviter les fuites mémoire et les connexions multiples.
 *
 * <p>⚠ BASE_URL à modifier selon l'environnement :
 * <ul>
 *   <li>Émulateur Android Studio → {@code http://10.0.2.2:5000/}
 *       (10.0.2.2 est l'alias de localhost depuis l'émulateur)</li>
 *   <li>Téléphone réel (même WiFi) → {@code http://192.168.X.X:5000/}
 *       (trouver l'IP avec ipconfig sous Windows)</li>
 * </ul>
 */
public class ApiClient {

    // ── URL de base du backend Flask ───────────────────────────────────────────
    // ÉMULATEUR : "http://10.0.2.2:5000/"
    // TÉLÉPHONE RÉEL : IP Wi-Fi (ipconfig → Carte réseau sans fil Wi-Fi → Adresse IPv4)
    private static final String BASE_URL = "http://192.168.1.3:5000/";

    /** Instance unique Retrofit (pattern Singleton). */
    private static Retrofit retrofit = null;

    /**
     * Retourne l'instance Retrofit en la créant une seule fois.
     *
     * <p>Configure :
     * <ul>
     *   <li>L'URL de base du serveur Flask</li>
     *   <li>Le convertisseur Gson (JSON ↔ objets Java)</li>
     *   <li>Un intercepteur de logs pour voir les requêtes dans Logcat</li>
     *   <li>Des timeouts de 30 secondes pour éviter les blocages réseau</li>
     * </ul>
     *
     * @return Instance Retrofit configurée et prête à créer des services API.
     */
    public static Retrofit getClient() {
        if (retrofit == null) {

            // Intercepteur HTTP : affiche les requêtes et réponses dans Logcat (onglet Debug)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Client OkHttp avec timeouts et logs
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)  // Timeout connexion : 30s
                    .readTimeout(30, TimeUnit.SECONDS)     // Timeout lecture : 30s
                    .writeTimeout(30, TimeUnit.SECONDS)    // Timeout écriture : 30s
                    .addInterceptor(loggingInterceptor)    // Logs visibles dans Logcat
                    .build();

            // Construction de l'instance Retrofit (TP6 : même structure que CurrencyApp du prof)
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)                               // URL du serveur Flask
                    .addConverterFactory(GsonConverterFactory.create()) // JSON → Java via Gson
                    .client(okHttpClient)                            // Client HTTP configuré
                    .build();
        }
        return retrofit;
    }

    /**
     * Raccourci pour obtenir directement le service API sans appeler getClient().create().
     *
     * <p>Usage :
     * <pre>
     *     ApiClient.getApiService().login(body).enqueue(...);
     * </pre>
     *
     * @return Instance de {@link ApiService} prête à faire des appels réseau.
     */
    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}
