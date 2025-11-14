package com.example.proyecto_iot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class Ubicacion extends NavBarActivity implements OnMapReadyCallback {

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private GoogleMap mMap;
    private ArrayList<Coche> listaCoches = new ArrayList<>();

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

        // Adapter personalizado
        CocheAdapter adapter = new CocheAdapter(listaCoches);
        listViewCoches.setAdapter(adapter);

        // BBDD
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("Usuarios").document(uid).collection("Coches")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listaCoches.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        listaCoches.add(new Coche(
                                doc.getId(),
                                doc.getString("nombre"),
                                doc.getString("marca"),
                                doc.getString("modelo"),
                                doc.getString("matricula"),
                                doc.getDouble("ubicacionLat") != null ? doc.getDouble("ubicacionLat") : 0,
                                doc.getDouble("ubicacionLng") != null ? doc.getDouble("ubicacionLng") : 0,
                                doc.getString("direccion")
                        ));
                    }
                    adapter.notifyDataSetChanged();

                    // añadir marcadores si el mapa ya está listo
                    if (mMap != null) actualizarMarcadores();
                });

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

    private void actualizarMarcadores() {
        if (mMap == null) return; // por si el mapa aún no está listo
        mMap.clear(); // borra marcadores antiguos
        for (Coche coche : listaCoches) {
            LatLng pos = new LatLng(coche.lat, coche.lng);
            mMap.addMarker(new MarkerOptions().position(pos).title(coche.nombre));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        actualizarMarcadores();

        LatLng coche = new LatLng(39.4699, -0.3763);
        mMap.addMarker(new MarkerOptions().position(coche).title("Mi coche"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coche, 14));

        // Para ver la ubicación del usuario
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

    }

    // Clase para los datos de cada coche
    private static class Coche {
        String id, nombre, marca, modelo, matricula, direccion;
        double lat, lng;

        // Constructor
        Coche(String id, String nombre, String marca, String modelo, String matricula, double lat, double lng, String direccion) {
            this.id = id;
            this.nombre = nombre;
            this.marca = marca;
            this.modelo = modelo;
            this.matricula = matricula;
            this.lat = lat;
            this.lng = lng;
            this.direccion = direccion;
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
            ubicacion.setText(coche.direccion);

            // Click en el lápiz
            btnEditar.setOnClickListener(v -> {
                Intent intent = new Intent(Ubicacion.this, EditarCocheActivity.class);
                intent.putExtra("cocheId", coche.id);
                intent.putExtra("nombreCoche", coche.nombre);
                intent.putExtra("marcaCoche", coche.marca);
                intent.putExtra("modeloCoche", coche.modelo);
                intent.putExtra("matriculaCoche", coche.matricula);
                startActivity(intent);
            });

            return convertView;
        }
    }


    // Permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    try {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            mMap.setMyLocationEnabled(true);
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "No se pudo activar la ubicación", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
