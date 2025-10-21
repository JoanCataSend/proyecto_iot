package com.example.proyecto_iot;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Activa el modo "Edge to Edge" (diseño hasta los bordes)
        EdgeToEdge.enable(this);

        // Carga el layout 'intento.xml'
        setContentView(R.layout.intento);

        // Ajuste para respetar los márgenes del sistema (barras de estado, navegación, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            // Obtenemos los márgenes de las barras del sistema (barra de estado, barra de navegación)
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); // Cambié el tipo a 'Insets'
            // Aplicamos los márgenes de las barras del sistema
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ---------- Configuración del Spinner ----------
        Spinner spinner = findViewById(R.id.spinnerCars);

        // Obtiene la lista de coches definida en strings.xml
        String[] carList = getResources().getStringArray(R.array.lista_coches);

        // Crea un adaptador para mostrar los coches en el Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                carList
        );

        // Define el diseño del menú desplegable
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Asigna el adaptador al Spinner
        spinner.setAdapter(adapter);
    }
}
