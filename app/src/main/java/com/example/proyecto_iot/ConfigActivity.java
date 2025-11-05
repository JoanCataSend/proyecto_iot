package com.example.proyecto_iot;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ConfigActivity extends NavBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config); // usa tu layout config.xml
        inicializarNavbar(R.id.nav_settings);

        // (opcional) si quieres mostrar el botón de volver arriba
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.ajustes));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
