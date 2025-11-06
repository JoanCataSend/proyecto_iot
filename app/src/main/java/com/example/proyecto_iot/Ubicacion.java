package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;

public class Ubicacion extends NavBarActivity implements OnMapReadyCallback {

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ubicacion);

        inicializarNavbar(R.id.nav_cars);

        // --- PANEL DESPLEGABLE ---
        View panel = findViewById(R.id.panel_desplegable);
        bottomSheetBehavior = BottomSheetBehavior.from(panel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setPeekHeight(50);

        // --- LISTA DE COCHES ---
        ListView listViewCoches = findViewById(R.id.listViewCoches);

        // Datos de ejemplo: nombre y ubicación
        ArrayList<Coche> listaCoches = new ArrayList<>();
        listaCoches.add(new Coche("Mi coche", "Calle Ejemplo 123"));
        listaCoches.add(new Coche("Coche rojo", "Av. Valencia 45"));
        listaCoches.add(new Coche("Coche azul", "Calle Mayor 12"));

        // Adapter personalizado
        CocheAdapter adapter = new CocheAdapter(listaCoches);
        listViewCoches.setAdapter(adapter);

        // --- BOTÓN VOLVER ---
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
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
    }

    // Clase para los datos de cada coche
    private static class Coche {
        String nombre;
        String ubicacion;

        Coche(String nombre, String ubicacion) {
            this.nombre = nombre;
            this.ubicacion = ubicacion;
        }
    }

    // Adapter para la lista de coches
    private class CocheAdapter extends BaseAdapter {
        private final ArrayList<Coche> coches;

        CocheAdapter(ArrayList<Coche> coches) {
            this.coches = coches;
        }

        @Override
        public int getCount() {
            return coches.size();
        }

        @Override
        public Object getItem(int position) {
            return coches.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(Ubicacion.this)
                        .inflate(R.layout.item_coche, parent, false);
            }

            Coche coche = coches.get(position);

            TextView nombre = convertView.findViewById(R.id.nombre_coche);
            TextView ubicacion = convertView.findViewById(R.id.ubicacion_coche);
            ImageButton btnEditar = convertView.findViewById(R.id.btn_editar_coche);

            nombre.setText(coche.nombre);
            ubicacion.setText(coche.ubicacion);

            // Click en el lápiz
            btnEditar.setOnClickListener(v -> {
                Intent intent = new Intent(Ubicacion.this, EditarCocheActivity.class);
                intent.putExtra("nombreCoche", coche.nombre);
                startActivity(intent);
            });

            return convertView;
        }
    }
}
