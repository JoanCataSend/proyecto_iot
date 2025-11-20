package com.example.proyecto_iot;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class IntentoFragment extends Fragment {

    private static final String CHANNEL_ID = "alertas_vehiculo";
    private static final int NOTIF_PUERTA = 1001;
    private static final int NOTIF_IMPACTO = 1002;

    private static final String PREFS_NAME = "notificaciones_prefs";
    private static final String PREF_KEY_LOCKED = "lock_state_locked";
    private static final String PREF_KEY_SELECTED_CAR = "vehiculo_seleccionado";

    private final List<String> carNames = new ArrayList<>();
    private final List<String> carIds   = new ArrayList<>();

    // UI
    private Spinner spinnerCars;
    private ImageView ivCar;
    private FrameLayout flLock;
    private ImageView ivLock;
    private TextView tvLockState;

    // Estado
    private boolean isLocked = true;
    private int currentCarIndex = 0;

    private String ultimaPuerta = null;
    private boolean ultimoImpacto = false;

    private FirebaseFirestore firestore;
    private ListenerRegistration estadoListener;

    private Vibrator vibrator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        firestore = FirebaseFirestore.getInstance();
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        spinnerCars = view.findViewById(R.id.spinnerCars);
        ivCar       = view.findViewById(R.id.ivCar);
        flLock      = view.findViewById(R.id.flLock);
        ivLock      = view.findViewById(R.id.ivLock);
        tvLockState = view.findViewById(R.id.tvLockState);

        ArrayAdapter<String> carsAdapter =
                new ArrayAdapter<>(context, R.layout.spinner_coches, carNames);

        spinnerCars.setAdapter(carsAdapter);

        int savedPosition = prefs.getInt(PREF_KEY_SELECTED_CAR, 0);
        currentCarIndex = savedPosition;

        // ====== CLICK EN CANDADO (APP → FIREBASE → RASPI) ======
        flLock.setOnClickListener(v -> toggleLock(prefs));
        ivLock.setOnClickListener(v -> toggleLock(prefs));

        // ====== SPINNER ======
        spinnerCars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

                currentCarIndex = position;
                prefs.edit().putInt(PREF_KEY_SELECTED_CAR, position).apply();

                updateCarImage(position);

                String cocheIdReal = carIds.get(position);
                Log.d("INTENTO", "Car seleccionado ID REAL = " + cocheIdReal);

                listenEstadoActual(cocheIdReal);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadCarsFromFirestore(savedPosition, carsAdapter);
    }

    // =========================
    //      CARGAR COCHES
    // =========================
    private void loadCarsFromFirestore(int savedPosition, ArrayAdapter<String> adapter) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("Coches")
                .whereArrayContains("Propietario", uid)
                .get()
                .addOnSuccessListener(query -> {

                    carNames.clear();
                    carIds.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        carNames.add(doc.getString("Nombre"));
                        carIds.add(doc.getId());
                    }

                    adapter.notifyDataSetChanged();

                    int pos = Math.min(savedPosition, carNames.size() - 1);

                    spinnerCars.setSelection(pos);
                    listenEstadoActual(carIds.get(pos));
                });
    }

    // =========================
    //    ESCUCHAR ESTADO REAL
    // =========================
    private void listenEstadoActual(String carId) {

        if (estadoListener != null) estadoListener.remove();

        estadoListener = firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .addSnapshotListener((doc, error) -> {

                    if (error != null || doc == null || !doc.exists()) return;

                    Log.d("INTENTO", "Estado recibido: " + doc.getData());

                    String puerta = doc.getString("puerta");
                    Boolean impacto = doc.getBoolean("impacto");

                    // ===== PUERTA =====
                    if (puerta != null && !puerta.equals(ultimaPuerta)) {

                        ultimaPuerta = puerta;

                        if (puerta.equals("open")) {
                            isLocked = false;
                            updateLockUi();
                            mostrarNotifPuertaAbierta();
                        } else {
                            isLocked = true;
                            updateLockUi();
                            mostrarNotifPuertaCerrada();
                        }
                    }

                    // ===== IMPACTO =====
                    boolean hayImpacto = impacto != null && impacto;

                    if (hayImpacto && !ultimoImpacto) {
                        ultimoImpacto = true;
                        manejarImpacto();
                    } else if (!hayImpacto) {
                        ultimoImpacto = false;
                    }
                });
    }

    // =========================
    //  APP → FIREBASE → RASPI
    // =========================
    private void toggleLock(SharedPreferences prefs) {

        isLocked = !isLocked;

        // Guardar local
        prefs.edit().putBoolean(getLockPrefKeyForIndex(currentCarIndex), isLocked).apply();

        // Actualizar UI
        updateLockUi();

        // Actualizar Firestore
        String carId = carIds.get(currentCarIndex);
        String nuevoEstado = isLocked ? "closed" : "open";

        firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .update("puerta", nuevoEstado)
                .addOnSuccessListener(a ->
                        Log.d("INTENTO", "🔥 Puerta actualizada en Firebase → " + nuevoEstado)
                )
                .addOnFailureListener(e ->
                        Log.e("INTENTO", "❌ Error actualizando puerta", e)
                );
    }

    // =========================
    //     NOTIFICACIONES
    // =========================
    private void mostrarNotifPuertaAbierta() {
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_info)
                        .setContentTitle("Puerta abierta")
                        .setContentText("La puerta se ha abierto (RFID / App)")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat.from(requireContext()).notify(NOTIF_PUERTA, b.build());
    }

    private void mostrarNotifPuertaCerrada() {
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_info)
                        .setContentTitle("Puerta cerrada")
                        .setContentText("La puerta se ha cerrado")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat.from(requireContext()).notify(NOTIF_PUERTA + 1, b.build());
    }

    private void manejarImpacto() {

        // Vibración potente
        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE));
            } else vibrator.vibrate(700);
        }

        // Notificación
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_info)
                        .setContentTitle("Impacto detectado")
                        .setContentText("Tu vehículo ha recibido un impacto")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat.from(requireContext()).notify(NOTIF_IMPACTO, b.build());

        // Abrir cámara
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CameraFragment())
                .addToBackStack(null)
                .commit();
    }

    // =========================
    //          UI
    // =========================
    private void updateCarImage(int position) {
        ivCar.setImageResource(R.drawable.coche_naranja);
    }

    private void updateLockUi() {

        if (isLocked) {
            ivLock.setImageResource(R.drawable.ic_candado_cerrado);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.error));
            tvLockState.setText("Bloqueado");
            tvLockState.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
        } else {
            ivLock.setImageResource(R.drawable.ic_candado_abierto);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.verdeoscuro));
            tvLockState.setText("Puertas abiertas");
            tvLockState.setTextColor(ContextCompat.getColor(requireContext(), R.color.verdeoscuro));
        }
    }

    // =========================
    //  NOTIFICATION CHANNEL
    // =========================
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c =
                    new NotificationChannel(CHANNEL_ID, "Alertas vehículo",
                            NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = requireContext().getSystemService(NotificationManager.class);
            nm.createNotificationChannel(c);
        }
    }

    private String getLockPrefKeyForIndex(int index) {
        return PREF_KEY_LOCKED + "_" + index;
    }
}
