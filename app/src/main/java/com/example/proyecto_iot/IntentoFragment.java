package com.example.proyecto_iot;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

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
    private static final String PREFS_NAME = "notificaciones_prefs";
    private static final String PREF_KEY_LOCKED = "lock_state_locked";

    private boolean isLocked = true;

    // Referencias UI para el spinner y la imagen del coche
    private Spinner spinnerCars;
    private ImageView ivCar;

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

        // Spinner de coches y foto principal
        spinnerCars = view.findViewById(R.id.spinnerCars);
        ivCar = view.findViewById(R.id.ivCar);

        // Contenedor completo del "spinner" (tarjeta con iconos y flecha)
        LinearLayout vehicleSelector = view.findViewById(R.id.vehicleSelector);
        if (vehicleSelector != null) {
            vehicleSelector.setOnClickListener(v -> {
                if (spinnerCars != null) {
                    // Abrimos el desplegable al pulsar en cualquier parte de la tarjeta
                    spinnerCars.performClick();
                }
            });
        }

        if (spinnerCars != null) {
            Context context = requireContext();

            // Adaptador usando el string-array lista_coches (strings.xml)
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    context,
                    R.array.lista_coches,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCars.setAdapter(adapter);

            // Listener para cambiar la imagen del coche según el item seleccionado
            spinnerCars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    updateCarImage(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // No hacemos nada
                }
            });

            // Imagen inicial en función del elemento seleccionado por defecto (normalmente posición 0)
            updateCarImage(spinnerCars.getSelectedItemPosition());
        }

        // Bloque del candado / bloqueo de puertas
        FrameLayout flLock = view.findViewById(R.id.flLock);
        ImageView ivLock = view.findViewById(R.id.ivLock);
        TextView tvLockState = view.findViewById(R.id.tvLockState);

        if (flLock != null && ivLock != null && tvLockState != null) {
            Context context = requireContext();
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            // Estado guardado de bloqueo/desbloqueo
            isLocked = prefs.getBoolean(PREF_KEY_LOCKED, true);
            updateLockUi(flLock, ivLock, tvLockState);

            View.OnClickListener lockClickListener =
                    v -> handleLockClick(context, prefs, flLock, ivLock, tvLockState);

            flLock.setOnClickListener(lockClickListener);
            ivLock.setOnClickListener(lockClickListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // En el Home nunca mostramos la flecha atrás
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }
    }

    /** Cambia la imagen del coche según la posición del spinner */
    private void updateCarImage(int position) {
        if (ivCar == null) return;

        String drawableName;
        switch (position) {
            case 0:
                drawableName = "coche_naranja";
                break;
            case 1:
                drawableName = "coche_azul"; // Asegúrate de tener este drawable
                break;
            case 2:
                drawableName = "coche_negro"; // Y este también
                break;
            default:
                drawableName = "coche_naranja";
                break;
        }

        Context context = requireContext();
        int resId = context.getResources()
                .getIdentifier(drawableName, "drawable", context.getPackageName());

        if (resId != 0) {
            ivCar.setImageResource(resId);
        } else {
            // Fallback seguro si algún drawable no existe
            int fallbackResId = context.getResources()
                    .getIdentifier("coche_naranja", "drawable", context.getPackageName());
            if (fallbackResId != 0) {
                ivCar.setImageResource(fallbackResId);
            }
        }
    }

    /**
     * Maneja el click sobre el candado:
     * - Cambia estado bloqueado/desbloqueado
     * - Guarda en SharedPreferences
     * - Si swPuertasAbiertas está activado -> lanza notificación del sistema y guarda en NotificacionRepository
     * - Actualiza el estado visual (color, icono, texto)
     */
    private void handleLockClick(Context context,
                                 SharedPreferences prefs,
                                 FrameLayout flLock,
                                 ImageView ivLock,
                                 TextView tvLockState) {

        // Preferencia de "Puertas abiertas" de Ajustes → swPuertasAbiertas
        // Esta clave la guarda la ConfigNotificacionesActivity/Fragment en el SharedPreferences "notificaciones_prefs"
        boolean puertasAbiertasEnabled = prefs.getBoolean("swPuertasAbiertas", true);

        // Cambiamos el estado y lo persistimos
        isLocked = !isLocked;
        prefs.edit().putBoolean(PREF_KEY_LOCKED, isLocked).apply();

        String titulo;
        String mensaje;

        if (isLocked) {
            // Estado tras el click: BLOQUEADO
            titulo = getString(R.string.bloqueado);
            mensaje = "Las puertas del coche se han bloqueado correctamente desde la app.";
        } else {
            // Estado tras el click: DESBLOQUEADO / PUERTAS ABIERTAS
            titulo = getString(R.string.puertas_abiertas);
            mensaje = "Las puertas del coche se han desbloqueado correctamente desde la app.";
        }

        // Fecha/hora para guardar en el histórico de notificaciones
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault());
        String fecha = sdf.format(new Date());

        // Sólo enviamos notificación si el usuario lo tiene activado en ajustes
        if (puertasAbiertasEnabled) {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_info)
                            .setContentTitle(titulo)
                            .setContentText(mensaje)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID_PUERTAS, builder.build());

            // Guardamos también en el repositorio de notificaciones para mostrar en la lista
            Notificacion notificacion = new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info);
            NotificacionRepository.getInstance().addNotificacion(notificacion);
        }

        // Actualizamos el estado visual del candado
        updateLockUi(flLock, ivLock, tvLockState);
    }

    /** Actualiza colores, icono y texto del candado según isLocked */
    private void updateLockUi(FrameLayout flLock,
                              ImageView ivLock,
                              TextView tvLockState) {
        if (isLocked) {
            // Estado BLOQUEADO → icono cerrado, color error (rojo), texto "Bloqueado"
            flLock.setBackgroundResource(R.drawable.bg_card_soft);
            ivLock.setImageResource(R.drawable.ic_candado_cerrado);
            ivLock.setImageTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.error)
            );
            tvLockState.setText(R.string.bloqueado);
        } else {
            // Estado DESBLOQUEADO → icono abierto, color verde, texto "Puertas abiertas"
            flLock.setBackgroundResource(R.drawable.bg_card_soft);
            ivLock.setImageResource(R.drawable.ic_candado_abierto);
            ivLock.setImageTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.verdeoscuro)
            );
            tvLockState.setText(R.string.puertas_abiertas);
        }
    }

    /** Crea el canal de notificaciones para Android 8+ */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = requireContext();
            CharSequence name = "Alertas del vehículo";
            String description = "Notificaciones relacionadas con el estado del coche";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
