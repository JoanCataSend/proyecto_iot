package com.example.proyecto_iot;

public class Notificacion {
    private String titulo;
    private String mensaje;
    private String fecha;
    private int icono; // id del drawable

    public Notificacion(String titulo, String mensaje, String fecha, int icono) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.icono = icono;
    }

    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public String getFecha() { return fecha; }
    public int getIcono() { return icono; }
}

