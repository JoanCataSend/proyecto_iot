package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText nombreUsuarioEditText, emailEditText, passwordEditText, repeatPasswordEditText;
    private Button btnContinuar, btnLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register); // tu XML se llama register.xml

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Referencias a las vistas
        nombreUsuarioEditText = findViewById(R.id.nombreUsuario);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        repeatPasswordEditText = findViewById(R.id.repeatPassword);
        btnContinuar = findViewById(R.id.btnContinuar);
        btnLogin = findViewById(R.id.btnLogin);

        // Acción para registrarse
        btnContinuar.setOnClickListener(v -> registrarUsuario());

        // Ir al login
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

        // Crear usuario en Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Enviar correo de verificación
                        mAuth.getCurrentUser().sendEmailVerification()
                                .addOnCompleteListener(verificationTask -> {
                                    if (verificationTask.isSuccessful()) {
                                        Toast.makeText(this,
                                                "Registro exitoso 🎉. Verifica tu correo antes de iniciar sesión.",
                                                Toast.LENGTH_LONG).show();
                                        mAuth.signOut(); // cerrar sesión hasta que verifique
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(this,
                                                "Error al enviar correo de verificación: "
                                                        + verificationTask.getException().getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Error al registrar: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

}
