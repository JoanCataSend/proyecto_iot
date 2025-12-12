package com.example.proyecto_iot;

import android.app.Application;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar sistema de precarga
        ImagePreloader.init(this);
    }
}
