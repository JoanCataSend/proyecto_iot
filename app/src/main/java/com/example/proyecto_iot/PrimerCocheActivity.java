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

        // ===================== VALIDACIONES =====================

        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            CustomToast.warning(this, "Por favor, completa todos los campos");
            return;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            CustomToast.error(this, "Error: usuario no autenticado");
            return;
        }

        // ===================== CREACIÓN OBJETO =====================

        Map<String, Object> coche = new HashMap<>();
        coche.put("Marca", marca);
        coche.put("Modelo", modelo);
        coche.put("Matrícula", matricula.toUpperCase());
        coche.put("Nombre", nombre);
        coche.put("Propietario", Collections.singletonList(userId));

        // ===================== FIRESTORE =====================

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
}
