package com.example.proyecto_iot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iot.utils.CustomToast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AnadirCoche extends AppCompatActivity {

    private EditText etMarca, etModelo, etMatricula, etNombre;
    private Button btnGuardar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.anadir_coche);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etMarca = findViewById(R.id.etMarcaEditar);
        etModelo = findViewById(R.id.etModeloEditar);
        etMatricula = findViewById(R.id.etMatriculaEditar);
        etNombre = findViewById(R.id.etNombreCocheEditar);

        btnGuardar = findViewById(R.id.btnGuardarCambios);

        btnGuardar.setOnClickListener(v -> guardarCoche());
    }

    private void guardarCoche() {

        String marca = etMarca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String matricula = etMatricula.getText().toString().trim().toUpperCase();
        String nombre = etNombre.getText().toString().trim();

        // ===== VALIDACIONES =====
        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            CustomToast.warning(this, "Rellena todos los campos");
            return;
        }

        if (!esMatriculaValida(matricula)) {
            etMatricula.setError("Formato inválido. Ej: 1234 DKB");
            etMatricula.requestFocus();
            return;
        }

        // Normalizar
        matricula = matricula.replaceAll("\\s+", "");

        String uid = auth.getCurrentUser().getUid();
        String finalMatricula = matricula;

        // ===== COMPROBAR MATRÍCULA ÚNICA =====
        db.collection("Coches")
                .whereEqualTo("Matrícula", finalMatricula)
                .get()
                .addOnSuccessListener(query -> {

                    if (!query.isEmpty()) {
                        etMatricula.setError("Esta matrícula ya está registrada");
                        etMatricula.requestFocus();
                        CustomToast.error(this, "La matrícula ya existe");
                        return;
                    }

                    // Guardar coche
                    Map<String, Object> coche = new HashMap<>();
                    coche.put("Marca", marca);
                    coche.put("Modelo", modelo);
                    coche.put("Matrícula", finalMatricula);
                    coche.put("Nombre", nombre);
                    coche.put("Propietario", java.util.Collections.singletonList(uid));

                    db.collection("Coches")
                            .add(coche)
                            .addOnSuccessListener(ref -> {
                                CustomToast.success(this, "Coche añadido correctamente");
                                setResult(RESULT_OK);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    CustomToast.error(this, "Error al añadir un nuevo coche")
                            );
                })
                .addOnFailureListener(e ->
                        CustomToast.error(this, "Error comprobando matrícula")
                );
    }



    private boolean esMatriculaValida(String mat) {
        if (mat == null || mat.isEmpty()) return false;

        mat = mat.trim().toUpperCase(); // normalizar

        // Patrón → 4 dígitos + espacio opcional + 3 letras
        String patron = "^[0-9]{4}\\s?[A-Z]{3}$";

        return mat.matches(patron);
    }

}
