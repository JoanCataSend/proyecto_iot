package com.example.proyecto_iot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegistroCoche extends AppCompatActivity {

    private EditText etMarca, etModelo, etMatricula, etNombreCoche;
    private Button btnRegistrar;

    // Variables donde se guardarán los datos
    private String marca, modelo, matricula, nombreCoche;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_coche);

        // Referenciar los elementos del layout
        etMarca = findViewById(R.id.etMarca);
        etModelo = findViewById(R.id.etModelo);
        etMatricula = findViewById(R.id.etMatricula);
        etNombreCoche = findViewById(R.id.etNombreCoche);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        // Cargar datos guardados anteriormente (si existen)
        cargarDatos();

        btnRegistrar.setOnClickListener(v -> {
            // Obtener textos y limpiar espacios
            marca = etMarca.getText().toString().trim();
            modelo = etModelo.getText().toString().trim();
            matricula = etMatricula.getText().toString().trim();
            nombreCoche = etNombreCoche.getText().toString().trim();

            // Verificar campos vacíos
            if (marca.isEmpty()) {
                etMarca.setError("El campo Marca es obligatorio");
                etMarca.requestFocus();
                return;
            }
            if (modelo.isEmpty()) {
                etModelo.setError("El campo Modelo es obligatorio");
                etModelo.requestFocus();
                return;
            }
            if (matricula.isEmpty()) {
                etMatricula.setError("El campo Matrícula es obligatorio");
                etMatricula.requestFocus();
                return;
            }
            if (nombreCoche.isEmpty()) {
                etNombreCoche.setError("El campo Nombre del coche es obligatorio");
                etNombreCoche.requestFocus();
                return;
            }

            // Validar formato de matrícula
            if (!esMatriculaValida(matricula)) {
                etMatricula.setError("Formato inválido. Debe ser 4 números y 3 letras (ej. 1234DKB o 1234 DKB).");
                etMatricula.requestFocus();
                return;
            }

            // Si el metodo está correcto → guardar datos
            matricula = matricula.replaceAll("\\s+", "").toUpperCase();
            guardarDatos();

            Toast.makeText(this, "Vehículo registrado correctamente", Toast.LENGTH_SHORT).show();
        });
    }

    // Valida que la matrícula tenga 4 números y 3 letras (con o sin espacio)
    private boolean esMatriculaValida(String mat) {
        if (TextUtils.isEmpty(mat)) return false;
        String patron = "^[0-9]{4}\\s?[A-Za-z]{3}$";
        return mat.matches(patron);
    }

    // Guarda los datos de forma persistente
    private void guardarDatos() {
        SharedPreferences prefs = getSharedPreferences("vehiculo_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("marca", marca);
        editor.putString("modelo", modelo);
        editor.putString("matricula", matricula);
        editor.putString("nombreCoche", nombreCoche);
        editor.apply();
    }

    // Carga los datos si existen
    private void cargarDatos() {
        SharedPreferences prefs = getSharedPreferences("vehiculo_prefs", MODE_PRIVATE);
        etMarca.setText(prefs.getString("marca", ""));
        etModelo.setText(prefs.getString("modelo", ""));
        etMatricula.setText(prefs.getString("matricula", ""));
        etNombreCoche.setText(prefs.getString("nombreCoche", ""));
    }
}
