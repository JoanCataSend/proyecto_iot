package com.example.proyecto_iot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ubicacion);

        // Inicializar navbar
        inicializarNavbar(R.id.nav_cars);

        // Ajuste de los márgenes del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- PANEL DESPLEGABLE ---
        View panel = findViewById(R.id.panel_desplegable);
        bottomSheetBehavior = BottomSheetBehavior.from(panel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // empieza cerrado
        bottomSheetBehavior.setPeekHeight(50); // altura visible del “asa”

        // ListView de coches
        ListView listViewCoches = findViewById(R.id.listViewCoches);
        String[] coches = getResources().getStringArray(R.array.lista_coches);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, coches);
        listViewCoches.setAdapter(adapter);

        // Clics en coches
        listViewCoches.setOnItemClickListener((parent, view, position, id) -> {
            String cocheSeleccionado = coches[position];
            Toast.makeText(this, "Has pulsado: " + cocheSeleccionado, Toast.LENGTH_SHORT).show();
        });

        // --- BOTÓN VOLVER ---
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        // --- BOTÓN AJUSTES ---
        ImageButton navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(Ubicacion.this, ConfigActivity.class))
            );
        }

        // --- MAPA ---
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng coche = new LatLng(39.4699, -0.3763);
        mMap.addMarker(new MarkerOptions().position(coche).title("Mi coche"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coche, 14));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }
}
