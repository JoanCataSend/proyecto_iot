package com.example.proyecto_iot;

import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
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

import java.lang.reflect.Constructor;
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
    private static final int NOTIFICATION_ID_SAFE_MODE = 1005;

    private boolean primerSnapshotSafeMode = true;
    private Boolean ultimoSafeModeNotificado = null;


    private static final String PREFS_NAME = "notificaciones_prefs";
    private static final String PREF_KEY_LOCKED = "lock_state_locked";
    private static final String PREF_KEY_SELECTED_CAR = "vehiculo_seleccionado";
    private static final String PREF_KEY_SELECTED_CAR_ID = "vehiculo_seleccionado_id";

    private static final long SIGNALS_DURATION_MS = 5_000;

    // Anti-rebote puertas (Código2)
    private static final long DOOR_COOLDOWN_MS = 3000; // 3s (sube si tu hardware rebota)
    private String doorExpectedState = null; // "open" o "closed"
    private long doorExpectedUntilTs = 0L;
    private boolean doorCommandInFlight = false;

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

    private TextView tvAddress;
    private View addressPillView;

    // =========================
    //      ESTADO
    // =========================
    private boolean puertasHabilitadas = true;
    private boolean alarmaHabilitada = true;


    private boolean isLocked = true;
    private int currentCarIndex = 0;
    private boolean spinnerInicializado = false;

    private String ultimaPuerta = null;
    private boolean ultimoImpacto = false;

    // Listener robusto (Código2)
    private boolean primerSnapshotEstado = true;
    private String ultimoCarIdListener = null;

    // =========================
    //      SEÑALES
    // =========================
    private ValueAnimator signalsBlinkAnimator;
    private Handler signalsStopHandler;
    private Runnable signalsStopRunnable;
    private Vibrator vibrator;

    // =========================
    //      MODO SEGURO
    // =========================
    private LinearLayout layoutSafeMode;
    private ImageView ivSafe;
    private TextView tvSafeState;
    private LinearLayout layoutCamarasRef; // para poder desactivar cámaras desde aquí
    private boolean safeModeEnabled = true; // por defecto ON (cámbialo si quieres)
    private ListenerRegistration safeModeListener;


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
        if (safeModeListener != null) {
            safeModeListener.remove();
            safeModeListener = null;
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Mantener comportamiento del Código1
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

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

        loadCarsFromFirestore(savedPosition, (ArrayAdapter<String>) spinnerCars.getAdapter());
    }

    // =========================
    //      BIND / SETUP
    // =========================
    private void bindViews(View view) {
        spinnerCars = view.findViewById(R.id.spinnerCars);
        ivCar = view.findViewById(R.id.ivCar);

        flLock = view.findViewById(R.id.flLock);
        ivLock = view.findViewById(R.id.ivLock);
        tvLockState = view.findViewById(R.id.tvLockState);

        ivSignals = view.findViewById(R.id.ivSignals);
        flSignals = (FrameLayout) ivSignals.getParent();

        tvAddress = view.findViewById(R.id.tvAddress);
        addressPillView = view.findViewById(R.id.addressPill);

        layoutSafeMode = view.findViewById(R.id.layoutSafeMode);
        ivSafe = view.findViewById(R.id.ivSafe);
        tvSafeState = view.findViewById(R.id.tvSafeState);

        // referencia para poder desactivar/activar cámaras
        layoutCamarasRef = view.findViewById(R.id.layoutCamaras);

    }

    private void setupSpinner(Context context, SharedPreferences prefs) {

        ArrayAdapter<String> carsAdapter = new ArrayAdapter<String>(
                context, R.layout.spinner_coches, carNames
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Mantener el “valor visible” como el coche actual (Código1/2)
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

                // Guardar también ID real (Código1)
                if (position < carIds.size()) {
                    String carId = carIds.get(position);
                    prefs.edit().putString(PREF_KEY_SELECTED_CAR_ID, carId).apply();

                    updateCarImage(position);
                    updateCarLocation(carId);
                    leerSeguridadPuertas(carId);
                    listenEstadoActual(carId);
                    primerSnapshotSafeMode = true;
                    ultimoSafeModeNotificado = null;
                    listenSafeMode(carId);
                } else {
                    updateCarImage(position);
                }

                isLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
                updateLockUi();

                spinnerCars.post(() -> ((ArrayAdapter) spinnerCars.getAdapter()).notifyDataSetChanged());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupClicks(Context context, SharedPreferences prefs) {

        LinearLayout vehicleSelector = requireView().findViewById(R.id.vehicleSelector);
        LinearLayout layoutCamaras = requireView().findViewById(R.id.layoutCamaras);

        vehicleSelector.setOnClickListener(v -> spinnerCars.performClick());

        // ✅ Candado: SOLO el contenedor (Código2) + seguridad puertas (Código1)
        View.OnClickListener lockClickListener = v -> {
            if (!puertasHabilitadas) return;
            toggleLockAndNotify(prefs);
        };
        flLock.setOnClickListener(lockClickListener);

        // Evitar doble disparo (Código2)
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
        layoutSafeMode.setOnClickListener(v -> {
            if (currentCarIndex < 0 || currentCarIndex >= carIds.size()) return;
            String carId = carIds.get(currentCarIndex);

            boolean nuevoEstado = !safeModeEnabled;
            setSafeModeFirestore(carId, nuevoEstado);
        });


        // Señales: SOLO contenedor (Código2)
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

                    if (!isAdded()) return;

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
                        String carId = carIds.get(pos);

                        // Inicialización completa (Código1)
                        getPrefs(requireContext())
                                .edit()
                                .putString(PREF_KEY_SELECTED_CAR_ID, carId)
                                .apply();

                        updateCarLocation(carId);
                        leerSeguridadPuertas(carId);
                        listenEstadoActual(carId);
                        primerSnapshotSafeMode = true;
                        ultimoSafeModeNotificado = null;
                        listenSafeMode(carId);
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

        // Reset robusto al cambiar de coche (Código2)
        if (ultimoCarIdListener == null || !ultimoCarIdListener.equals(carId)) {
            primerSnapshotEstado = true;
            ultimaPuerta = null;
            ultimoImpacto = false;

            ultimoCarIdListener = carId;

            doorExpectedState = null;
            doorExpectedUntilTs = 0L;
            doorCommandInFlight = false;
        }

        estadoListener = firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .addSnapshotListener((doc, error) -> {

                    if (!isAdded()) return;
                    if (error != null || doc == null || !doc.exists()) return;

                    String puerta = doc.getString("puerta");
                    if (puerta != null) puerta = puerta.trim().toLowerCase(Locale.ROOT);

                    Boolean impacto = doc.getBoolean("impacto");
                    boolean pending = doc.getMetadata() != null && doc.getMetadata().hasPendingWrites();

                    long now = System.currentTimeMillis();

                    // ---- PRIMER SNAPSHOT: solo inicializa, NO notifiques ----
                    if (primerSnapshotEstado) {
                        primerSnapshotEstado = false;

                        if (puerta != null) {
                            ultimaPuerta = puerta;
                            boolean lockedInit = !"open".equals(puerta);
                            isLocked = lockedInit;
                            updateLockUi();
                            guardarLockEnPrefs(lockedInit);
                        }
                        // impacto solo se procesa en cambios posteriores
                    } else {
                        // ✅ Filtro anti-rebote: si esperamos un estado y llega el contrario dentro del cooldown -> ignorar
                        if (doorExpectedState != null && now < doorExpectedUntilTs) {
                            if (puerta != null && !puerta.equals(doorExpectedState)) {
                                // Ignora totalmente rebotes del hardware
                                return;
                            }
                        }

                        // ---- Puertas: actualizar UI/PREFS pero NO notificar aquí (para evitar duplicados) ----
                        if (puerta != null && !puerta.equals(ultimaPuerta)) {
                            boolean lockedNow = !"open".equals(puerta);
                            isLocked = lockedNow;
                            updateLockUi();
                            guardarLockEnPrefs(lockedNow);

                            // Solo consolidamos "ultimaPuerta" si no hay writes pendientes
                            if (!pending) {
                                ultimaPuerta = puerta;
                            }
                        }
                    }

                    // ---- Impacto (Código1): solo cuando pasa a true y no estaba true ----
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

                    if (!isAdded()) return;
                    if (snap.isEmpty()) return;

                    QueryDocumentSnapshot doc =
                            (QueryDocumentSnapshot) snap.getDocuments().get(0);

                    String tipo = doc.getString("tipo");
                    Long ts = doc.getLong("timestamp");

                    if (tipo == null || ts == null) return;

                    long ahora = System.currentTimeMillis();

                    if (tipo.equals("impacto") && (ahora - ts) < 10_000) {
                        manejarImpacto();
                    }
                });
    }

    private void manejarImpacto() {

        if (!isAdded()) return;

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

    private void guardarLockEnPrefs(boolean locked) {
        SharedPreferences prefs = getPrefs(requireContext());
        prefs.edit()
                .putBoolean(getLockPrefKeyForIndex(currentCarIndex), locked)
                .apply();
    }

    // =========================
    //      CANDADO (APP → FIRESTORE)
    // =========================
    private void toggleLockAndNotify(SharedPreferences prefs) {

        if (!puertasHabilitadas) return;
        if (currentCarIndex < 0 || currentCarIndex >= carIds.size()) return;

        if (doorCommandInFlight) return;
        doorCommandInFlight = true;
        flLock.setEnabled(false);

        boolean targetLocked = !isLocked;

        String nuevoEstado = targetLocked ? "close" : "open";

        doorExpectedState = nuevoEstado;
        doorExpectedUntilTs = System.currentTimeMillis() + DOOR_COOLDOWN_MS;

        String carId = carIds.get(currentCarIndex);

        firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .update("puerta", nuevoEstado)
                .addOnSuccessListener(v -> {

                    // UI inmediata (Código2)
                    isLocked = targetLocked;
                    updateLockUi();
                    guardarLockEnPrefs(targetLocked);

                    // 🔔 Notificación SOLO aquí (no en listener)
                    if (targetLocked) mostrarNotifPuertaCerrada();
                    else mostrarNotifPuertaAbierta();

                    doorCommandInFlight = false;
                    flLock.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    doorCommandInFlight = false;
                    flLock.setEnabled(true);
                });
    }

    // =========================
    //      NOTIFICACIONES
    // =========================
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

    private void mostrarNotifPuertaCerrada() {
        if (!getPrefs(requireContext()).getBoolean("swPuertasAbiertas", true)) return;

        String titulo = getString(R.string.bloqueado);
        String mensaje = "Las puertas del coche se han bloqueado correctamente.";

        notifyAndSave(
                NOTIFICATION_ID_PUERTAS_CERRADAS,
                titulo,
                mensaje,
                R.drawable.ic_info,
                "Puertas"
        );
    }

    private void notifyAndSave(int notifId,
                               String titulo,
                               String mensaje,
                               int iconRes,
                               String category) {

        if (!isAdded()) return;

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

        // Guardar en repositorio sin romper constructores (merge seguro)
        safeAddNotificacion(titulo, mensaje, fecha, iconRes, getCurrentCarName(), category, ts);
    }

    private String getCurrentCarName() {
        return (currentCarIndex >= 0 && currentCarIndex < carNames.size())
                ? carNames.get(currentCarIndex)
                : "Vehículo";
    }

    /**
     * Merge-safe: si tu clase Notificacion tiene constructor "rico" (7 params), lo usa.
     * Si solo tiene el "simple" (4 params), cae al simple.
     * Así NO perdemos funcionalidad en proyectos donde exista el modelo avanzado,
     * y tampoco rompemos cmpilación en proyectos antiguos.
     */
    private void safeAddNotificacion(String titulo,
                                     String mensaje,
                                     String fecha,
                                     int iconRes,
                                     String carName,
                                     String category,
                                     long ts) {
        try {
            // Intentar constructor avanzado: (String, String, String, int, String, String, long)
            Constructor<?> c = Notificacion.class.getConstructor(
                    String.class, String.class, String.class, int.class, String.class, String.class, long.class
            );
            Object notif = c.newInstance(titulo, mensaje, fecha, iconRes, carName, category, ts);
            NotificacionRepository.getInstance().addNotificacion((Notificacion) notif);
            return;
        } catch (Exception ignored) { }

        try {
            // Fallback constructor simple: (String, String, String,, int)
            Constructor<?> c2 = Notificacion.class.getConstructor(
                    String.class, String.class, String.class, int.class
            );
            Object notif2 = c2.newInstance(titulo, mensaje, fecha, iconRes);
            NotificacionRepository.getInstance().addNotificacion((Notificacion) notif2);
        } catch (Exception ignored2) { }
    }

    // =========================
    //      SEÑALES (ALARMAS)
    // =========================
    private void handleSignalsClick(Context context,
                                    SharedPreferences prefs,
                                    ImageView ivSignals) {
        if (!alarmaHabilitada) return;

        boolean sonidoEnabled = prefs.getBoolean("swSonido", true);

        // UI ON (Código2)
        setSignalsUiOn(context, true);

        startColorBlinkAnimation(ivSignals, context);
        startVibrationPattern(context);
        updateFirestoreAlerts(true);

        if (sonidoEnabled) {
            String titulo = "Alertas activadas";
            String mensaje = "Se han activado las alertas luminosas y sonoras.";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_info)
                            .setContentTitle(titulo)
                            .setContentText(mensaje)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true);

            NotificationManagerCompat.from(context)
                    .notify(NOTIFICATION_ID_SENALES, builder.build());

            // Guardar notificación (merge-safe)
            safeAddNotificacion(
                    titulo,
                    mensaje,
                    getFechaActual(),
                    R.drawable.ic_sonido,
                    getCurrentCarName(),
                    "Alarmas",
                    System.currentTimeMillis()
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
        if (ivSignals != null) {
            ivSignals.setImageTintList(ContextCompat.getColorStateList(context, color));
        }
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

                    if (!isAdded()) return;

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
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    ivCar.setImageResource(R.drawable.coche_julia);
                });
    }

    // =========================
    //      UBICACIÓN REAL
    // =========================
    private void updateCarLocation(String carId) {

        if (tvAddress == null || addressPillView == null) return;

        firestore.collection("Coches")
                .document(carId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!isAdded()) return;
                    if (!doc.exists()) return;

                    Double lat = doc.getDouble("lat");
                    Double lng = doc.getDouble("lng");

                    if (lat == null || lng == null) {
                        tvAddress.setText("Ubicación desconocida");
                        addressPillView.setOnClickListener(null);
                        return;
                    }

                    String direccion = "Ubicación desconocida";
                    try {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            direccion = addresses.get(0).getAddressLine(0);
                        }
                    } catch (Exception e) {
                        direccion = "Dirección no disponible";
                    }

                    tvAddress.setText(direccion);

                    double finalLat = lat;
                    double finalLng = lng;

                    addressPillView.setOnClickListener(v -> {
                        if (!isAdded()) return;
                        String uri = "geo:" + finalLat + "," + finalLng
                                + "?q=" + finalLat + "," + finalLng;
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);
                    });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    tvAddress.setText("Ubicación desconocida");
                    addressPillView.setOnClickListener(null);
                });
    }

    // =========================
    //      UI - CANDADO
    // =========================
    private void updateLockUi() {

        if (!puertasHabilitadas) {
            actualizarUiPuertas();
            return;
        }

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
    //      SEGURIDAD PUERTAS
    // =========================
    @SuppressWarnings("unchecked")
    private void leerSeguridadPuertas(String carId) {

        firestore.collection("Coches")
                .document(carId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!isAdded()) return;

                    Map<String, Object> seguridad =
                            (Map<String, Object>) doc.get("seguridad");

                    if (seguridad == null) {
                        puertasHabilitadas = true;
                        alarmaHabilitada = true;
                    } else {
                        puertasHabilitadas = getBool(seguridad, "puertas");
                        alarmaHabilitada  = getBool(seguridad, "alarma");
                    }

                    actualizarUiPuertas();
                    actualizarUiAlarmas(); // 👈 NUEVO
                });
    }

    private void actualizarUiAlarmas() {

        if (flSignals == null || ivSignals == null) return;

        if (!safeModeEnabled) {
            flSignals.setEnabled(false);
            ivSignals.setEnabled(false);
            ivSignals.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.texto_desactivado));
            return;
        }

        if (!alarmaHabilitada) {
            flSignals.setEnabled(false);
            ivSignals.setEnabled(false);
            ivSignals.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.texto_desactivado));
        } else {
            flSignals.setEnabled(true);
            ivSignals.setEnabled(true);
            ivSignals.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.texto_oscuro));
        }
    }


    private void actualizarUiPuertas() {

        if (flLock == null || ivLock == null || tvLockState == null) return;

        if (!safeModeEnabled) {
            flLock.setEnabled(false);
            ivLock.setEnabled(false);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.texto_desactivado));
            tvLockState.setText("Desactivado");
            tvLockState.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_desactivado));
            return;
        }

        if (!puertasHabilitadas) {
            flLock.setEnabled(false);
            ivLock.setEnabled(false);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.texto_desactivado));
            tvLockState.setText("Desactivado");
            tvLockState.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_desactivado));
        } else {
            flLock.setEnabled(true);
            ivLock.setEnabled(true);
            updateLockUi();
        }
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

    private boolean getBool(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Boolean ? (Boolean) v : true;
    }

    private void listenSafeMode(String carId) {

        if (safeModeListener != null) {
            safeModeListener.remove();
            safeModeListener = null;
        }

        safeModeListener = firestore.collection("Coches")
                .document(carId)
                .addSnapshotListener((doc, error) -> {
                    if (!isAdded()) return;
                    if (error != null || doc == null || !doc.exists()) return;

                    Boolean v = doc.getBoolean("safeMode");
                    if (v == null) v = true;

                    // 1) Primer snapshot: solo inicializa, NO notificar
                    if (primerSnapshotSafeMode) {
                        primerSnapshotSafeMode = false;
                        safeModeEnabled = v;
                        ultimoSafeModeNotificado = v;

                        actualizarUiSafeMode();
                        actualizarUiPuertas();
                        actualizarUiAlarmas();
                        actualizarUiCamaras();
                        return;
                    }

                    // 2) Si cambió respecto al último valor notificado → notifica
                    if (ultimoSafeModeNotificado == null || !v.equals(ultimoSafeModeNotificado)) {
                        mostrarNotifSafeMode(v);
                        ultimoSafeModeNotificado = v;
                    }

                    safeModeEnabled = v;

                    if (!safeModeEnabled) {
                        stopSignalsEffects();
                        updateFirestoreAlerts(false);
                    }

                    actualizarUiSafeMode();
                    actualizarUiPuertas();
                    actualizarUiAlarmas();
                    actualizarUiCamaras();
                });
    }


    private void setSafeModeFirestore(String carId, boolean activar) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("safeMode", activar);

        // Esto “activa/desactiva sensores” en tu estructura actual
        // (tú ya lees seguridad.puertas y seguridad.alarma para habilitar UI)
        updates.put("seguridad.puertas", activar);
        updates.put("seguridad.alarma", activar);

        firestore.collection("Coches")
                .document(carId)
                .update(updates);
        // La UI se actualizará sola por el listener listenSafeMode()

        // ✅ GUARDAR EVENTO PARA HISTORIAL
        Map<String, Object> evento = new HashMap<>();
        evento.put("tipo", "safe_mode");
        evento.put("activar", activar ? 1 : 0);
        evento.put("timestamp", System.currentTimeMillis());

        firestore.collection("Coches")
                .document(carId)
                .collection("eventos")
                .add(evento);
    }

    private void actualizarUiSafeMode() {
        if (ivSafe == null || tvSafeState == null) return;

        if (safeModeEnabled) {
            tvSafeState.setText("Seguro: ON");
            ivSafe.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.verdeoscuro));
            ivSafe.setAlpha(1f);
        } else {
            tvSafeState.setText("Seguro: OFF");
            ivSafe.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.texto_desactivado));
            ivSafe.setAlpha(0.7f);
        }
    }

    private void actualizarUiCamaras() {
        if (layoutCamarasRef == null) return;

        layoutCamarasRef.setEnabled(safeModeEnabled);
        layoutCamarasRef.setAlpha(safeModeEnabled ? 1f : 0.5f);
    }

    private void mostrarNotifSafeMode(boolean activado) {
        String titulo = "Modo seguro";
        String mensaje = activado ? "Activado" : "Desactivado";

        notifyAndSave(
                NOTIFICATION_ID_SAFE_MODE,
                titulo,
                mensaje,
                R.drawable.ic_escudo,   // usa tu icono del escudo
                "Seguridad"
        );
    }


}
