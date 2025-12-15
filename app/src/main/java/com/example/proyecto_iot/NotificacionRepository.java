package com.example.proyecto_iot;

import java.util.ArrayList;
import java.util.List;

public class NotificacionRepository {

    private static NotificacionRepository instance;

    private final List<Notificacion> notificaciones;

    private NotificacionRepository() {
        notificaciones = new ArrayList<>();
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
