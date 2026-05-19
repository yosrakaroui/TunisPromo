package com.example.tunispromo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.tunispromo.api.ApiClient;
import com.example.tunispromo.models.AuthRequest;
import com.example.tunispromo.models.AuthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Synchronise le compte Firebase avec le backend Flask (JWT pour GET /api/promos).
 *
 * <p>Si l'utilisateur existe dans Firebase mais pas dans Flask, tente un register automatique.</p>
 */
public final class FlaskAuthHelper {

  /** Nom du fichier SharedPreferences partagé avec les activités. */
  public static final String PREFS_AUTH = "auth";

  /** Clé du token JWT Flask. */
  public static final String KEY_JWT = "jwt_token";

  /** Mot de passe gardé localement pour resynchroniser Flask si l'IP réseau a changé. */
  public static final String KEY_FLASK_PASSWORD = "flask_password";

  private FlaskAuthHelper() {
  }

  /**
   * Tente de récupérer un JWT si la connexion précédente a échoué (mauvaise IP, Flask arrêté).
   */
  public static void retrySyncIfNeeded(final Context context, final SyncCallback callback) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE);
    String email = prefs.getString("email", "");
    String password = prefs.getString(KEY_FLASK_PASSWORD, "");
    if (email.isEmpty() || password.isEmpty()) {
      callback.onFailure("Reconnectez-vous (email/mot de passe) pour obtenir le token API promos.");
      return;
    }
    syncFlaskToken(context, email, password, callback);
  }

  /**
   * Interface de retour après synchronisation Flask.
   */
  public interface SyncCallback {
    /** @param jwtToken Token JWT Flask enregistré. */
    void onSuccess(String jwtToken);

    /** @param message Message d'erreur lisible. */
    void onFailure(String message);
  }

  /**
   * Login Flask puis register si 401 (utilisateur Firebase seulement).
   */
  public static void syncFlaskToken(final Context context, String email, String password,
                                    final SyncCallback callback) {
    if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
      callback.onFailure("Email ou mot de passe manquant pour Flask");
      return;
    }

    AuthRequest request = new AuthRequest(email, password);
    ApiClient.getApiService().login(request).enqueue(new Callback<AuthResponse>() {
      @Override
      public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          String token = response.body().getAccessToken();
          saveJwt(context, token);
          callback.onSuccess(token);
          return;
        }
        if (response.code() == 401) {
          registerFlaskUser(context, email, password, callback);
          return;
        }
        callback.onFailure("Flask login échoué (code " + response.code() + ")");
      }

      @Override
      public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
        callback.onFailure("Flask injoignable: " + t.getMessage());
      }
    });
  }

  private static void registerFlaskUser(final Context context, String email, String password,
                                        final SyncCallback callback) {
    ApiClient.getApiService().register(new AuthRequest(email, password)).enqueue(new Callback<AuthResponse>() {
      @Override
      public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          String token = response.body().getAccessToken();
          saveJwt(context, token);
          callback.onSuccess(token);
          return;
        }
        if (response.code() == 409) {
          retryLoginOnly(context, email, password, callback);
          return;
        }
        callback.onFailure("Flask register échoué (code " + response.code() + ")");
      }

      @Override
      public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
        callback.onFailure("Flask injoignable: " + t.getMessage());
      }
    });
  }

  /** @return Token JWT ou chaîne vide. */
  public static String getJwtToken(Context context) {
    return context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE).getString(KEY_JWT, "");
  }

  /** @return Header Authorization complet ou null si pas de token. */
  public static String getBearerHeader(Context context) {
    String jwt = getJwtToken(context);
    if (jwt.isEmpty()) {
      return null;
    }
    return "Bearer " + jwt;
  }

  private static void retryLoginOnly(final Context context, String email, String password,
                                     final SyncCallback callback) {
    ApiClient.getApiService().login(new AuthRequest(email, password)).enqueue(new Callback<AuthResponse>() {
      @Override
      public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          String token = response.body().getAccessToken();
          saveJwt(context, token);
          callback.onSuccess(token);
          return;
        }
        callback.onFailure("Compte Flask existant mais mot de passe différent (code "
            + response.code() + ")");
      }

      @Override
      public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
        callback.onFailure("Flask injoignable: " + t.getMessage());
      }
    });
  }

  private static void saveJwt(Context context, String token) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE);
    prefs.edit().putString(KEY_JWT, token).apply();
  }

  /** Enregistre le mot de passe pour une resync Flask ultérieure (MainActivity). */
  public static void saveFlaskPassword(Context context, String password) {
    if (password == null || password.isEmpty()) {
      return;
    }
    context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE).edit()
        .putString(KEY_FLASK_PASSWORD, password)
        .apply();
  }

  /** Efface le mot de passe local à la déconnexion. */
  public static void clearFlaskPassword(Context context) {
    context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE).edit()
        .remove(KEY_FLASK_PASSWORD)
        .remove(KEY_JWT)
        .apply();
  }
}
