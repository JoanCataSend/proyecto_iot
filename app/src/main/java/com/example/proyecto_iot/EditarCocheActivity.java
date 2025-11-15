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

    private EditText etMarca, etModelo, etMatricula, etNombre; // Campos

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
        etNombre = findViewById(R.id.etNombreCocheEditar);
        etMarca = findViewById(R.id.etMarcaEditar);
        etModelo = findViewById(R.id.etModeloEditar);
        etMatricula = findViewById(R.id.etMatriculaEditar);

        // --- BOTONES ---
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnEliminarCoche = findViewById(R.id.btnEliminarCoche);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // --- RECIBIR DATOS DEL COCHE ---
        cocheId = getIntent().getStringExtra("cocheId");
        String marca = getIntent().getStringExtra("marcaCoche");
        String modelo = getIntent().getStringExtra("modeloCoche");
        String matricula = getIntent().getStringExtra("matriculaCoche");
        String nombre = getIntent().getStringExtra("nombreCoche");

        // Ponerlos en pantalla
        etMarca.setText(marca);
        etModelo.setText(modelo);
        etMatricula.setText(matricula);
        etNombre.setText(nombre);

        // Volver atrás
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Guardar cambios
        btnGuardarCambios.setOnClickListener(v -> guardarCambios());

        // Eliminar coche
        btnEliminarCoche.setOnClickListener(v -> eliminarCoche());
    }

    private void guardarCambios() {
        String marca = etMarca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String matricula = etMatricula.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();

        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(this, "Debes rellenar todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Coches").document(cocheId)
                .update(
                        "Marca", marca,
                        "Modelo", modelo,
                        "Matrícula", matricula,
                        "Nombre", nombre
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();

                    // Devolver resultado OK a la actividad anterior
                    setResult(RESULT_OK);

                    // Cerrar esta pantalla y volver
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar los cambios", Toast.LENGTH_SHORT).show()
                );
    }

    private void eliminarCoche() {
        db.collection("Coches").document(cocheId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Coche eliminado con éxito", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al borrar el coche", Toast.LENGTH_SHORT).show()
                );
    }
}
