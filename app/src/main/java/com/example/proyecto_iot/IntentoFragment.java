package com.example.proyecto_iot;

import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IntentoFragment extends Fragment {

    // =========================
    //      CONSTANTES
    // =========================
    private static final String CHANNEL_ID = "alertas_vehiculo";

    private static final int NOTIFICATION_ID_PUERTAS = 1001;
    private static final int NOTIFICATION_ID_PUERTAS_CERRADAS = 1002;
    private static final int NOTIFICATION_ID_IMPACTO = 1003;
    private static final int NOTIFICATION_ID_SENALES = 1004;

    private static final String PREFS_NAME = "notificaciones_prefs";
    private static final String PREF_KEY_LOCKED = "lock_state_locked";
    private static final String PREF_KEY_SELECTED_CAR = "vehiculo_seleccionado";

    // Coches
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

    // Firebase estado
    private String ultimaPuerta = null;
    private boolean ultimoImpacto = false;

    private FirebaseFirestore firestore;
    private ListenerRegistration estadoListener;

    // Señales
    private ValueAnimator signalsBlinkAnimator;
    private Handler signalsStopHandler;
    private Runnable signalsStopRunnable;
    private Vibrator vibrator;

    private final List<String> popupItems = new ArrayList<>();
    private boolean spinnerInicializado = false;



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
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // ====== FIND VIEWS ======
        spinnerCars      = view.findViewById(R.id.spinnerCars);
        ivCar            = view.findViewById(R.id.ivCar);
        flLock           = view.findViewById(R.id.flLock);
        ivLock           = view.findViewById(R.id.ivLock);
        tvLockState      = view.findViewById(R.id.tvLockState);
        LinearLayout vehicleSelector = view.findViewById(R.id.vehicleSelector);
        LinearLayout layoutCamaras   = view.findViewById(R.id.layoutCamaras);
        LinearLayout addressPill     = view.findViewById(R.id.addressPill);
        ImageView ivSignals          = view.findViewById(R.id.ivSignals);
        FrameLayout flSignals        = (FrameLayout) ivSignals.getParent();

        // ====== SPINNER COCHES ======
        ArrayAdapter<String> carsAdapter =
                new ArrayAdapter<String>(context, R.layout.spinner_coches, carNames) {

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        return super.getView(currentCarIndex, convertView, parent);
                    }

                    @Override
                    public int getCount() {
                        return popupItems.size();
                    }

                    @Override
                    public String getItem(int position) {
                        return popupItems.get(position);
                    }
                };


        carsAdapter.setDropDownViewResource(R.layout.spinner_coches);
        spinnerCars.setAdapter(carsAdapter);

        spinnerCars.setPopupBackgroundDrawable(
                ContextCompat.getDrawable(context, R.drawable.bg_card_soft)
        );

        int savedPosition = prefs.getInt(PREF_KEY_SELECTED_CAR, 0);
        currentCarIndex = savedPosition;

        vehicleSelector.setOnClickListener(v -> spinnerCars.performClick());

        View.OnClickListener lockClickListener = v -> toggleLock(prefs);
        flLock.setOnClickListener(lockClickListener);
        ivLock.setOnClickListener(lockClickListener);

        spinnerCars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

                if (!spinnerInicializado) {
                    spinnerInicializado = true;
                    return;
                }

                if (popupItems.get(position).contains("Añadir")) {
                    startActivity(new Intent(requireContext(), PrimerCocheActivity.class));
                    spinnerCars.setSelection(currentCarIndex, false);
                    return;
                }

                if (position < 0 || position >= carNames.size()) return;

                currentCarIndex = position;
                prefs.edit().putInt(PREF_KEY_SELECTED_CAR, position).apply();

                updateCarImage(position);

                boolean savedLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
                isLocked = savedLocked;
                updateLockUi();

                if (position < carIds.size()) {
                    listenEstadoActual(carIds.get(position));
                }

                spinnerCars.post(() -> {
                    ((ArrayAdapter) spinnerCars.getAdapter()).notifyDataSetChanged();
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        boolean savedLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
        isLocked = savedLocked;
        updateLockUi();

        loadCarsFromFirestore(savedPosition, carsAdapter);

        layoutCamaras.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CameraFragment())
                        .addToBackStack(null)
                        .commit()
        );

        addressPill.setOnClickListener(v -> {
            double lat = 38.99614697675971;
            double lon = -0.16569078767633452;
            String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        });

        View.OnClickListener signalsClick = v ->
                handleSignalsClick(context, prefs, ivSignals);
        ivSignals.setOnClickListener(signalsClick);
        flSignals.setOnClickListener(signalsClick);
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (estadoListener != null) {
            estadoListener.remove();
            estadoListener = null;
        }

        stopSignalsEffects();
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
    }

    // =========================
    //      FIRESTORE COCHES
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
                    popupItems.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("Nombre");
                        if (nombre != null && !nombre.trim().isEmpty()) {
                            carNames.add(nombre.trim());
                            carIds.add(doc.getId());
                        }
                    }

                    popupItems.addAll(carNames);
                    popupItems.add("+ Añadir coche");

                    adapter.notifyDataSetChanged();

                    if (!carNames.isEmpty()) {
                        int pos = savedPosition;
                        if (pos < 0 || pos >= carNames.size()) pos = 0;
                        currentCarIndex = pos;
                        spinnerCars.setSelection(pos, false);
                        updateCarImage(pos); // cargar imagen precargada
                    }
                });
    }

    // =========================
    //    ESCUCHAR ESTADO REAL
    // =========================
    private void listenEstadoActual(String carId) {

        if (estadoListener != null) {
            estadoListener.remove();
            estadoListener = null;
        }

        estadoListener = firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .addSnapshotListener((doc, error) -> {

                    if (error != null || doc == null || !doc.exists()) return;

                    String puerta = doc.getString("puerta");
                    Boolean impacto = doc.getBoolean("impacto");

                    if (puerta != null && !puerta.equals(ultimaPuerta)) {

                        ultimaPuerta = puerta;

                        if ("open".equals(puerta)) {
                            isLocked = false;
                            updateLockUi();
                            guardarLockEnPrefs(false);
                            mostrarNotifPuertaAbierta();
                        } else {
                            isLocked = true;
                            updateLockUi();
                            guardarLockEnPrefs(true);
                            mostrarNotifPuertaCerrada();
                        }
                    }

                    boolean hayImpacto = impacto != null && impacto;

                    if (hayImpacto && !ultimoImpacto) {
                        ultimoImpacto = true;
                        verificarImpactoReciente(carId);
                    } else if (!hayImpacto) {
                        ultimoImpacto = false;
                    }

                });
    }

    private void verificarImpactoReciente(String carId) {

        firestore.collection("Coches")
                .document(carId)
                .collection("eventos")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) return;

                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snap.getDocuments().get(0);

                    String tipo = doc.getString("tipo");
                    Long ts = doc.getLong("timestamp");

                    if (tipo == null || ts == null) return;

                    long ahora = System.currentTimeMillis();

                    if (tipo.equals("impacto") && (ahora - ts) < 10_000) {
                        manejarImpacto();
                    }
                });
    }


    private void guardarLockEnPrefs(boolean locked) {
        Context ctx = requireContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(getLockPrefKeyForIndex(currentCarIndex), locked).apply();
    }

    // =========================
    //  APP → FIREBASE → RASPI
    // =========================
    private void toggleLock(SharedPreferences prefs) {

        if (currentCarIndex < 0 || currentCarIndex >= carIds.size()) {
            isLocked = !isLocked;
            updateLockUi();
            return;
        }

        isLocked = !isLocked;

        prefs.edit()
                .putBoolean(getLockPrefKeyForIndex(currentCarIndex), isLocked)
                .apply();

        updateLockUi();

        String carId = carIds.get(currentCarIndex);
        String nuevoEstado = isLocked ? "closed" : "open";

        firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .update("puerta", nuevoEstado);
    }

    // =========================
    //     NOTIFICACIONES
    // =========================
    private void mostrarNotifPuertaAbierta() {
        Context ctx = requireContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (!prefs.getBoolean("swPuertasAbiertas", true)) return;

        String titulo = getString(R.string.puertas_abiertas);
        String mensaje = "Las puertas del coche se han desbloqueado correctamente.";
        String fecha = getFechaActual();

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(ctx, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_info)
                        .setContentTitle(titulo)
                        .setContentText(mensaje)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID_PUERTAS, b.build());

        NotificacionRepository.getInstance().addNotificacion(
                new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info)
        );
    }

    private void mostrarNotifPuertaCerrada() {
        Context ctx = requireContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (!prefs.getBoolean("swPuertasAbiertas", true)) return;

        String titulo = getString(R.string.bloqueado);
        String mensaje = "Las puertas del coche se han bloqueado correctamente.";
        String fecha = getFechaActual();

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(ctx, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_info)
                        .setContentTitle(titulo)
                        .setContentText(mensaje)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID_PUERTAS_CERRADAS, b.build());

        NotificacionRepository.getInstance().addNotificacion(
                new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info)
        );
    }

    private void manejarImpacto() {

        Context ctx = requireContext();

        vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        700, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(700);
            }
        }

        String titulo = "Impacto detectado";
        String mensaje = "Tu vehículo ha recibido un impacto";
        String fecha = getFechaActual();

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(ctx, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_info)
                        .setContentTitle(titulo)
                        .setContentText(mensaje)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID_IMPACTO, b.build());

        NotificacionRepository.getInstance().addNotificacion(
                new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info)
        );

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CameraFragment())
                .addToBackStack(null)
                .commit();
    }

    // =========================
    //          SEÑALES
    // =========================
    private void handleSignalsClick(Context context,
                                    SharedPreferences prefs,
                                    ImageView ivSignals) {

        boolean sonidoEnabled = prefs.getBoolean("swSonido", true);

        ivSignals.setImageTintList(
                ContextCompat.getColorStateList(context, R.color.aviso));

        startColorBlinkAnimation(ivSignals, context);
        startVibrationPattern(context);
        updateFirestoreAlerts(true);

        if (sonidoEnabled) {
            String titulo = "Alertas activadas";
            String mensaje = "Se han activado las alertas luminosas y sonoras.";
            String fecha = getFechaActual();

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_info)
                            .setContentTitle(titulo)
                            .setContentText(mensaje)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true);

            NotificationManagerCompat.from(context)
                    .notify(NOTIFICATION_ID_SENALES, builder.build());

            NotificacionRepository.getInstance().addNotificacion(
                    new Notificacion(titulo, mensaje, fecha, R.drawable.ic_sonido)
            );
        }

        if (signalsStopHandler == null)
            signalsStopHandler = new Handler(Looper.getMainLooper());

        if (signalsStopRunnable != null)
            signalsStopHandler.removeCallbacks(signalsStopRunnable);

        signalsStopRunnable = () -> {
            stopBlinkAnimation();
            if (vibrator != null) vibrator.cancel();
            ivSignals.setColorFilter(
                    ContextCompat.getColor(context, R.color.texto_oscuro));
            updateFirestoreAlerts(false);
        };

        signalsStopHandler.postDelayed(signalsStopRunnable, 10_000);
    }

    private void startColorBlinkAnimation(ImageView target, Context context) {
        int color1 = ContextCompat.getColor(context, R.color.aviso);
        int color2 = ContextCompat.getColor(context, R.color.texto_oscuro);

        signalsBlinkAnimator = ValueAnimator.ofFloat(0f, 1f);
        signalsBlinkAnimator.setDuration(400);
        signalsBlinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        signalsBlinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        signalsBlinkAnimator.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            int blendedColor = blendColors(color1, color2, f);
            target.setColorFilter(blendedColor);
        });
        signalsBlinkAnimator.start();
    }

    private int blendColors(int from, int to, float ratio) {
        float inv = 1f - ratio;
        int r = Math.round(Color.red(from) * inv + Color.red(to) * ratio);
        int g = Math.round(Color.green(from) * inv + Color.green(to) * ratio);
        int b = Math.round(Color.blue(from) * inv + Color.blue(to) * ratio);
        return Color.rgb(r, g, b);
    }

    private void startVibrationPattern(Context context) {
        try {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null) return;

            long[] pattern = {0, 180, 220};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception ignored) { }
    }

    private void stopBlinkAnimation() {
        if (signalsBlinkAnimator != null) {
            signalsBlinkAnimator.cancel();
            signalsBlinkAnimator = null;
        }
    }

    private void stopSignalsEffects() {
        stopBlinkAnimation();
        if (signalsStopHandler != null && signalsStopRunnable != null) {
            signalsStopHandler.removeCallbacks(signalsStopRunnable);
            signalsStopRunnable = null;
        }
        if (vibrator != null) vibrator.cancel();
        vibrator = null;
    }

    private void updateFirestoreAlerts(boolean active) {
        if (firestore == null) return;
        if (currentCarIndex < 0 || currentCarIndex >= carIds.size()) return;

        String carId = carIds.get(currentCarIndex);

        long ts = System.currentTimeMillis();

        Map<String, Object> evento = new HashMap<>();
        evento.put("tipo", "alerta_sonido");
        evento.put("activar", active ? 1 : 0);
        evento.put("timestamp", ts);

        firestore.collection("Coches")
                .document(carId)
                .collection("eventos")
                .add(evento);

        firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .update("alertaSonido", active);
    }


    // =========================
    //          UI (IMAGEN AUTO + PRECARGA)
    // =========================
    private void updateCarImage(int position) {

        if (ivCar == null) return;
        if (position < 0 || position >= carIds.size()) return;

        String carId = carIds.get(position);

        // Intentamos cargar LA URL desde precarga
        firestore.collection("Coches")
                .document(carId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        ivCar.setImageResource(R.drawable.coche_julia);
                        return;
                    }

                    String fotoUrl = doc.getString("Foto");

                    // ============================
                    // 1️⃣ SI YA ESTÁ PRECARGADA → instantáneo
                    // ============================
                    if (fotoUrl != null &&
                            ImagePreloader.getCarImages().contains(fotoUrl)) {

                        Glide.with(requireContext())
                                .load(fotoUrl)
                                .placeholder(R.drawable.coche_julia)
                                .into(ivCar);

                        return;
                    }

                    // ============================
                    // 2️⃣ Si NO está precargada → cargar normalmente y cachear
                    // ============================
                    if (fotoUrl != null && !fotoUrl.isEmpty()) {

                        Glide.with(requireContext())
                                .load(fotoUrl)
                                .placeholder(R.drawable.coche_julia)
                                .into(ivCar);

                        return;
                    }

                    ivCar.setImageResource(R.drawable.coche_julia);
                })
                .addOnFailureListener(e -> ivCar.setImageResource(R.drawable.coche_julia));
    }


    private void updateLockUi() {
        if (ivLock == null || tvLockState == null) return;

        if (isLocked) {
            ivLock.setImageResource(R.drawable.ic_candado_cerrado);
            ivLock.setImageTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.error));
            tvLockState.setText(R.string.bloqueado);
            tvLockState.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error));
        } else {
            ivLock.setImageResource(R.drawable.ic_candado_abierto);
            ivLock.setImageTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.verdeoscuro));
            tvLockState.setText(R.string.puertas_abiertas);
            tvLockState.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.verdeoscuro));
        }
    }

    // =========================
    //  NOTIFICATION CHANNEL
    // =========================
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = requireContext();
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alertas del vehículo",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones relacionadas con el estado del coche");
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void guardarEventoPuerta(String carId, String estado) {
        if (firestore == null || carId == null) return;

        HashMap<String, Object> evento = new HashMap<>();
        evento.put("tipo", "estado");
        evento.put("estado", estado.equals("open") ? "open" : "closed");
        evento.put("timestamp", System.currentTimeMillis());

        firestore.collection("Coches")
                .document(carId)
                .collection("eventos")
                .add(evento);
    }



    // =========================
    //      HELPERS
    // =========================
    private String getLockPrefKeyForIndex(int index) {
        return PREF_KEY_LOCKED + "_" + index;
    }

    private String getFechaActual() {
        return new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault())
                .format(new Date());
    }
}
