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
import android.util.Log;

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
                new ArrayAdapter<>(context, R.layout.spinner_coches, carNames);
        carsAdapter.setDropDownViewResource(R.layout.spinner_coches);
        spinnerCars.setAdapter(carsAdapter);

        // Fondo del popup del spinner
        spinnerCars.setPopupBackgroundDrawable(
                ContextCompat.getDrawable(context, R.drawable.bg_card_soft)
        );

        int savedPosition = prefs.getInt(PREF_KEY_SELECTED_CAR, 0);
        currentCarIndex = savedPosition;

        // Abrir spinner tocando toda la tarjeta
        vehicleSelector.setOnClickListener(v -> spinnerCars.performClick());

        // Listener lock (APP → FIREBASE → RASPI)
        View.OnClickListener lockClickListener = v -> toggleLock(prefs);
        flLock.setOnClickListener(lockClickListener);
        ivLock.setOnClickListener(lockClickListener);

        // Listener SPINNER
        spinnerCars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position < 0 || position >= carNames.size()) return;

                currentCarIndex = position;

                // Guardar selección
                prefs.edit().putInt(PREF_KEY_SELECTED_CAR, position).apply();

                // Actualizar coche
                updateCarImage(position);

                // Restaurar estado local bloqueado/desbloqueado
                boolean savedLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
                isLocked = savedLocked;
                updateLockUi();

                // Escuchar estado en tiempo real de este coche en Firebase
                if (position < carIds.size()) {
                    String cocheIdReal = carIds.get(position);
                    Log.d("INTENTO", "Car seleccionado ID REAL = " + cocheIdReal);
                    listenEstadoActual(cocheIdReal);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Estado inicial del candado desde prefs
        boolean savedLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
        isLocked = savedLocked;
        updateLockUi();

        // ====== CARGAR COCHES DESDE FIRESTORE ======
        loadCarsFromFirestore(savedPosition, carsAdapter);

        // ====== NAVEGAR A CÁMARAS ======
        layoutCamaras.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CameraFragment())
                        .addToBackStack(null)
                        .commit()
        );

        // ====== ABRIR GOOGLE MAPS ======
        addressPill.setOnClickListener(v -> {
            double lat = 38.99614697675971;
            double lon = -0.16569078767633452;
            String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        });

        // ====== SEÑALES (ANIMACIÓN + FIREBASE) ======
        View.OnClickListener signalsClick = v ->
                handleSignalsClick(context, prefs, ivSignals);
        ivSignals.setOnClickListener(signalsClick);
        flSignals.setOnClickListener(signalsClick);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Quitar listener de Firebase
        if (estadoListener != null) {
            estadoListener.remove();
            estadoListener = null;
        }

        // Parar animaciones y vibración si quedaran activas
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

                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("Nombre");
                        if (nombre != null && !nombre.trim().isEmpty()) {
                            carNames.add(nombre.trim());
                            carIds.add(doc.getId());
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (!carNames.isEmpty()) {
                        int pos = savedPosition;
                        if (pos < 0 || pos >= carNames.size()) pos = 0;
                        spinnerCars.setSelection(pos);
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

                    Log.d("INTENTO", "Estado recibido: " + doc.getData());

                    String puerta = doc.getString("puerta");
                    Boolean impacto = doc.getBoolean("impacto");

                    // ===== PUERTA =====
                    if (puerta != null && !puerta.equals(ultimaPuerta)) {

                        ultimaPuerta = puerta;

                        // Mapeamos "open"/"closed" a nuestro candado
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

                    // ===== IMPACTO =====
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

                    // Impacto reciente = ocurrío hace menos de 10 segundos
                    if (tipo.equals("impacto") && (ahora - ts) < 10_000) {
                        manejarImpacto();
                    } else {
                        Log.d("INTENTO", "Impacto antiguo, NO abrir cámara");
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
            // Aún no se han cargado los coches
            isLocked = !isLocked;
            updateLockUi();
            return;
        }

        isLocked = !isLocked;

        // Guardar local
        prefs.edit()
                .putBoolean(getLockPrefKeyForIndex(currentCarIndex), isLocked)
                .apply();

        // Actualizar UI
        updateLockUi();

        // Actualizar Firestore (puerta open/closed)
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
        Context ctx = requireContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean puertasAbiertasEnabled = prefs.getBoolean("swPuertasAbiertas", true);
        if (!puertasAbiertasEnabled) return;

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

        boolean puertasAbiertasEnabled = prefs.getBoolean("swPuertasAbiertas", true);
        if (!puertasAbiertasEnabled) return;

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

        // Vibración potente
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

        // Notificación
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

        // Abrir cámara
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
        updateFirestoreAlerts(true);   // <<--- 7: mandar también al coche por Firebase

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
            updateFirestoreAlerts(false);  // apagar alertas en Firebase
        };

        // Parar señales tras 10 segundos
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

        // 1) Crear evento para el M5Stack
        long ts = System.currentTimeMillis();

        Map<String, Object> evento = new HashMap<>();
        evento.put("tipo", "alerta_sonido");
        evento.put("activar", active ? 1 : 0);
        evento.put("timestamp", ts);

        firestore.collection("Coches")
                .document(carId)
                .collection("eventos")
                .add(evento)
                .addOnSuccessListener(docRef ->
                        Log.d("INTENTO", "✅ Evento alerta_sonido creado: " + active)
                )
                .addOnFailureListener(e ->
                        Log.e("INTENTO", "❌ Error creando evento alerta_sonido", e)
                );

        // 2) Actualizar estado actual (tu código original)
        firestore.collection("Coches")
                .document(carId)
                .collection("estado")
                .document("actual")
                .update("alertaSonido", active)
                .addOnSuccessListener(a ->
                        Log.d("INTENTO", "🔥 Alertas = " + active + " enviado a Firebase")
                )
                .addOnFailureListener(e ->
                        Log.e("INTENTO", "❌ Error actualizando alertas", e)
                );
    }


    // =========================
    //          UI
    // =========================
    private void updateCarImage(int position) {
        if (ivCar == null) return;

        String drawableName;
        switch (position) {
            case 0:
                drawableName = "coche_naranja";
                break;
            case 1:
                drawableName = "coche_azul";
                break;
            case 2:
                drawableName = "coche_negro";
                break;
            default:
                drawableName = "coche_naranja";
                break;
        }

        int resId = getResources().getIdentifier(
                drawableName,
                "drawable",
                requireContext().getPackageName()
        );
        if (resId != 0) ivCar.setImageResource(resId);
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
