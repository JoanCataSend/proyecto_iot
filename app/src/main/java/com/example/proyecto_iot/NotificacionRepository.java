package com.example.proyecto_iot;

import java.util.ArrayList;
import java.util.List;

public class NotificacionRepository {

    private static NotificacionRepository instance;

    private final List<Notificacion> notificaciones;

    private NotificacionRepository() {
        notificaciones = new ArrayList<>();

        // Datos de ejemplo iniciales, iguales a los que había en NotificacionesFragment
        notificaciones.add(new Notificacion(
                "Ventana rota detectada",
                "El sensor del coche 'El Champon' ha detectado una vibración fuerte en la ventana delantera.",
                "18:42  12/10/25",
                R.drawable.ic_info
        ));
        notificaciones.add(new Notificacion(
                "Puertas desbloqueadas",
                "Las puertas del coche 'Speedy Azul' se han abierto correctamente desde la app.",
                "17:58  12/10/25",
                R.drawable.ic_info
        ));
    }

    public static synchronized NotificacionRepository getInstance() {
        if (instance == null) {
            instance = new NotificacionRepository();
        }
        return instance;
    }

    public synchronized void addNotificacion(Notificacion notificacion) {
        if (notificacion == null) {
            return;
        }
        notificaciones.add(0, notificacion);
    }

    public synchronized List<Notificacion> getNotificaciones() {
        return new ArrayList<>(notificaciones);
    }
}
