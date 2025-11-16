package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

public class ConfigActivity extends NavBarActivity {

    LinearLayout opcionCuenta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);
        inicializarNavbar(R.id.nav_settings);

        opcionCuenta = findViewById(R.id.cuentayperfil);

        opcionCuenta.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigActivity.this, CuentaYPerfil.class);
            startActivity(intent);
        });

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
