package com.example.tunispromo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tunispromo.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Écran de démarrage simple de l'application TunisPromo.
 *
 * <p>Cette Activity illustre le TP1 avec un layout XML simple, puis le TP2 avec un Intent
 * explicite vers LoginActivity ou MainActivity selon l'état FirebaseAuth.</p>
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                goToNextScreen();
            }
        }, SPLASH_DELAY);
    }

    /**
     * Choisit l'écran suivant selon l'utilisateur Firebase connecté.
     */
    private void goToNextScreen() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Intent intent;

        if (auth.getCurrentUser() != null) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
