package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_config);

        // ========== HEADER ==========
        TextView tvHeader = findViewById(R.id.tvHeaderTitle);
        if (tvHeader != null) {
            tvHeader.setText(getString(R.string.ajustes)); // título "Ajustes"
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish()); // botón atrás
        }

        // ========== NAVBAR ==========
        ImageButton navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v ->
                    startActivity(new Intent(this, MainActivity.class))
            );
        }

        ImageButton navCar = findViewById(R.id.nav_car);
        if (navCar != null) {
            navCar.setOnClickListener(v ->
                    startActivity(new Intent(this, RegistroCoche.class))
            );
        }

        ImageButton navNotif = findViewById(R.id.nav_notifications);
        if (navNotif != null) {
            navNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, ConfigActivity.class)) // cambia si es otra activity
            );
        }

        ImageButton navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                // Ya estás en Ajustes → no hace nada
            });
        }
    }
}
