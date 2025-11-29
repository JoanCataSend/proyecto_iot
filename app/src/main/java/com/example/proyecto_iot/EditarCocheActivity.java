package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iot.utils.CustomToast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

public class EditarCocheActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String cocheId;

    private EditText etMarca, etModelo, etMatricula, etNombre;

    private Button btnGuardarCambios, btnEliminarCoche;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar_coche);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etNombre = findViewById(R.id.etNombreCocheEditar);
        etMarca = findViewById(R.id.etMarcaEditar);
        etModelo = findViewById(R.id.etModeloEditar);
        etMatricula = findViewById(R.id.etMatriculaEditar);

        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnEliminarCoche = findViewById(R.id.btnEliminarCoche);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        cocheId = getIntent().getStringExtra("cocheId");
        String marca = getIntent().getStringExtra("marcaCoche");
        String modelo = getIntent().getStringExtra("modeloCoche");
        String matricula = getIntent().getStringExtra("matriculaCoche");
        String nombre = getIntent().getStringExtra("nombreCoche");

        etMarca.setText(marca);
        etModelo.setText(modelo);
        etMatricula.setText(matricula);
        etNombre.setText(nombre);

        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
        btnEliminarCoche.setOnClickListener(v -> eliminarCoche());
    }

    // =====================================================
    private void guardarCambios() {

        String marca = etMarca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String matricula = etMatricula.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();

        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            CustomToast.warning(this, "Debes rellenar todos los campos");
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
                    CustomToast.success(this, "Cambios guardados");

                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        CustomToast.error(this, "Error al guardar los cambios")
                );
    }

    // =====================================================
    private void eliminarCoche() {

        db.collection("Coches").document(cocheId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    CustomToast.success(this, "Coche eliminado con éxito");
                    finish();
                })
                .addOnFailureListener(e ->
                        CustomToast.error(this, "Error al borrar el coche")
                );
    }
}
