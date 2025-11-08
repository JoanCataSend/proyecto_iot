package com.example.proyecto_iot;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.splashscreen.SplashScreen;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class SplashActivity extends AppCompatActivity {

    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SplashScreen.installSplashScreen(this);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        PlayerView playerView = findViewById(R.id.playerView);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Entrada suave al estar listo el primer frame
        playerView.setAlpha(0f);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    playerView.animate().alpha(1f).setDuration(120).start();
                }
            }
        });


        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.kova_animacion);

        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item);
        player.setRepeatMode(Player.REPEAT_MODE_ALL); // 🔁 bucle continuo
        player.setVolume(0f); // silencio
        player.prepare();
        player.play();

        // ⏱️ salir tras ~4 s (4 ciclos de un vídeo de 1 s)
        playerView.postDelayed(this::goNext, 4000);

        // tap para saltar
        playerView.setOnClickListener(v -> goNext());
    }

    private void goNext() {
        if (isFinishing()) return;
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) { player.pause(); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }
}
