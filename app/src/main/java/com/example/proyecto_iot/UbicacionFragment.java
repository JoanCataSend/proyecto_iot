package com.example.proyecto_iot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UbicacionFragment extends Fragment implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private GoogleMap mMap;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private final ArrayList<CocheMapa> listaCoches = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // 🔐 Guardar targets Glide para poder cancelarlos
    private final List<CustomTarget<Bitmap>> glideTargets = new ArrayList<>();

    // =========================
    //   GETTER PARA EL ADAPTER
    // =========================
    public GoogleMap getMapa() {
        return mMap;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ubicacion, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        View panel = view.findViewById(R.id.panel_desplegable);
        bottomSheetBehavior = BottomSheetBehavior.from(panel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(120);

        DisplayMetrics dm = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        panel.getLayoutParams().height = (int) (dm.heightPixels * 0.6f);

        view.findViewById(R.id.btnAnadirCoche)
                .setOnClickListener(v ->
                        startActivity(new Intent(getActivity(), AnadirCoche.class)));

        RecyclerView recycler = view.findViewById(R.id.recyclerCoches);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        CocheMapaAdapter adapter = new CocheMapaAdapter(listaCoches, this);
        recycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            db.collection("Coches")
                    .whereArrayContains("Propietario", user.getUid())
                    .get()
                    .addOnSuccessListener(query -> {

                        if (!isAdded()) return;

                        listaCoches.clear();

                        for (DocumentSnapshot doc : query) {

                            Double lat = doc.getDouble("lat");
                            Double lng = doc.getDouble("lng");

                            String direccion = "";
                            if (lat != null && lng != null && lat != 0 && lng != 0) {
                                direccion = obtenerDireccion(lat, lng);
                            }

                            listaCoches.add(new CocheMapa(
                                    doc.getId(),
                                    doc.getString("Nombre"),
                                    doc.getString("Marca"),
                                    doc.getString("Modelo"),
                                    doc.getString("Matrícula"),
                                    lat != null ? lat : 0,
                                    lng != null ? lng : 0,
                                    doc.getString("Foto"),
                                    direccion
                            ));
                        }

                        adapter.notifyDataSetChanged();
                        actualizarMarcadores();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(),
                                e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        solicitarPermisoUbicacionSiEsNecesario();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        for (CustomTarget<Bitmap> t : glideTargets) {
            Glide.with(this).clear(t);
        }
        glideTargets.clear();

        mMap = null;
    }

    private String obtenerDireccion(double lat, double lng) {
        if (!isAdded()) return "Ubicación desconocida";

        try {
            Geocoder g = new Geocoder(getContext(), Locale.getDefault());
            List<Address> list = g.getFromLocation(lat, lng, 1);
            if (!list.isEmpty()) return list.get(0).getAddressLine(0);
        } catch (Exception ignored) {}
        return "Ubicación desconocida";
    }

    private void actualizarMarcadores() {
        if (mMap == null || listaCoches.isEmpty()) return;

        mMap.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hayPosicionesValidas = false;

        for (CocheMapa c : listaCoches) {
            if (c.lat == 0 || c.lng == 0) continue;

            LatLng pos = new LatLng(c.lat, c.lng);
            hayPosicionesValidas = true;
            boundsBuilder.include(pos);

            if (c.fotoUrl != null && !c.fotoUrl.isEmpty()) {
                cargarIconoPersonalizado(pos, c.fotoUrl, c.nombre);
            } else {
                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(c.nombre));
            }
        }

        if (!hayPosicionesValidas) return;

        LatLngBounds bounds = boundsBuilder.build();

        // MUY IMPORTANTE: esperar a que el mapa esté renderizado
        mMap.setOnMapLoadedCallback(() ->
                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, 120)
                )
        );
    }

    private void cargarIconoPersonalizado(LatLng pos, String url, String nombre) {
        if (!isAdded() || mMap == null) return;

        CustomTarget<Bitmap> target = new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap,
                                        @Nullable Transition<? super Bitmap> transition) {

                if (!isAdded() || mMap == null) return;

                Bitmap out = Bitmap.createBitmap(
                        bitmap.getWidth() + 20,
                        bitmap.getHeight() + 20,
                        Bitmap.Config.ARGB_8888
                );
                Canvas c = new Canvas(out);
                c.drawBitmap(bitmap, 10, 10, null);

                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(nombre)
                        .icon(BitmapDescriptorFactory.fromBitmap(out)));
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {}
        };

        glideTargets.add(target);

        Glide.with(this)
                .asBitmap()
                .load(url)
                .override(120, 120)
                .centerInside()
                .into(target);
    }

    private void solicitarPermisoUbicacionSiEsNecesario() {
        if (!isAdded()) return;

        if (ContextCompat.checkSelfPermission(
                getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        actualizarMarcadores();
    }

    // =========================
    //      MODELO
    // =========================
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
