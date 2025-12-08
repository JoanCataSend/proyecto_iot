package com.example.proyecto_iot;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterWizardActivity extends AppCompatActivity {

    private LinearLayout stepNombre, stepEmail, stepPassword;
    private EditText etNombre, etEmail, etPassword, etRepeatPassword;
    private TextView tvStep, tvTitle, tvSubtitle, tvSecondaryAction;
    private Button btnPrimary;

    private int currentStep = 0;

    private String nombreUsuario, email, password, repeatPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_wizard);

        mAuth = FirebaseAuth.getInstance();

        // Steps
        stepNombre = findViewById(R.id.stepNombre);
        stepEmail = findViewById(R.id.stepEmail);
        stepPassword = findViewById(R.id.stepPassword);

        // Inputs
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);

        // UI
        tvStep = findViewById(R.id.tvStep);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvSecondaryAction = findViewById(R.id.tvSecondaryAction);

        // Omitir
        TextView tvSkip = findViewById(R.id.tvSkip);
        tvSkip.setPaintFlags(tvSkip.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnPrimary = findViewById(R.id.btnPrimary);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> goPreviousStep());

        tvSecondaryAction.setPaintFlags(tvSecondaryAction.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvSecondaryAction.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnPrimary.setOnClickListener(v -> onPrimaryClicked());

        updateUiForStep();
    }

    private void onPrimaryClicked() {
        switch (currentStep) {
            case 0:
                nombreUsuario = etNombre.getText().toString().trim();
                if (TextUtils.isEmpty(nombreUsuario)) {
                    etNombre.setError("Introduce tu nombre");
                    return;
                }
                currentStep++;
                updateUiForStep();
                break;

            case 1:
                email = etEmail.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    etEmail.setError("Introduce tu correo");
                    return;
                }
                currentStep++;
                updateUiForStep();
                break;

            case 2:
                password = etPassword.getText().toString().trim();
                repeatPassword = etRepeatPassword.getText().toString().trim();

                if (TextUtils.isEmpty(password) || TextUtils.isEmpty(repeatPassword)) {
                    Toast.makeText(this, "Completa la contraseña", Toast.LENGTH_SHORT).show();
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

                registrarUsuario();
                break;
        }
    }

    private void goPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            updateUiForStep();
        } else {
            finish();
        }
    }

    private void updateUiForStep() {
        stepNombre.setVisibility(currentStep == 0 ? LinearLayout.VISIBLE : LinearLayout.GONE);
        stepEmail.setVisibility(currentStep == 1 ? LinearLayout.VISIBLE : LinearLayout.GONE);
        stepPassword.setVisibility(currentStep == 2 ? LinearLayout.VISIBLE : LinearLayout.GONE);

        tvStep.setText("Paso " + (currentStep + 1) + " de 3");

        switch (currentStep) {
            case 0:
                tvTitle.setText("Empecemos con lo básico");
                tvSubtitle.setText("Cuéntanos tu nombre para personalizar tu experiencia.");
                btnPrimary.setText("Continuar");
                break;

            case 1:
                tvTitle.setText("Tu correo electrónico");
                tvSubtitle.setText("Será tu usuario y donde recibirás avisos importantes.");
                btnPrimary.setText("Continuar");
                break;

            case 2:
                tvTitle.setText("Crea una contraseña segura");
                tvSubtitle.setText("Protegeremos tu cuenta con tus datos cifrados.");
                btnPrimary.setText("Crear cuenta");
                break;
        }

    }

    private void registrarUsuario() {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Error al registrar: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user == null) {
                        Toast.makeText(this,
                                "Error inesperado: usuario nulo.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    user.sendEmailVerification()
                            .addOnCompleteListener(verificationTask -> {

                                if (!verificationTask.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Error enviando correo: " + verificationTask.getException().getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                guardarUsuarioEnFirestore(user);
                            });
                });
    }

    private void guardarUsuarioEnFirestore(FirebaseUser user) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userData = new HashMap<>();
        userData.put("Usuario", nombreUsuario);
        userData.put("Correo", email);
        userData.put("Imagen", "/foto/so"); // mismo valor que tu otro registro
        userData.put("FechaRegistro", FieldValue.serverTimestamp());

        db.collection("Usuarios")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(unused -> {

                    getSharedPreferences("kova_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("wizard_completed", true)
                            .apply();

                    Toast.makeText(this,
                            "¡Registro exitoso! Verifica tu correo antes de iniciar sesión.",
                            Toast.LENGTH_LONG).show();

                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error guardando usuario: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
