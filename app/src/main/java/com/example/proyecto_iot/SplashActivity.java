package com.example.proyecto_iot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private ExoPlayer player;
    private boolean navigationDone = false;  // evita dobles ejecuciones

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SplashScreen.installSplashScreen(this);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        PlayerView playerView = findViewById(R.id.playerView);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Fade-in
        playerView.setAlpha(0f);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    playerView.animate().alpha(1f).setDuration(120).start();
                }
            }
        });

        // Cargar animación
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.kova_animacion);
        MediaItem item = MediaItem.fromUri(uri);

        player.setMediaItem(item);
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.setVolume(0f);
        player.prepare();
        player.play();

        // Salto manual o automático
        playerView.postDelayed(() -> navigateNextSafe(), 4000);
        playerView.setOnClickListener(v -> navigateNextSafe());
    }

    private synchronized void navigateNextSafe() {
        if (navigationDone) return;
        navigationDone = true;
        navigateNext();
    }

    // Lógica profesional de navegación
    private void navigateNext() {

        SharedPreferences prefs = getSharedPreferences("kova_prefs", MODE_PRIVATE);

        boolean onboardingSeen   = prefs.getBoolean("onboarding_seen",   false);
        boolean wizardCompleted  = prefs.getBoolean("wizard_completed",  false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Onboarding
        if (!onboardingSeen) {
            go(OnboardingIntroActivity.class);
            return;
        }

        // Wizard
        if (!wizardCompleted) {
            go(RegisterWizardActivity.class);
            return;
        }

        // Login obligatorio si no hay usuario
        if (user == null) {
            go(LoginActivity.class);
            return;
        }

        // Usuario logueado → comprobar si tiene coche
        FirebaseFirestore.getInstance()
                .collection("Coches")
                .whereArrayContains("Propietario", user.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        go(PrimerCocheActivity.class);
                    } else {
                        go(MainActivity.class);
                    }
                })
                .addOnFailureListener(e -> {
                    // En caso de fallo → al menos entrar a main
                    go(MainActivity.class);
                });
    }

    private void go(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
