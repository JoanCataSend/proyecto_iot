package com.example.proyecto_iot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UbicacionFragment extends Fragment implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "UBICACION_FRAGMENT";

    private GoogleMap mMap;
    public GoogleMap getMapa() { return mMap; }

    private BottomSheetBehavior<View> bottomSheetBehavior;

    private final ArrayList<CocheMapa> listaCoches = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration cochesListener;

    // ===== GPS =====
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // ⚠️ COCHE QUE ESTE DISPOSITIVO CONTROLA
    private String cocheIdActual = "ZkB10ikraHvc11ig0vD0";

    private final List<CustomTarget<Bitmap>> glideTargets = new ArrayList<>();

    // =========================
    //        UI
    // =========================
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

        // 🔥 ESCUCHA EN TIEMPO REAL (CLAVE)
        if (user != null) {
            cochesListener = db.collection("Coches")
                    .whereArrayContains("Propietario", user.getUid())
                    .addSnapshotListener((snapshots, error) -> {

                        if (error != null || snapshots == null || !isAdded()) return;

                        listaCoches.clear();

                        for (DocumentSnapshot doc : snapshots) {
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
                    });
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        solicitarPermisoUbicacionSiEsNecesario();
        inicializarLocalizacion();

        return view;
    }

    // =========================
    //        GPS
    // =========================
    private void inicializarLocalizacion() {

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireActivity());

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(15000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {

                Location loc = result.getLastLocation();
                if (loc == null) return;

                if (loc.getAccuracy() > 1000) return;

                guardarUbicacionEnFirebase(
                        loc.getLatitude(),
                        loc.getLongitude()
                );
            }
        };
    }

    private void guardarUbicacionEnFirebase(double lat, double lng) {

        Map<String, Object> data = new HashMap<>();
        data.put("lat", lat);
        data.put("lng", lng);
        data.put("lastUpdate", FieldValue.serverTimestamp());

        db.collection("Coches")
                .document(cocheIdActual)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "✔ Firebase actualizado: " + lat + ", " + lng))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error Firebase", e));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // =========================
    //        MAPA
    // =========================
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        actualizarMarcadores();
    }

    private void actualizarMarcadores() {
        if (mMap == null || listaCoches.isEmpty()) return;

        mMap.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hay = false;

        for (CocheMapa c : listaCoches) {
            if (c.lat == 0 || c.lng == 0) continue;

            LatLng pos = new LatLng(c.lat, c.lng);
            hay = true;
            builder.include(pos);

            if (c.fotoUrl != null && !c.fotoUrl.isEmpty()) {
                cargarIconoPersonalizado(pos, c.fotoUrl, c.nombre);
            } else {
                mMap.addMarker(new MarkerOptions().position(pos).title(c.nombre));
            }
        }

        if (!hay) return;

        mMap.setOnMapLoadedCallback(() ->
                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                                builder.build(), 120)));
    }

    // =========================
    //        UTILS
    // =========================
    private String obtenerDireccion(double lat, double lng) {
        try {
            Geocoder g = new Geocoder(getContext(), Locale.getDefault());
            List<Address> list = g.getFromLocation(lat, lng, 1);
            if (!list.isEmpty()) return list.get(0).getAddressLine(0);
        } catch (Exception ignored) {}
        return "Ubicación desconocida";
    }

    private void cargarIconoPersonalizado(LatLng pos, String url, String nombre) {

        CustomTarget<Bitmap> target = new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap,
                                        @Nullable Transition<? super Bitmap> transition) {

                Bitmap out = Bitmap.createBitmap(
                        bitmap.getWidth() + 20,
                        bitmap.getHeight() + 20,
                        Bitmap.Config.ARGB_8888);

                new Canvas(out).drawBitmap(bitmap, 10, 10, null);

                if (mMap != null) {
                    mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(nombre)
                            .icon(BitmapDescriptorFactory.fromBitmap(out)));
                }
            }

            @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
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
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cochesListener != null) cochesListener.remove();
        for (CustomTarget<Bitmap> t : glideTargets) {
            Glide.with(this).clear(t);
        }
        glideTargets.clear();
        mMap = null;
    }

    // =========================
    //        MODELO
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
