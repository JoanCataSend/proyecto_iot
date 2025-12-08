package com.example.proyecto_iot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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

        // Quitar flecha atrás
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        // PANEL DESPLEGABLE
        View panel = view.findViewById(R.id.panel_desplegable);
        bottomSheetBehavior = BottomSheetBehavior.from(panel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(120);

        // ALTURA DEL PANEL (40%)
        DisplayMetrics dm = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int maxHeight = (int) (dm.heightPixels * 0.60f);
        panel.getLayoutParams().height = maxHeight;

        // BOTÓN AÑADIR COCHE
        LinearLayout btnAnadirCoche = view.findViewById(R.id.btnAnadirCoche);
        btnAnadirCoche.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AnadirCoche.class);
            startActivity(intent);
        });

        // RECYCLER VIEW
        RecyclerView recycler = view.findViewById(R.id.recyclerCoches);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        CocheMapaAdapter adapter = new CocheMapaAdapter(listaCoches, this);
        recycler.setAdapter(adapter);

        // FIREBASE
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            db.collection("Coches")
                    .whereArrayContains("Propietario", user.getUid())
                    .get()
                    .addOnSuccessListener(query -> {

                        listaCoches.clear();

                        for (DocumentSnapshot doc : query.getDocuments()) {

                            Double lat = doc.getDouble("lat");
                            Double lng = doc.getDouble("lng");
                            String fotoUrl = doc.getString("Foto"); // <-- nombre correcto

                            listaCoches.add(new CocheMapa(
                                    doc.getId(),
                                    doc.getString("Nombre"),
                                    doc.getString("Marca"),
                                    doc.getString("Modelo"),
                                    doc.getString("Matrícula"),
                                    lat != null ? lat : 0.0,
                                    lng != null ? lng : 0.0,
                                    fotoUrl,
                                    "" // dirección opcional
                            ));
                        }

                        adapter.notifyDataSetChanged();
                        actualizarMarcadores();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Error al cargar coches: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        }

        // MAPA
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) mapFragment.getMapAsync(this);

        solicitarPermisoUbicacionSiEsNecesario();

        return view;
    }

    private void solicitarPermisoUbicacionSiEsNecesario() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

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

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    // -------------------------
    // MARCADORES EN EL MAPA
    // -------------------------
    private void actualizarMarcadores() {
        if (mMap == null) return;

        mMap.clear();

        for (CocheMapa coche : listaCoches) {

            if (coche.lat == 0.0 && coche.lng == 0.0) continue;

            LatLng posicion = new LatLng(coche.lat, coche.lng);

            if (coche.fotoUrl != null && !coche.fotoUrl.isEmpty()) {
                cargarIconoPersonalizado(posicion, coche.fotoUrl, coche.nombre);
            } else {
                mMap.addMarker(new MarkerOptions()
                        .position(posicion)
                        .title(coche.nombre));
            }
        }

        // Centrar en el primer coche válido
        for (CocheMapa coche : listaCoches) {
            if (coche.lat != 0 && coche.lng != 0) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(coche.lat, coche.lng), 14f
                ));
                break;
            }
        }
    }

    // -------------------------
    // Cargar imagen SIN recorte
    // -------------------------
    private void cargarIconoPersonalizado(LatLng pos, String url, String nombre) {

        Picasso.get()
                .load(url)
                .resize(100, 100)        // tamaño del icono
                .centerInside()
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                        Bitmap finalBitmap = agregarPadding(bitmap, 10);

                        mMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(nombre)
                                .icon(BitmapDescriptorFactory.fromBitmap(finalBitmap)));
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        mMap.addMarker(new MarkerOptions().position(pos).title(nombre));
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {}
                });
    }

    // Añade un pequeño margen al icono
    private Bitmap agregarPadding(Bitmap bmp, int padding) {

        Bitmap output = Bitmap.createBitmap(
                bmp.getWidth() + padding * 2,
                bmp.getHeight() + padding * 2,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(output);
        canvas.drawBitmap(bmp, padding, padding, null);

        return output;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        actualizarMarcadores();
        habilitarMiUbicacionEnMapa();
    }

    // -------------------------
    // MODELO CocheMapa
    // -------------------------
    public static class CocheMapa {
        public String id, nombre, marca, modelo, matricula, fotoUrl, direccion;
        public double lat, lng;

        public CocheMapa(String id, String nombre, String marca, String modelo,
                         String matricula, double lat, double lng,
                         String fotoUrl, String direccion) {

            this.id = id;
            this.nombre = nombre;
            this.marca = marca;
            this.modelo = modelo;
            this.matricula = matricula;
            this.lat = lat;
            this.lng = lng;
            this.fotoUrl = fotoUrl;
            this.direccion = direccion;
        }
    }
}
