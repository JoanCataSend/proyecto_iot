package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iot.utils.CustomToast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterWizardActivity extends AppCompatActivity {

    private LinearLayout stepNombre, stepEmail, stepPassword;
    private EditText etNombre, etEmail, etPassword, etRepeatPassword;
    private TextView tvStep, tvTitle, tvSubtitle, tvLogin;
    private Button btnPrimary;

    private int currentStep = 0;

    private String nombreUsuario, email, password, repeatPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_wizard);

        mAuth = FirebaseAuth.getInstance();

        stepNombre = findViewById(R.id.stepNombre);
        stepEmail = findViewById(R.id.stepEmail);
        stepPassword = findViewById(R.id.stepPassword);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);

        tvStep = findViewById(R.id.tvStep);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvLogin = findViewById(R.id.tvLogin);
        btnPrimary = findViewById(R.id.btnPrimary);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> goPreviousStep());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnPrimary.setOnClickListener(v -> onPrimaryClicked());

        updateUiForStep();
    }

    // ======================================================
    private void onPrimaryClicked() {

        switch (currentStep) {

            case 0:
                nombreUsuario = etNombre.getText().toString().trim();
                if (TextUtils.isEmpty(nombreUsuario)) {
                    CustomToast.warning(this, "Introduce tu nombre");
                    return;
                }
                currentStep++;
                updateUiForStep();
                break;

            case 1:
                email = etEmail.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    CustomToast.warning(this, "Introduce tu correo");
                    return;
                }
                currentStep++;
                updateUiForStep();
                break;

            case 2:
                password = etPassword.getText().toString().trim();
                repeatPassword = etRepeatPassword.getText().toString().trim();

                if (TextUtils.isEmpty(password) || TextUtils.isEmpty(repeatPassword)) {
                    CustomToast.warning(this, "Completa la contraseña");
                    return;
                }

                if (!password.equals(repeatPassword)) {
                    CustomToast.error(this, "Las contraseñas no coinciden");
                    return;
                }

                if (password.length() < 6) {
                    CustomToast.warning(this, "La contraseña debe tener al menos 6 caracteres");
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

    // ======================================================
    private void updateUiForStep() {

        stepNombre.setVisibility(currentStep == 0 ? LinearLayout.VISIBLE : LinearLayout.GONE);
        stepEmail.setVisibility(currentStep == 1 ? LinearLayout.VISIBLE : LinearLayout.GONE);
        stepPassword.setVisibility(currentStep == 2 ? LinearLayout.VISIBLE : LinearLayout.GONE);

        tvStep.setText("Paso " + (currentStep + 1) + " de 3");

        switch (currentStep) {
            case 0:
                tvTitle.setText("Vamos a conocernos");
                tvSubtitle.setText("¿Cómo te llamas?");
                btnPrimary.setText("Siguiente");
                break;

            case 1:
                tvTitle.setText("Tu correo electrónico");
                tvSubtitle.setText("Lo usaremos para crear tu cuenta.");
                btnPrimary.setText("Siguiente");
                break;

            case 2:
                tvTitle.setText("Protege tu cuenta");
                tvSubtitle.setText("Crea una contraseña segura.");
                btnPrimary.setText("Crear cuenta");
                break;
        }
    }

    // ======================================================
    private void registrarUsuario() {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        CustomToast.error(this,
                                "Error al registrar: " + task.getException().getMessage());
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user == null) {
                        CustomToast.error(this, "Error inesperado: usuario nulo.");
                        return;
                    }

                    user.sendEmailVerification()
                            .addOnCompleteListener(verificationTask -> {

                                if (!verificationTask.isSuccessful()) {
                                    CustomToast.error(this,
                                            "Error enviando correo: " +
                                                    verificationTask.getException().getMessage());
                                    return;
                                }

                                guardarUsuarioEnFirestore(user);
                            });
                });
    }

    // ======================================================
    private void guardarUsuarioEnFirestore(FirebaseUser user) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userData = new HashMap<>();
        userData.put("Usuario", nombreUsuario);
        userData.put("Correo", email);
        userData.put("Imagen", "/foto/so");
        userData.put("FechaRegistro", FieldValue.serverTimestamp());

        db.collection("Usuarios")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(unused -> {

                    getSharedPreferences("kova_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("wizard_completed", true)
                            .apply();

                    CustomToast.success(this,
                            "¡Registro exitoso! Verifica tu correo antes de iniciar sesión.");

                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        CustomToast.error(this,
                                "Error guardando usuario: " + e.getMessage()));
    }
}
