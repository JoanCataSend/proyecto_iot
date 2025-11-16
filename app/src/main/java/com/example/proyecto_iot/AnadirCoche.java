package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AnadirCoche extends AppCompatActivity {

    private EditText etMarca, etModelo, etMatricula, etNombre;
    private Button btnGuardar;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.anadir_coche);

        // BBDD
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // ENLAZAR VISTAS
        etMarca = findViewById(R.id.etMarcaEditar);
        etModelo = findViewById(R.id.etModeloEditar);
        etMatricula = findViewById(R.id.etMatriculaEditar);
        etNombre = findViewById(R.id.etNombreCocheEditar);

        btnGuardar = findViewById(R.id.btnGuardarCambios);
        btnBack = findViewById(R.id.btnBack);

        // VOLVER ATRÁS
        btnBack.setOnClickListener(v -> finish());

        // GUARDAR
        btnGuardar.setOnClickListener(v -> guardarCoche());
    }

    private void guardarCoche() {
        String marca = etMarca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String matricula = etMatricula.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();

        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Datos del coche
        Map<String, Object> coche = new HashMap<>();
        coche.put("Marca", marca);
        coche.put("Modelo", modelo);
        coche.put("Matrícula", matricula);
        coche.put("Nombre", nombre);
        coche.put("Propietario", java.util.Collections.singletonList(uid));

        // Guardar en Firestore
        db.collection("Coches")
                .add(coche)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Coche añadido correctamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al añadir un nuevo coche", Toast.LENGTH_SHORT).show()
                );
    }
}
