package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.intento);

        // Ajuste de los márgenes del sistema (status bar / nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Spinner de coches
        Spinner spinner = findViewById(R.id.spinnerCars);
        if (spinner != null) {
            String[] carList = getResources().getStringArray(R.array.lista_coches);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, carList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }

        // Botón "Cámaras"
        LinearLayout layoutCamaras = findViewById(R.id.layoutCamaras);
        if (layoutCamaras != null) {
            layoutCamaras.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, CamarasActivity.class))
            );
        }

        // --- Botones del navbar ---

            // Home
            ImageButton navHome = findViewById(R.id.nav_home);
            if (navHome != null) {
                navHome.setOnClickListener(v ->
                        startActivity(new Intent(MainActivity.this, MainActivity.class))
                );
            }

            // Coches y ubicación
            ImageButton navCars = findViewById(R.id.nav_cars);
            if (navCars != null) {
                navCars.setOnClickListener(v ->
                        startActivity(new Intent(MainActivity.this, Ubicacion.class))
                );
            }

            // Notificaciones
            ImageButton navNotificaciones = findViewById(R.id.nav_notifications);
            if (navNotificaciones != null) {
                navNotificaciones.setOnClickListener(v ->
                        startActivity(new Intent(MainActivity.this, ConfigActivity.class))
                );
            }

            // Configuración
            ImageButton navSettings = findViewById(R.id.nav_settings);
            if (navSettings != null) {
                navSettings.setOnClickListener(v ->
                        startActivity(new Intent(MainActivity.this, ConfigActivity.class))
                );
            }
    }
}
