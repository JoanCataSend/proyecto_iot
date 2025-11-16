package com.example.proyecto_iot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CuentaYPerfil extends AppCompatActivity {

    private EditText etNombre, etCorreo, etContrasena;
    private ImageView fotoPerfil;
    private ImageButton btnBack, btnEditarFoto;
    private Button btnCerrarSesion, btnGuardar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cuentayperfil);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        // Enlazar vistas
        fotoPerfil = findViewById(R.id.imageView2);
        btnBack = findViewById(R.id.btnBack);
        btnEditarFoto = findViewById(R.id.btnEditarFoto);

        etNombre = findViewById(R.id.etModeloEditar);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);

        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Volver atrás
        btnBack.setOnClickListener(v -> finish());
        cargarDatosUsuario();

        // Guardar cambios
        btnGuardar.setOnClickListener(v -> guardarCambios());

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> {
            auth.signOut();
            finish();
        });
    }

    private void cargarDatosUsuario() {
        if (user == null) return;

        // Correo directamente desde FirebaseAuth
        etCorreo.setText(user.getEmail());

        // Traer nombre desde Firestore
        db.collection("Usuarios")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String nombre = task.getResult().getString("nombre"); // <-- clave que usamos en Firestore
                        if (nombre != null) {
                            etNombre.setText(nombre);
                        }
                    } else {
                        Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void guardarCambios() {
        if (user == null) return;

        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevaContrasena = etContrasena.getText().toString().trim();

        // Guardar nombre en Firestore
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nuevoNombre);

        db.collection("Usuarios")
                .document(user.getUid())
                .update(datos)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                );

        // Cambiar contraseña si hay algo
        if (!nuevaContrasena.isEmpty()) {
            user.updatePassword(nuevaContrasena)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "No se pudo cambiar la contraseña", Toast.LENGTH_SHORT).show()
                    );
        }
    }
}
