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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IntentoFragment extends Fragment {

    private static final String CHANNEL_ID = "alertas_vehiculo";
    private static final int NOTIFICATION_ID_PUERTAS = 1001;
    private static final String PREFS_NAME = "notificaciones_prefs";
    private static final String PREF_KEY_LOCKED = "lock_state_locked";
    private boolean isLocked = true;

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

        FrameLayout flLock = view.findViewById(R.id.flLock);
        ImageView ivLock = view.findViewById(R.id.ivLock);
        TextView tvLockState = view.findViewById(R.id.tvLockState);

        if (flLock != null && ivLock != null && tvLockState != null) {
            Context context = requireContext();
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            isLocked = prefs.getBoolean(PREF_KEY_LOCKED, true);
            updateLockUi(flLock, ivLock, tvLockState);

            View.OnClickListener lockClickListener = v ->
                    handleLockClick(context, prefs, flLock, ivLock, tvLockState);

            flLock.setOnClickListener(lockClickListener);
            ivLock.setOnClickListener(lockClickListener);
        }
    }

    private void handleLockClick(Context context,
                                 SharedPreferences prefs,
                                 FrameLayout flLock,
                                 ImageView ivLock,
                                 TextView tvLockState) {

        boolean puertasAbiertasEnabled = prefs.getBoolean("swPuertasAbiertas", true);

        isLocked = !isLocked;
        prefs.edit().putBoolean(PREF_KEY_LOCKED, isLocked).apply();

        String titulo;
        String mensaje;

        if (isLocked) {
            titulo = getString(R.string.bloqueado);
            mensaje = "Las puertas del coche se han bloqueado correctamente desde la app.";
        } else {
            titulo = getString(R.string.puertas_abiertas);
            mensaje = "Las puertas del coche se han desbloqueado correctamente desde la app.";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault());
        String fecha = sdf.format(new Date());

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

            Notificacion notificacion = new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info);
            NotificacionRepository.getInstance().addNotificacion(notificacion);
        }

        updateLockUi(flLock, ivLock, tvLockState);
    }

    private void updateLockUi(FrameLayout flLock,
                              ImageView ivLock,
                              TextView tvLockState) {
        if (isLocked) {
            flLock.setBackgroundResource(R.drawable.bg_circle_rojo);
            ivLock.setImageResource(R.drawable.ic_candado_cerrado);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.error));
            tvLockState.setText(R.string.bloqueado);
        } else {
            flLock.setBackgroundResource(R.drawable.bg_circle_verde);
            ivLock.setImageResource(R.drawable.ic_candado_abierto);
            ivLock.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.bien));
            tvLockState.setText(R.string.puertas_abiertas);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = requireContext();
            CharSequence name = "Alertas del vehículo";
            String description = "Notificaciones relacionadas con el estado del coche";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
