package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailReset;
    private Button btnReset, btnBackLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();

        // Referencias a vistas
        emailReset = findViewById(R.id.emailReset);
        btnReset = findViewById(R.id.btnReset);
        btnBackLogin = findViewById(R.id.btnBackLogin);

        // Botón para enviar el enlace
        btnReset.setOnClickListener(v -> {
            String email = emailReset.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Por favor, introduce tu correo electrónico", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Correo de recuperación enviado a " + email, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Botón para volver al login
        btnBackLogin.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }
}
