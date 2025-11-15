package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText nombreUsuarioEditText, emailEditText, passwordEditText, repeatPasswordEditText;
    private ImageView togglePassword, toggleRepeatPassword;
    private Button btnContinuar, btnLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isPasswordVisible = false;
    private boolean isRepeatPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nombreUsuarioEditText = findViewById(R.id.nombreUsuario);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        repeatPasswordEditText = findViewById(R.id.repeatPassword);
        togglePassword = findViewById(R.id.imageViewTogglePassword);
        toggleRepeatPassword = findViewById(R.id.imageViewToggleRepeatPassword);
        btnContinuar = findViewById(R.id.btnContinuar);
        btnLogin = findViewById(R.id.btnLogin);

        // Mostrar / Ocultar contraseña
        togglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_open);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        // Mostrar / Ocultar repetir contraseña
        toggleRepeatPassword.setOnClickListener(v -> {
            if (isRepeatPasswordVisible) {
                repeatPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleRepeatPassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                repeatPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleRepeatPassword.setImageResource(R.drawable.ic_eye_open);
            }
            repeatPasswordEditText.setSelection(repeatPasswordEditText.getText().length());
            isRepeatPasswordVisible = !isRepeatPasswordVisible;
        });

        btnContinuar.setOnClickListener(v -> registrarUsuario());

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registrarUsuario() {
        String nombreUsuario = nombreUsuarioEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String repeatPassword = repeatPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(nombreUsuario) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(repeatPassword)) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(repeatPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            guardarUsuarioEnFirestore(user, nombreUsuario, email);
                        }

                        // Enviar correo de verificación
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            Toast.makeText(this,
                                                    "Registro exitoso 🎉. Verifica tu correo antes de iniciar sesión.",
                                                    Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                            startActivity(new Intent(this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(this,
                                                    "Error al enviar correo de verificación: "
                                                            + verificationTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this,
                                "Error al registrar: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarUsuarioEnFirestore(FirebaseUser user, String nombre, String email) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("Usuario", nombre);
        usuario.put("Correo", email);
        usuario.put("Imagen", "/foto/so"); // valor por defecto o cambia según tu lógica
        usuario.put("FechaRegistro", FieldValue.serverTimestamp());

        db.collection("Usuarios")
                .document(user.getUid())
                .set(usuario)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Usuario guardado en Firestore", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
