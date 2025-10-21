package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login); // 👈 conecta con tu login.xml

        // Referencias a tus vistas
        EditText emailEditText = findViewById(R.id.editTextText);
        EditText passwordEditText = findViewById(R.id.editTextTextPassword);
        Button loginButton = findViewById(R.id.button);
        Button registerButton = findViewById(R.id.button2);
        Button forgotButton = findViewById(R.id.button3);

        // Acción: Iniciar sesión
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            } else if (email.equals("usuario@ejemplo.com") && password.equals("1234")) {
                Toast.makeText(this, "Inicio de sesión exitoso ✅", Toast.LENGTH_SHORT).show();

                // Ejemplo: abrir otra Activity (MainActivity)
                // Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                // startActivity(intent);
            } else {
                Toast.makeText(this, "Correo o contraseña incorrectos ❌", Toast.LENGTH_SHORT).show();
            }
        });

        // Acción: Ir a registro
        registerButton.setOnClickListener(v ->
                Toast.makeText(this, "Ir a registro (no implementado aún)", Toast.LENGTH_SHORT).show()
        );

        // Acción: Olvidar contraseña
        forgotButton.setOnClickListener(v ->
                Toast.makeText(this, "Función de recuperación pendiente", Toast.LENGTH_SHORT).show()
        );
    }
}
