package com.example.proyecto_iot;

import android.content.Intent;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public abstract class NavBarActivity extends AppCompatActivity {

    protected void inicializarNavbar(int idActivo) {
        ImageButton navHome = findViewById(R.id.nav_home);
        ImageButton navCars = findViewById(R.id.nav_cars);
        ImageButton navNotifications = findViewById(R.id.nav_notifications);
        ImageButton navSettings = findViewById(R.id.nav_settings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (!(this instanceof MainActivity))
                    startActivity(new Intent(this, MainActivity.class));
            });
        }

        if (navCars != null) {
            navCars.setOnClickListener(v -> {
                if (!(this instanceof Ubicacion))
                    startActivity(new Intent(this, Ubicacion.class));
            });
        }

        if (navNotifications != null) {
            navNotifications.setOnClickListener(v -> {
                if (!(this instanceof ConfigActivity))
                    startActivity(new Intent(this, ConfigActivity.class));
            });
        }

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                if (!(this instanceof ConfigActivity))
                    startActivity(new Intent(this, ConfigActivity.class));
            });
        }

        marcarBotonActivo(idActivo);
    }

    private void marcarBotonActivo(int idActivo) {
        ImageButton navHome = findViewById(R.id.nav_home);
        ImageButton navCars = findViewById(R.id.nav_cars);
        ImageButton navNotifications = findViewById(R.id.nav_notifications);
        ImageButton navSettings = findViewById(R.id.nav_settings);

        if (navHome != null) navHome.setSelected(idActivo == R.id.nav_home);
        if (navCars != null) navCars.setSelected(idActivo == R.id.nav_cars);
        if (navNotifications != null) navNotifications.setSelected(idActivo == R.id.nav_notifications);
        if (navSettings != null) navSettings.setSelected(idActivo == R.id.nav_settings);
    }
}

