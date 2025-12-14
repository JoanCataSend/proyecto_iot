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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
    private static final int NOTIFICATION_ID_IMPACTO = 1003;
    private static final int NOTIFICATION_ID_SENALES = 1004;

    private static final String PREFS_NAME = "notificaciones_prefs";
    private static final String PREF_KEY_LOCKED = "lock_state_locked";
    private static final String PREF_KEY_SELECTED_CAR = "vehiculo_seleccionado";

    private static final long SIGNALS_DURATION_MS = 5_000;



    // =========================
    //      FIRESTORE / DATOS
    // =========================
    private FirebaseFirestore firestore;
    private ListenerRegistration estadoListener;

    private final List<String> carNames = new ArrayList<>();
    private final List<String> carIds = new ArrayList<>();
    private final List<String> popupItems = new ArrayList<>();

    // =========================
    //      UI
    // =========================
    private Spinner spinnerCars;
    private ImageView ivCar;

    private FrameLayout flLock;
    private ImageView ivLock;
    private TextView tvLockState;

    private ImageView ivSignals;
    private FrameLayout flSignals;

    // =========================
    //      ESTADO
    // =========================
    private boolean isLocked = true;
    private int currentCarIndex = 0;

    private boolean spinnerInicializado = false;

    private String ultimaPuerta = null;
    private boolean ultimoImpacto = false;


    // =========================
    //      SEÑALES
    // =========================
    private ValueAnimator signalsBlinkAnimator;
    private Handler signalsStopHandler;
    private Runnable signalsStopRunnable;
    private Vibrator vibrator;

    // =========================
    //      LIFECYCLE
    // =========================
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        Context context = requireContext();
        SharedPreferences prefs = getPrefs(context);

        bindViews(view);
        setupSpinner(context, prefs);
        setupClicks(context, prefs);

        int savedPosition = prefs.getInt(PREF_KEY_SELECTED_CAR, 0);
        currentCarIndex = savedPosition;

        // Estado candado inicial
        isLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
        updateLockUi();

        // Cargar coches + escuchar el seleccionado
        loadCarsFromFirestore(savedPosition, (ArrayAdapter<String>) spinnerCars.getAdapter());

        NotificationManagerCompat nm = NotificationManagerCompat.from(requireContext());

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
    //      SETUP UI
    // =========================
    private void bindViews(View view) {
        spinnerCars = view.findViewById(R.id.spinnerCars);
        ivCar = view.findViewById(R.id.ivCar);

        flLock = view.findViewById(R.id.flLock);
        ivLock = view.findViewById(R.id.ivLock);
        tvLockState = view.findViewById(R.id.tvLockState);

        ivSignals = view.findViewById(R.id.ivSignals);
        flSignals = (FrameLayout) ivSignals.getParent();
    }

    private void setupSpinner(Context context, SharedPreferences prefs) {

        ArrayAdapter<String> carsAdapter = new ArrayAdapter<String>(
                context, R.layout.spinner_coches, carNames
        ) {
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

        spinnerCars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

                if (!spinnerInicializado) {
                    spinnerInicializado = true;
                    return;
                }

                if (position >= 0 && position < popupItems.size()
                        && popupItems.get(position).contains("Añadir")) {
                    startActivity(new Intent(requireContext(), PrimerCocheActivity.class));
                    spinnerCars.setSelection(currentCarIndex, false);
                    return;
                }

                if (position < 0 || position >= carNames.size()) return;

                currentCarIndex = position;
                prefs.edit().putInt(PREF_KEY_SELECTED_CAR, position).apply();

                updateCarImage(position);

                isLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
                updateLockUi();

                if (position < carIds.size()) {
                    listenEstadoActual(carIds.get(position));
                }

                spinnerCars.post(() -> ((ArrayAdapter) spinnerCars.getAdapter()).notifyDataSetChanged());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupClicks(Context context, SharedPreferences prefs) {

        LinearLayout vehicleSelector = requireView().findViewById(R.id.vehicleSelector);
        LinearLayout layoutCamaras = requireView().findViewById(R.id.layoutCamaras);
        LinearLayout addressPill = requireView().findViewById(R.id.addressPill);

        vehicleSelector.setOnClickListener(v -> spinnerCars.performClick());

        View.OnClickListener lockClickListener = v -> toggleLockAndNotify(prefs);

        // SOLO el contenedor clicable (zona grande)
        flLock.setOnClickListener(lockClickListener);

        // El icono NO clicable (evita disparo doble)
        ivLock.setClickable(false);
        ivLock.setFocusable(false);


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

        // Señales: SOLO el contenedor para evitar doble click
        View.OnClickListener signalsClick = v -> handleSignalsClick(context, prefs, ivSignals);
        flSignals.setOnClickListener(signalsClick);

        ivSignals.setClickable(false);
        ivSignals.setFocusable(false);
    }

    // =========================
    //      FIRESTORE - COCHES
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

                    if (carNames.isEmpty()) return;

                    int pos = savedPosition;
                    if (pos < 0 || pos >= carNames.size()) pos = 0;

                    currentCarIndex = pos;
                    spinnerCars.setSelection(pos, false);
                    updateCarImage(pos);

                    if (pos < carIds.size()) {
                        listenEstadoActual(carIds.get(pos));
                    }
                });
    }

    // =========================
    //      FIRESTORE - ESTADO
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

                    if (puerta != null) {
                        puerta = puerta.trim().toLowerCase(Locale.ROOT);
                    }

                    if (puerta != null && !puerta.equals(ultimaPuerta)) {
                        ultimaPuerta = puerta;

                        boolean lockedNow = !"open".equals(puerta);
                        isLocked = lockedNow;
                        updateLockUi();
                        guardarLockEnPrefs(lockedNow);

                        if (lockedNow) {
                            mostrarNotifPuertaCerrada();
                        } else {
                            mostrarNotifPuertaAbierta();
                        }
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

    private void guardarEventoPuerta(String carId, String puerta) {
        if (firestore == null || carId == null) return;

        HashMap<String, Object> evento = new HashMap<>();
        evento.put("tipo", "puerta");
        evento.put("puerta", "open".equals(puerta) ? "open" : "closed");
        evento.put("timestamp", System.currentTimeMillis());

        firestore.collection("Coches")
                .document(carId)
                .collection("eventos")
                .add(evento);
    }

    private void guardarLockEnPrefs(boolean locked) {
        SharedPreferences prefs = getPrefs(requireContext());
        prefs.edit()
                .putBoolean(getLockPrefKeyForIndex(currentCarIndex), locked)
                .apply();
    }


    // =========================
    //      CANDADO
    // =========================
    private void toggleLockAndNotify(SharedPreferences prefs) {
        if (currentCarIndex < 0 || currentCarIndex >= carIds.size()) return;

        boolean targetLocked = !isLocked;
        String nuevoEstado = targetLocked ? "closed" : "open";

        String carId = carIds.get(currentCarIndex);

        firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .update("puerta", nuevoEstado);
    }

    private void mostrarNotifPuertaCerrada() {
        if (!getPrefs(requireContext()).getBoolean("swPuertasAbiertas", true)) return;

        String titulo = getString(R.string.bloqueado);
        String mensaje = "Las puertas del coche se han bloqueado correctamente.";

        notifyAndSave(
                NOTIFICATION_ID_PUERTAS,
                titulo,
                mensaje,
                R.drawable.ic_info,
                "Puertas"
        );
    }

    private void manejarImpacto() {
        vibrateOnce(700);

        String titulo = "Impacto detectado";
        String mensaje = "Tu vehículo ha recibido un impacto";

        notifyAndSave(
                NOTIFICATION_ID_IMPACTO,
                titulo,
                mensaje,
                R.drawable.ic_info,
                "Impacto"
        );

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CameraFragment())
                .addToBackStack(null)
                .commit();
    }

    private void notifyAndSave(int notifId,
                               String titulo,
                               String mensaje,
                               int iconRes,
                               String category) {

        Context ctx = requireContext();
        String fecha = getFechaActual();
        long ts = System.currentTimeMillis();

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_info)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(ctx).notify(notifId, b.build());

        String carName = getCurrentCarName();

        NotificacionRepository.getInstance().addNotificacion(
                new Notificacion(titulo, mensaje, fecha, iconRes, carName, category, ts)
        );
    }

    private String getCurrentCarName() {
        return (currentCarIndex >= 0 && currentCarIndex < carNames.size())
                ? carNames.get(currentCarIndex)
                : "Vehículo";
    }

    // =========================
    //      SEÑALES (ALARMAS)
    // =========================
    private void handleSignalsClick(Context context,
                                    SharedPreferences prefs,
                                    ImageView ivSignals) {

        boolean sonidoEnabled = prefs.getBoolean("swSonido", true);

        // UI ON
        setSignalsUiOn(context, true);

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
                    new Notificacion(
                            titulo,
                            mensaje,
                            fecha,
                            R.drawable.ic_sonido,
                            getCurrentCarName(),
                            "Alarmas",
                            System.currentTimeMillis()
                    )
            );
        }

        scheduleSignalsStop(context);
    }

    private void scheduleSignalsStop(Context context) {
        if (signalsStopHandler == null) {
            signalsStopHandler = new Handler(Looper.getMainLooper());
        }

        if (signalsStopRunnable != null) {
            signalsStopHandler.removeCallbacks(signalsStopRunnable);
        }

        signalsStopRunnable = () -> {
            stopBlinkAnimation();
            if (vibrator != null) vibrator.cancel();

            setSignalsUiOn(context, false);
            updateFirestoreAlerts(false);
        };

        signalsStopHandler.postDelayed(signalsStopRunnable, SIGNALS_DURATION_MS);
    }

    private void setSignalsUiOn(Context context, boolean on) {
        int color = on ? R.color.aviso : R.color.texto_oscuro;
        ivSignals.setImageTintList(ContextCompat.getColorStateList(context, color));
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
            target.setColorFilter(blendColors(color1, color2, f));
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

    private void vibrateOnce(long ms) {
        Context ctx = requireContext();
        vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms);
        }
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
    //      UI - IMAGEN COCHE
    // =========================
    private void updateCarImage(int position) {
        if (ivCar == null) return;
        if (position < 0 || position >= carIds.size()) return;

        String carId = carIds.get(position);

        firestore.collection("Coches")
                .document(carId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        ivCar.setImageResource(R.drawable.coche_julia);
                        return;
                    }

                    String fotoUrl = doc.getString("Foto");

                    if (fotoUrl != null && ImagePreloader.getCarImages().contains(fotoUrl)) {
                        Glide.with(requireContext())
                                .load(fotoUrl)
                                .placeholder(R.drawable.coche_julia)
                                .into(ivCar);
                        return;
                    }

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
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.error));
            tvLockState.setText(R.string.bloqueado);
            tvLockState.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
        } else {
            ivLock.setImageResource(R.drawable.ic_candado_abierto);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.verdeoscuro));
            tvLockState.setText(R.string.puertas_abiertas);
            tvLockState.setTextColor(ContextCompat.getColor(requireContext(), R.color.verdeoscuro));
        }
    }

    private void mostrarNotifPuertaAbierta() {
        if (!getPrefs(requireContext()).getBoolean("swPuertasAbiertas", true)) return;

        String titulo = getString(R.string.puertas_abiertas);
        String mensaje = "Las puertas del coche se han desbloqueado correctamente.";

        notifyAndSave(
                NOTIFICATION_ID_PUERTAS,
                titulo,
                mensaje,
                R.drawable.ic_info,
                "Puertas"
        );
    }


    // =========================
    //      NOTIFICATION CHANNEL
    // =========================
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        Context context = requireContext();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alertas del vehículo",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notificaciones relacionadas con el estado del coche");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    // =========================
    //      HELPERS
    // =========================
    private SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String getLockPrefKeyForIndex(int index) {
        return PREF_KEY_LOCKED + "_" + index;
    }

    private String getFechaActual() {
        return new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault())
                .format(new Date());
    }
}
