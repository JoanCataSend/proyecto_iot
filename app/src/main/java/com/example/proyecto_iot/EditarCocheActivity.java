package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditarCocheActivity extends AppCompatActivity {

    private EditText etMarca, etModelo, etMatricula, etNombre;
    private Button btnGuardarCambios, btnEliminarCoche;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar_coche);

        // --- ENLAZAR VISTAS ---
        etMarca = findViewById(R.id.etMarca);
        etModelo = findViewById(R.id.etModeloE);
        etMatricula = findViewById(R.id.etMatriculaE);
        etNombre = findViewById(R.id.etNombreE);

        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnEliminarCoche = findViewById(R.id.btnEliminarCoche);
        btnBack = findViewById(R.id.btnBack);

        // --- VOLVER ---
        btnBack.setOnClickListener(v -> finish());

        // --- RECIBIR DATOS DEL COCHE ---
        Intent intent = getIntent();
        if (intent != null) {
            String nombreCoche = intent.getStringExtra("nombreCoche");
            etNombre.setText(nombreCoche);
            // Aquí puedes cargar Marca, Modelo, Matrícula si los pasas también
        }

        // --- GUARDAR CAMBIOS ---
        btnGuardarCambios.setOnClickListener(v -> {
            String marca = etMarca.getText().toString();
            String modelo = etModelo.getText().toString();
            String matricula = etMatricula.getText().toString();
            String nombre = etNombre.getText().toString();

            // Aquí podrías guardar los datos en la base de datos o SharedPreferences
            Toast.makeText(EditarCocheActivity.this,
                    "Datos guardados:\n" + nombre + "\n" + marca + "\n" + modelo + "\n" + matricula,
                    Toast.LENGTH_SHORT).show();
        });

        // --- ELIMINAR COCHE ---
        btnEliminarCoche.setOnClickListener(v -> {
            // Aquí eliminarías el coche de tu base de datos
            Toast.makeText(EditarCocheActivity.this,
                    "Coche eliminado", Toast.LENGTH_SHORT).show();
            finish(); // cerrar actividad
        });
    }
}
