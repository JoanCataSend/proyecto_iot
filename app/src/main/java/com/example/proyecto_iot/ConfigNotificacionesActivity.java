package com.example.proyecto_iot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton; // TU CAMBIO AQUÍ: botón guardar
import com.google.android.material.materialswitch.MaterialSwitch; // TU CAMBIO AQUÍ: switches de notificaciones

public class ConfigNotificacionesActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "notificaciones_prefs";
    private MaterialSwitch swVentanaRota;
    private MaterialSwitch swPuertasAbiertas;
    private MaterialSwitch swCamara;
    private MaterialSwitch swMovimiento;
    private MaterialSwitch swSonido;
    private MaterialSwitch swUbicacion;
    private MaterialButton btnGuardarNotificaciones;
    private boolean originalVentanaRota;
    private boolean originalPuertasAbiertas;
    private boolean originalCamara;
    private boolean originalMovimiento;
    private boolean originalSonido;
    private boolean originalUbicacion;
    private boolean originalApertura;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_config_notificaciones);

        // TU CAMBIO AQUÍ: header (título + flecha atrás)
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(R.string.title_notificaciones);
        }

        setupNavbar();

        swVentanaRota = findViewById(R.id.swVentanaRota);
        swPuertasAbiertas = findViewById(R.id.swPuertasAbiertas);
        swCamara = findViewById(R.id.swCamara);
        swMovimiento = findViewById(R.id.swMovimiento);
        swSonido = findViewById(R.id.swSonido);
        swUbicacion = findViewById(R.id.swUbicacion);

        btnGuardarNotificaciones = findViewById(R.id.btnGuardarNotificaciones);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (swVentanaRota != null) {
            originalVentanaRota = prefs.getBoolean("swVentanaRota", true);
            swVentanaRota.setChecked(originalVentanaRota);
            swVentanaRota.setOnCheckedChangeListener((buttonView, isChecked) -> updateGuardarButtonState());
        }

        if (swPuertasAbiertas != null) {
            originalPuertasAbiertas = prefs.getBoolean("swPuertasAbiertas", true);
            swPuertasAbiertas.setChecked(originalPuertasAbiertas);
            swPuertasAbiertas.setOnCheckedChangeListener((buttonView, isChecked) -> updateGuardarButtonState());
        }

        if (swCamara != null) {
            originalCamara = prefs.getBoolean("swCamara", true);
            swCamara.setChecked(originalCamara);
            swCamara.setOnCheckedChangeListener((buttonView, isChecked) -> updateGuardarButtonState());
        }

        if (swMovimiento != null) {
            originalMovimiento = prefs.getBoolean("swMovimiento", true);
            swMovimiento.setChecked(originalMovimiento);
            swMovimiento.setOnCheckedChangeListener((buttonView, isChecked) -> updateGuardarButtonState());
        }

        if (swSonido != null) {
            originalSonido = prefs.getBoolean("swSonido", true);
            swSonido.setChecked(originalSonido);
            swSonido.setOnCheckedChangeListener((buttonView, isChecked) -> updateGuardarButtonState());
        }

        if (swUbicacion != null) {
            originalUbicacion = prefs.getBoolean("swUbicacion", true);
            swUbicacion.setChecked(originalUbicacion);
            swUbicacion.setOnCheckedChangeListener((buttonView, isChecked) -> updateGuardarButtonState());
        }

        if (btnGuardarNotificaciones != null) {
            btnGuardarNotificaciones.setEnabled(false);
            btnGuardarNotificaciones.setAlpha(0.5f);

            btnGuardarNotificaciones.setOnClickListener(v -> {
                SharedPreferences.Editor editor = prefs.edit();

                if (swVentanaRota != null) {
                    editor.putBoolean("swVentanaRota", swVentanaRota.isChecked());
                    originalVentanaRota = swVentanaRota.isChecked();
                }

                if (swPuertasAbiertas != null) {
                    editor.putBoolean("swPuertasAbiertas", swPuertasAbiertas.isChecked());
                    originalPuertasAbiertas = swPuertasAbiertas.isChecked();
                }

                if (swCamara != null) {
                    editor.putBoolean("swCamara", swCamara.isChecked());
                    originalCamara = swCamara.isChecked();
                }

                if (swMovimiento != null) {
                    editor.putBoolean("swMovimiento", swMovimiento.isChecked());
                    originalMovimiento = swMovimiento.isChecked();
                }

                if (swSonido != null) {
                    editor.putBoolean("swSonido", swSonido.isChecked());
                    originalSonido = swSonido.isChecked();
                }

                if (swUbicacion != null) {
                    editor.putBoolean("swUbicacion", swUbicacion.isChecked());
                    originalUbicacion = swUbicacion.isChecked();
                }
                editor.apply();

                updateGuardarButtonState();
            });
        }

        updateGuardarButtonState();
    }
    private void updateGuardarButtonState() {
        if (btnGuardarNotificaciones == null) return;

        boolean hayCambios = false;

        if (swVentanaRota != null && swVentanaRota.isChecked() != originalVentanaRota) {
            hayCambios = true;
        }
        if (swPuertasAbiertas != null && swPuertasAbiertas.isChecked() != originalPuertasAbiertas) {
            hayCambios = true;
        }
        if (swCamara != null && swCamara.isChecked() != originalCamara) {
            hayCambios = true;
        }
        if (swMovimiento != null && swMovimiento.isChecked() != originalMovimiento) {
            hayCambios = true;
        }
        if (swSonido != null && swSonido.isChecked() != originalSonido) {
            hayCambios = true;
        }
        if (swUbicacion != null && swUbicacion.isChecked() != originalUbicacion) {
            hayCambios = true;
        }
        btnGuardarNotificaciones.setEnabled(hayCambios);
        btnGuardarNotificaciones.setAlpha(hayCambios ? 1f : 0.5f);
    }
    private void setupNavbar() {
        ImageButton navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("openFragment", "intento");
                startActivity(i);
            });
        }

        ImageButton navCar = findViewById(R.id.nav_car);
        if (navCar != null) {
            navCar.setOnClickListener(v -> {
                startActivity(new Intent(this, RegistroCoche.class));
            });
        }

        ImageButton navNotif = findViewById(R.id.nav_notifications);
        if (navNotif != null) {
            navNotif.setOnClickListener(v -> {
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
            });
        }

        ImageButton navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, ConfigActivity.class));
            });
        }
    }
}
