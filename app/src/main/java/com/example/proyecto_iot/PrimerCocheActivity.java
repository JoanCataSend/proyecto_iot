package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iot.utils.CustomToast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PrimerCocheActivity extends AppCompatActivity {

    private EditText etMarca, etModelo, etMatricula, etNombreCoche;
    private Button btnRegistrar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_coche_moderno);

        etMarca = findViewById(R.id.etMarca);
        etModelo = findViewById(R.id.etModelo);
        etMatricula = findViewById(R.id.etMatricula);
        etNombreCoche = findViewById(R.id.etNombreCoche);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnRegistrar.setOnClickListener(v -> guardarPrimerCoche());
    }

    private void guardarPrimerCoche() {
        String marca = etMarca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String matricula = etMatricula.getText().toString().trim();
        String nombre = etNombreCoche.getText().toString().trim();

        // ===== VALIDACIONES GENERALES =====
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

        if (!esMatriculaValida(matricula)) {
            etMatricula.setError("Formato inválido. Debe ser 4 números y 3 letras (ej. 1234 DKB)");
            etMatricula.requestFocus();
            return;
        }

        if (nombre.isEmpty()) {
            etNombreCoche.setError("El campo Nombre del coche es obligatorio");
            etNombreCoche.requestFocus();
            return;
        }

        // Normalizar matrícula → 1234DKB
        matricula = matricula.replaceAll("\\s+", "").toUpperCase();

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            CustomToast.error(this, "Error: usuario no autenticado");
            return;
        }

        // ===== CREAR OBJETO =====
        Map<String, Object> coche = new HashMap<>();
        coche.put("Marca", marca);
        coche.put("Modelo", modelo);
        coche.put("Matrícula", matricula);
        coche.put("Nombre", nombre);
        coche.put("Propietario", Collections.singletonList(userId));

        // ===== GUARDAR EN FIRESTORE =====
        db.collection("Coches")
                .add(coche)
                .addOnSuccessListener(documentReference -> {
                    CustomToast.success(this, "Coche registrado correctamente");
                    irAPaginaPrincipal();
                })
                .addOnFailureListener(e ->
                        CustomToast.error(this, "Error al registrar coche: " + e.getMessage())
                );
    }


    private void irAPaginaPrincipal() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private boolean esMatriculaValida(String mat) {
        if (mat == null || mat.isEmpty()) return false;
        mat = mat.trim().toUpperCase();
        String patron = "^[0-9]{4}\\s?[A-Z]{3}$";
        return mat.matches(patron);
    }

}
