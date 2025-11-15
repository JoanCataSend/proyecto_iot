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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IntentoFragment extends Fragment {

    private static final String CHANNEL_ID = "alertas_vehiculo";
    private static final int NOTIFICATION_ID_PUERTAS = 1001;
    private static final int NOTIFICATION_ID_SENALES = 1002;
    private static final String PREFS_NAME = "notificaciones_prefs";
    private static final String PREF_KEY_LOCKED = "lock_state_locked";        // base
    private static final String PREF_KEY_SELECTED_CAR = "vehiculo_seleccionado";

    private boolean isLocked = true;

    // UI
    private Spinner spinnerCars;
    private ImageView ivCar;

    private FrameLayout flLock;
    private ImageView ivLock;
    private TextView tvLockState;

    // Índice del coche actual en el spinner
    private int currentCarIndex = 0;

    // Señales: animación y vibración
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

        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        spinnerCars        = view.findViewById(R.id.spinnerCars);
        ivCar              = view.findViewById(R.id.ivCar);
        LinearLayout vehicleSelector = view.findViewById(R.id.vehicleSelector);
        LinearLayout layoutCamaras   = view.findViewById(R.id.layoutCamaras);
        LinearLayout addressPill     = view.findViewById(R.id.addressPill);

        flLock      = view.findViewById(R.id.flLock);
        ivLock      = view.findViewById(R.id.ivLock);
        tvLockState = view.findViewById(R.id.tvLockState);

        // Abrir el spinner tocando toda la tarjeta
        if (vehicleSelector != null) {
            vehicleSelector.setOnClickListener(v -> {
                if (spinnerCars != null) spinnerCars.performClick();
            });
        }

        // =================== SPINNER DE COCHES ===================
        if (spinnerCars != null) {

            // --- IMPLEMENTACIÓN ACTUAL: lee de resources (lista_coches) ---
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                    context,
                    R.layout.spinner_coches,
                    getResources().getStringArray(R.array.lista_coches)
            );
            adapter.setDropDownViewResource(R.layout.spinner_coches);
            spinnerCars.setAdapter(adapter);

            spinnerCars.setPopupBackgroundDrawable(
                    ContextCompat.getDrawable(context, R.drawable.bg_card_soft)
            );

            // Hacer que el dropdown tenga el mismo ancho que el spinner
            spinnerCars.post(() -> {
                spinnerCars.setDropDownWidth(spinnerCars.getWidth());
            });

            int savedPosition = prefs.getInt(PREF_KEY_SELECTED_CAR, 0);
            currentCarIndex = savedPosition;
            spinnerCars.setSelection(savedPosition);

            spinnerCars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    currentCarIndex = position;
                    updateCarImage(position);

                    // Guardar el coche seleccionado
                    prefs.edit().putInt(PREF_KEY_SELECTED_CAR, position).apply();

                    // Cargar el estado de bloqueo específico de este coche
                    boolean carLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
                    isLocked = carLocked;
                    if (flLock != null && ivLock != null && tvLockState != null) {
                        updateLockUi(flLock, ivLock, tvLockState);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });

            // Cargar imagen y estado del coche guardado
            updateCarImage(savedPosition);
        }

        // ========== FUTURA IMPLEMENTACIÓN FIREBASE PARA EL SPINNER (COMENTADA) ==========
        /*
        // Ejemplo de cómo poblar el spinner con Firebase:
        //
        // import com.google.firebase.auth.FirebaseAuth;
        // import com.google.firebase.database.DataSnapshot;
        // import com.google.firebase.database.DatabaseError;
        // import com.google.firebase.database.DatabaseReference;
        // import com.google.firebase.database.FirebaseDatabase;
        // import com.google.firebase.database.ValueEventListener;
        //
        // FirebaseAuth auth = FirebaseAuth.getInstance();
        // String uid = auth.getCurrentUser().getUid();
        //
        // DatabaseReference carsRef = FirebaseDatabase.getInstance()
        //        .getReference("usuarios")
        //        .child(uid)
        //        .child("coches");
        //
        // carsRef.addListenerForSingleValueEvent(new ValueEventListener() {
        //     @Override
        //     public void onDataChange(@NonNull DataSnapshot snapshot) {
        //         List<String> nombresCoches = new ArrayList<>();
        //         // Opcional: lista de IDs si quieres lógica por ID real
        //         List<String> idsCoches = new ArrayList<>();
        //
        //         for (DataSnapshot carSnap : snapshot.getChildren()) {
        //             String nombre = carSnap.child("nombre").getValue(String.class);
        //             if (nombre != null) {
        //                 nombresCoches.add(nombre);
        //                 idsCoches.add(carSnap.getKey());
        //             }
        //         }
        //
        //         ArrayAdapter<String> adapterFirebase = new ArrayAdapter<>(
        //                 requireContext(),
        //                 R.layout.spinner_coches,
        //                 nombresCoches
        //         );
        //         adapterFirebase.setDropDownViewResource(R.layout.spinner_coches);
        //         spinnerCars.setAdapter(adapterFirebase);
        //
        //         // Aquí podrías guardar en SharedPreferences la relación posición–idCoche
        //         // para que el estado de bloqueo sea 100% por coche real.
        //     }
        //
        //     @Override
        //     public void onCancelled(@NonNull DatabaseError error) { }
        // });
        */
        // =========================================================================

        // Navegar a CameraFragment al tocar la tarjeta de cámaras
        if (layoutCamaras != null) {
            layoutCamaras.setOnClickListener(v -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CameraFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // =========== CLICK EN addressPill PARA ABRIR GOOGLE MAPS ===========
        if (addressPill != null) {
            addressPill.setOnClickListener(v -> {
                double lat = 38.99614697675971;
                double lon = -0.16569078767633452;
                String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            });
        }

        // === BLOQUE CANDADO ===
        if (flLock != null && ivLock != null && tvLockState != null) {

            // Estado de bloqueo del coche actualmente seleccionado
            boolean savedLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
            isLocked = savedLocked;
            updateLockUi(flLock, ivLock, tvLockState);

            View.OnClickListener lockClickListener =
                    v -> handleLockClick(context, prefs, flLock, ivLock, tvLockState);

            flLock.setOnClickListener(lockClickListener);
            ivLock.setOnClickListener(lockClickListener);
        }

        // === BLOQUE SEÑALES ===
        ImageView ivSignals = view.findViewById(R.id.ivSignals);
        FrameLayout flSignals = (ivSignals != null && ivSignals.getParent() instanceof FrameLayout)
                ? (FrameLayout) ivSignals.getParent()
                : null;

        if (ivSignals != null && flSignals != null) {
            View.OnClickListener signalsClick = v -> handleSignalsClick(context, prefs, ivSignals);
            ivSignals.setOnClickListener(signalsClick);
            flSignals.setOnClickListener(signalsClick);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopSignalsEffects();
    }

    /** Devuelve la clave de SharedPreferences para el estado de bloqueo de un coche concreto */
    private String getLockPrefKeyForIndex(int index) {
        return PREF_KEY_LOCKED + "_" + index;
    }

    /** Cambia la imagen del coche según la posición del spinner */
    private void updateCarImage(int position) {
        if (ivCar == null) return;
        String drawableName;
        switch (position) {
            case 0: drawableName = "coche_naranja"; break;
            case 1: drawableName = "coche_azul";    break;
            case 2: drawableName = "coche_negro";   break;
            default: drawableName = "coche_naranja"; break;
        }

        int resId = getResources().getIdentifier(drawableName, "drawable", requireContext().getPackageName());
        if (resId != 0) ivCar.setImageResource(resId);
    }

    /** Maneja el click sobre el candado (por coche) */
    private void handleLockClick(Context context, SharedPreferences prefs,
                                 FrameLayout flLock, ImageView ivLock, TextView tvLockState) {

        boolean puertasAbiertasEnabled = prefs.getBoolean("swPuertasAbiertas", true);

        // Alternar estado del coche actual
        isLocked = !isLocked;
        prefs.edit().putBoolean(getLockPrefKeyForIndex(currentCarIndex), isLocked).apply();

        String titulo, mensaje;
        if (isLocked) {
            titulo = getString(R.string.bloqueado);
            mensaje = "Las puertas del coche se han bloqueado correctamente desde la app.";
        } else {
            titulo = getString(R.string.puertas_abiertas);
            mensaje = "Las puertas del coche se han desbloqueado correctamente desde la app.";
        }

        String fecha = new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault()).format(new Date());

        if (puertasAbiertasEnabled) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_info)
                    .setContentTitle(titulo)
                    .setContentText(mensaje)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PUERTAS, builder.build());
            NotificacionRepository.getInstance().addNotificacion(new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info));
        }

        updateLockUi(flLock, ivLock, tvLockState);
    }

    /** CLICK: Señales (sonido/luz) con parpadeo visible + vibración */
    private void handleSignalsClick(Context context, SharedPreferences prefs, ImageView ivSignals) {
        boolean sonidoEnabled = prefs.getBoolean("swSonido", true);

        ivSignals.setImageTintList(ContextCompat.getColorStateList(context, R.color.aviso));
        startColorBlinkAnimation(ivSignals, context);
        startVibration(context);

        if (sonidoEnabled) {
            String titulo = "Alertas activadas";
            String mensaje = "Se han activado las alertas luminosas y sonoras.";
            String fecha = new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault()).format(new Date());

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_info)
                    .setContentTitle(titulo)
                    .setContentText(mensaje)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SENALES, builder.build());
            NotificacionRepository.getInstance().addNotificacion(new Notificacion(titulo, mensaje, fecha, R.drawable.ic_sonido));
        }

        if (signalsStopHandler == null) signalsStopHandler = new Handler(Looper.getMainLooper());
        if (signalsStopRunnable != null) signalsStopHandler.removeCallbacks(signalsStopRunnable);

        signalsStopRunnable = () -> {
            stopBlinkAnimation();
            if (vibrator != null) {
                try { vibrator.cancel(); } catch (Exception ignored) {}
                vibrator = null;
            }
            ivSignals.setColorFilter(ContextCompat.getColor(context, R.color.texto_oscuro));
        };
        signalsStopHandler.postDelayed(signalsStopRunnable, 10_000);
    }

    /** Parpadeo (amarillo ↔ gris oscuro) */
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

    private void startVibration(Context context) {
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
            try { signalsBlinkAnimator.cancel(); } catch (Exception ignored) {}
            signalsBlinkAnimator = null;
        }
    }

    private void stopSignalsEffects() {
        stopBlinkAnimation();
        if (signalsStopHandler != null && signalsStopRunnable != null) {
            signalsStopHandler.removeCallbacks(signalsStopRunnable);
            signalsStopRunnable = null;
        }
        if (vibrator != null) {
            try { vibrator.cancel(); } catch (Exception ignored) {}
            vibrator = null;
        }
    }

    private void updateLockUi(FrameLayout flLock, ImageView ivLock, TextView tvLockState) {
        if (isLocked) {
            ivLock.setImageResource(R.drawable.ic_candado_cerrado);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.error));
            tvLockState.setText(R.string.bloqueado);
        } else {
            ivLock.setImageResource(R.drawable.ic_candado_abierto);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.verdeoscuro));
            tvLockState.setText(R.string.puertas_abiertas);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }
}
