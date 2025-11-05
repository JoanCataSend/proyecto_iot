package com.example.proyecto_iot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class Ubicacion extends NavBarActivity implements OnMapReadyCallback {

    // Desplegable ubicación
    BottomSheetBehavior<View> bottomSheetBehavior;

    // Mapa
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ubicacion);

        inicializarNavbar(R.id.nav_cars);

        // Ajuste de los márgenes del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ← Botón "Volver"
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        // ⚙️ Botón "Ajustes"
        ImageButton navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(Ubicacion.this, ConfigActivity.class))
            );
        }

        // Desplegable ubicación
        View panel = findViewById(R.id.panel_desplegable);
        bottomSheetBehavior = BottomSheetBehavior.from(panel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Ejemplo: marcador en Valencia
        LatLng coche = new LatLng(39.4699, -0.3763);
        mMap.addMarker(new MarkerOptions().position(coche).title("Mi coche"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coche, 14));

        // Mostrar ubicación del usuario si tiene permiso
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }
}
