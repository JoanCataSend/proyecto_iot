package com.example.proyecto_iot;

public class CapturaItem {
    private final String url;
    private final String nombre;

    public CapturaItem(String url, String nombre) {
        this.url = url;
        this.nombre = nombre;
    }

    public String getUrl() {
        return url;
    }

    public String getNombre() {
        return nombre;
    }
}
