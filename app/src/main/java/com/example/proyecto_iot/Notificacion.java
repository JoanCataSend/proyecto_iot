package com.example.proyecto_iot;

public class Notificacion {
    private String titulo;
    private String mensaje;
    private String fecha;
    private int icono; // id del drawable

    // Filtros
    private final String carName;
    private final String category;     // "puerta" | "impacto" | "alerta_sonido" | ...
    private final long timestamp;

    public Notificacion(String titulo, String mensaje, String fecha, int icono, String carName, String category, long timestamp) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.icono = icono;
        this.carName = carName;
        this.category = category;
        this.timestamp = timestamp;
    }

    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public String getFecha() { return fecha; }
    public int getIcono() { return icono; }
    public String getCarName() { return carName; }
    public String getCategory() { return category; }
    public long getTimestamp() { return timestamp; }
}

