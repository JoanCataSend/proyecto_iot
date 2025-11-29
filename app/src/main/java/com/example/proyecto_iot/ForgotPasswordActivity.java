package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iot.utils.CustomToast;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailReset;
    private Button btnReset, btnBackLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);

        mAuth = FirebaseAuth.getInstance();

        emailReset = findViewById(R.id.emailReset);
        btnReset = findViewById(R.id.btnReset);
        btnBackLogin = findViewById(R.id.btnBackLogin);

        btnReset.setOnClickListener(v -> {
            String email = emailReset.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                CustomToast.warning(this, "Por favor, introduce tu correo electrónico");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            CustomToast.success(this,
                                    "Correo de recuperación enviado a " + email);
                        } else {
                            CustomToast.error(this,
                                    "Error: " + task.getException().getMessage());
                        }
                    });
        });

        btnBackLogin.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }
}
