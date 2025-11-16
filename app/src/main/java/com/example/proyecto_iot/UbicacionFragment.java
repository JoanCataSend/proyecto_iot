package com.example.proyecto_iot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class UbicacionFragment extends Fragment implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private GoogleMap mMap;

    private final ArrayList<CocheMapa> listaCoches = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ubicacion, container, false);

        // Quitar flecha atrás en este fragment
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        // --- PANEL DESPLEGABLE ---
        View panel = view.findViewById(R.id.panel_desplegable);
        bottomSheetBehavior = BottomSheetBehavior.from(panel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(120);

        // Limitar altura máxima del panel
        DisplayMetrics dm = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenHeight = dm.heightPixels;

        int maxHeight = (int) (screenHeight * 0.40);
        panel.getLayoutParams().height = maxHeight;
        panel.requestLayout();

        // --- LISTA DE COCHES ---
        ListView listViewCoches = view.findViewById(R.id.listViewCoches);
        CocheAdapter adapter = new CocheAdapter(listaCoches);
        listViewCoches.setAdapter(adapter);

        // --- FIREBASE ---
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            db.collection("Coches")
                    .whereArrayContains("Propietario", uid)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        listaCoches.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            listaCoches.add(new CocheMapa(
                                    doc.getId(),
                                    doc.getString("Nombre"),
                                    doc.getString("Marca"),
                                    doc.getString("Modelo"),
                                    doc.getString("Matrícula"),
                                    0,
                                    0,
                                    ""
                            ));
                        }
                        adapter.notifyDataSetChanged();
                        actualizarMarcadores();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Error al cargar coches: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(requireContext(),
                    "Usuario no autenticado",
                    Toast.LENGTH_SHORT).show();
        }

        // --- MAPA ---
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        solicitarPermisoUbicacionSiEsNecesario();

        return view;
    }

    private void solicitarPermisoUbicacionSiEsNecesario() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        } else {
            habilitarMiUbicacionEnMapa();
        }
    }

    private void habilitarMiUbicacionEnMapa() {
        if (mMap == null) return;

        try {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

                mMap.setMyLocationEnabled(true);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "No se pudo activar la ubicación",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarMarcadores() {
        if (mMap == null) return;

        mMap.clear();
        for (CocheMapa coche : listaCoches) {
            LatLng pos = new LatLng(coche.lat, coche.lng);
            mMap.addMarker(new MarkerOptions().position(pos).title(coche.nombre));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        actualizarMarcadores();

        LatLng coche = new LatLng(39.4699, -0.3763);
        mMap.addMarker(new MarkerOptions().position(coche).title("Mi coche"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coche, 14f));

        habilitarMiUbicacionEnMapa();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                habilitarMiUbicacionEnMapa();
            } else {
                Toast.makeText(requireContext(),
                        "Permiso de ubicación denegado",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------------- CocheMapa ----------------
    private static class CocheMapa {
        String id;
        String nombre;
        String marca;
        String modelo;
        String matricula;
        double lat;
        double lng;
        String direccion;

        CocheMapa(String id, String nombre, String marca, String modelo,
                  String matricula, double lat, double lng, String direccion) {
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

    // ---------------- Adapter ----------------
    private class CocheAdapter extends BaseAdapter {

        private final ArrayList<CocheMapa> coches;

        CocheAdapter(ArrayList<CocheMapa> coches) {
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
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_coche, parent, false);
            }

            CocheMapa coche = coches.get(position);

            TextView nombre = convertView.findViewById(R.id.nombre_coche);
            TextView ubicacion = convertView.findViewById(R.id.ubicacion_coche);
            ImageButton btnEditar = convertView.findViewById(R.id.btn_editar_coche);

            nombre.setText(coche.nombre);
            ubicacion.setText(coche.direccion != null ? coche.direccion : "");

            // ------------------- NAVEGACIÓN REAL A FRAGMENT -------------------
            btnEditar.setOnClickListener(v -> {

                EditarCocheFragment fragment = new EditarCocheFragment();

                Bundle args = new Bundle();
                args.putString("cocheId", coche.id);
                fragment.setArguments(args);

                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
            // ------------------------------------------------------------------

            return convertView;
        }
    }
}
