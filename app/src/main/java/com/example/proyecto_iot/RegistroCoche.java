package com.example.proyecto_iot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iot.utils.CustomToast;

public class RegistroCoche extends AppCompatActivity {

    private EditText etMarca, etModelo, etMatricula, etNombreCoche;
    private Button btnRegistrar;

    private String marca, modelo, matricula, nombreCoche;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_coche);

        etMarca = findViewById(R.id.etMarcaEditar);
        etModelo = findViewById(R.id.etModelo);
        etMatricula = findViewById(R.id.etMatricula);
        etNombreCoche = findViewById(R.id.etNombreCoche);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        cargarDatos();

        btnRegistrar.setOnClickListener(v -> {

            marca = etMarca.getText().toString().trim();
            modelo = etModelo.getText().toString().trim();
            matricula = etMatricula.getText().toString().trim();
            nombreCoche = etNombreCoche.getText().toString().trim();

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

            if (!esMatriculaValida(matricula)) {
                etMatricula.setError("Formato inválido. Debe ser 4 números y 3 letras (ej. 1234DKB o 1234 DKB).");
                etMatricula.requestFocus();
                return;
            }

            matricula = matricula.replaceAll("\\s+", "").toUpperCase();
            guardarDatos();

            CustomToast.success(this, "Vehículo registrado correctamente");
        });
    }

    private boolean esMatriculaValida(String mat) {
        if (TextUtils.isEmpty(mat)) return false;
        String patron = "^[0-9]{4}\\s?[A-Za-z]{3}$";
        return mat.matches(patron);
    }

    private void guardarDatos() {
        SharedPreferences prefs = getSharedPreferences("vehiculo_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("marca", marca);
        editor.putString("modelo", modelo);
        editor.putString("matricula", matricula);
        editor.putString("nombreCoche", nombreCoche);
        editor.apply();
    }

    private void cargarDatos() {
        SharedPreferences prefs = getSharedPreferences("vehiculo_prefs", MODE_PRIVATE);
        etMarca.setText(prefs.getString("marca", ""));
        etModelo.setText(prefs.getString("modelo", ""));
        etMatricula.setText(prefs.getString("matricula", ""));
        etNombreCoche.setText(prefs.getString("nombreCoche", ""));
    }
}
