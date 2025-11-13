package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        setContentView(R.layout.registro_coche);

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

        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> coche = new HashMap<>();
        coche.put("Marca", marca);
        coche.put("Modelo", modelo);
        coche.put("Matrícula", matricula.toUpperCase());
        coche.put("Nombre", nombre);
        coche.put("Propietario", Collections.singletonList(userId));

        db.collection("Coches")
                .add(coche)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Coche registrado correctamente", Toast.LENGTH_SHORT).show();
                    irAPaginaPrincipal();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al registrar coche: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void irAPaginaPrincipal() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
