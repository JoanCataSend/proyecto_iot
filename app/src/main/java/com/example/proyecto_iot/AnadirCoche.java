package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

        // 🔹 MOSTRAR Y CONFIGURAR HEADER (Porque es Activity)
        View header = findViewById(R.id.header_incluido);
        if (header != null) {
            header.setVisibility(View.VISIBLE);
            ImageButton btnBack = header.findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setVisibility(View.VISIBLE);
                btnBack.setOnClickListener(v -> finish());
            }
        }

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

        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            CustomToast.warning(this, "Rellena todos los campos");
            return;
        }

        if (!esMatriculaValida(matricula)) {
            etMatricula.setError("Formato inválido");
            return;
        }

        matricula = matricula.replaceAll("\\s+", "");
        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> coche = new HashMap<>();
        coche.put("Marca", marca);
        coche.put("Modelo", modelo);
        coche.put("Matrícula", matricula);
        coche.put("Nombre", nombre);
        coche.put("Propietario", java.util.Collections.singletonList(uid));

        db.collection("Coches").add(coche).addOnSuccessListener(ref -> {
            CustomToast.success(this, "Coche añadido");
            setResult(RESULT_OK);
            finish();
        });
    }

    private boolean esMatriculaValida(String mat) {
        return mat.matches("^[0-9]{4}\\s?[A-Z]{3}$");
    }
}