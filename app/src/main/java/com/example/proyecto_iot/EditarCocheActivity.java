package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import com.google.firebase.auth.FirebaseAuth;

public class EditarCocheActivity extends AppCompatActivity {
    // Para la base de datos
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String cocheId; // el id del coche k se va a editar

    // Campos
    private EditText etMarca, etModelo, etMatricula, etNombre;

    // Botones
    private Button btnGuardarCambios, btnEliminarCoche;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar_coche);

        // Para la bbdd
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // --- ENLAZAR VISTAS ---
        etNombre = findViewById(R.id.etNombreE);
        etMarca = findViewById(R.id.etMarca);
        etModelo = findViewById(R.id.etModeloE);
        etMatricula = findViewById(R.id.etMatriculaE);

        // --- BOTONES ---
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnEliminarCoche = findViewById(R.id.btnEliminarCoche);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // --- RECIBIR DATOS DEL COCHE ---
        Intent intent = getIntent();
        if (intent != null) {
            cocheId = intent.getStringExtra("cocheId"); // Así sabemos qué coche editar/eliminar
            etNombre.setText(intent.getStringExtra("nombreCoche"));
            etMarca.setText(intent.getStringExtra("marcaCoche"));
            etModelo.setText(intent.getStringExtra("modeloCoche"));
            etMatricula.setText(intent.getStringExtra("matriculaCoche"));
        }

        // --- GUARDAR CAMBIOS ---
        btnGuardarCambios.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString();
            String marca = etMarca.getText().toString();
            String modelo = etModelo.getText().toString();
            String matricula = etMatricula.getText().toString();

            // Toast para que rellenes los campos
            if(nombre.isEmpty() || marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = auth.getCurrentUser().getUid();
            DocumentReference cocheRef = db.collection("Usuarios")
                    .document(uid)
                    .collection("Coches")
                    .document(cocheId);

            cocheRef.update(
                    "nombre", nombre,
                    "marca", marca,
                    "modelo", modelo,
                    "matricula", matricula
            ).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show();
                finish(); // cerrar actividad
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        // --- ELIMINAR COCHE ---
        btnEliminarCoche.setOnClickListener(v -> {
            String uid = auth.getCurrentUser().getUid();
            db.collection("Usuarios")
                    .document(uid)
                    .collection("Coches")
                    .document(cocheId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Coche eliminado", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
