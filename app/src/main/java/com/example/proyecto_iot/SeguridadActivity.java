package com.example.proyecto_iot;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SeguridadActivity extends AppCompatActivity {

    private CheckBox checkPuertas, checkVentanas, checkMovimiento, checkAlarma, checkLuces, checkBloqueo;
    private Button btnGuardar;
    private ImageView btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguridad);

        // Inicializar vistas
        initViews();

        // Configurar listeners
        setupListeners();

        // Cargar configuración actual
        cargarConfiguracionActual();
    }

    private void initViews() {
        checkPuertas = findViewById(R.id.checkPuertas);
        checkVentanas = findViewById(R.id.checkVentanas);
        checkMovimiento = findViewById(R.id.checkMovimiento);
        checkAlarma = findViewById(R.id.checkAlarma);
        checkLuces = findViewById(R.id.checkLuces);
        checkBloqueo = findViewById(R.id.checkBloqueo);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnVolver = findViewById(R.id.btnVolver);
    }

    private void setupListeners() {
        // Botón de retroceso
        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Botón Guardar cambios
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarCambios();
            }
        });
    }

    private void cargarConfiguracionActual() {
        // Cargar configuración desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("config_seguridad", MODE_PRIVATE);

        checkPuertas.setChecked(prefs.getBoolean("sensor_puertas", true));
        checkVentanas.setChecked(prefs.getBoolean("sensor_ventanas", true));
        checkMovimiento.setChecked(prefs.getBoolean("sensor_movimiento", true));
        checkAlarma.setChecked(prefs.getBoolean("alarma_sonora", true));
        checkLuces.setChecked(prefs.getBoolean("luces_aviso", true));
        checkBloqueo.setChecked(prefs.getBoolean("bloqueo_remoto", true));
    }

    private void guardarCambios() {
        // Obtener el estado actual de todos los checkboxes
        boolean puertas = checkPuertas.isChecked();
        boolean ventanas = checkVentanas.isChecked();
        boolean movimiento = checkMovimiento.isChecked();
        boolean alarma = checkAlarma.isChecked();
        boolean luces = checkLuces.isChecked();
        boolean bloqueo = checkBloqueo.isChecked();

        // Guardar en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("config_seguridad", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("sensor_puertas", puertas);
        editor.putBoolean("sensor_ventanas", ventanas);
        editor.putBoolean("sensor_movimiento", movimiento);
        editor.putBoolean("alarma_sonora", alarma);
        editor.putBoolean("luces_aviso", luces);
        editor.putBoolean("bloqueo_remoto", bloqueo);
        editor.apply();

        // Mostrar mensaje de confirmación
        Toast.makeText(this, "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
    }
}