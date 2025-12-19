package com.example.proyecto_iot;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

    // STEPS
    private LinearLayout stepNombre, stepEmail, stepPassword;
    private EditText etNombre, etEmail, etPassword, etRepeatPassword;

    // UI
    private TextView tvStep, tvTitle, tvSubtitle, tvSecondaryAction;
    private Button btnPrimary;
    private ImageButton btnBack;

    // LOADING
    private FrameLayout loadingOverlay;
    private WebView carLoader;

    // STATE
    private int currentStep = 0;
    private String nombreUsuario, email, password, repeatPassword;

    // FIREBASE
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_wizard);

        mAuth = FirebaseAuth.getInstance();

        // STEPS
        stepNombre = findViewById(R.id.stepNombre);
        stepEmail = findViewById(R.id.stepEmail);
        stepPassword = findViewById(R.id.stepPassword);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);

        // TEXTS
        tvStep = findViewById(R.id.tvStep);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvSecondaryAction = findViewById(R.id.tvSecondaryAction);

        // BUTTONS
        btnPrimary = findViewById(R.id.btnPrimary);
        btnBack = findViewById(R.id.btnBack);

        // SKIP
        TextView tvSkip = findViewById(R.id.tvSkip);
        tvSkip.setPaintFlags(tvSkip.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvSkip.setOnClickListener(v -> {
            markWizardCompleted();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // YA TENGO CUENTA
        tvSecondaryAction.setPaintFlags(tvSecondaryAction.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvSecondaryAction.setOnClickListener(v -> {
            markWizardCompleted();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // BACK
        btnBack.setOnClickListener(v -> goPreviousStep());

        // LOADING
        loadingOverlay = findViewById(R.id.loadingOverlay);
        carLoader = findViewById(R.id.carLoader);
        configurarWebView();

        btnPrimary.setOnClickListener(v -> onPrimaryClicked());

        updateUiForStep();
    }

    // ======================================================
    private void configurarWebView() {
        carLoader.setBackgroundColor(Color.TRANSPARENT);

        WebSettings settings = carLoader.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        carLoader.setInitialScale(100);
        carLoader.setVerticalScrollBarEnabled(false);
        carLoader.setHorizontalScrollBarEnabled(false);

        carLoader.loadUrl("file:///android_asset/car_loader.html");
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
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    CustomToast.error(this, "Introduce un email válido");
                    return;
                }
                comprobarEmailExiste(email);
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

    // ======================================================
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

        stepNombre.setVisibility(currentStep == 0 ? View.VISIBLE : View.GONE);
        stepEmail.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        stepPassword.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);

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

    // ======================================================
    private void comprobarEmailExiste(String email) {

        mostrarLoading(true);

        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {

                    mostrarLoading(false);

                    if (!task.isSuccessful()) {
                        CustomToast.error(this, "Error comprobando email");
                        return;
                    }

                    boolean existe = !task.getResult().getSignInMethods().isEmpty();

                    if (existe) {
                        CustomToast.error(this, "Este correo ya está registrado");
                    } else {
                        currentStep++;
                        updateUiForStep();
                    }
                });
    }

    // ======================================================
    private void registrarUsuario() {

        mostrarLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    mostrarLoading(false);

                    if (!task.isSuccessful()) {
                        CustomToast.error(this, "Error al registrar");
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) return;

                    user.sendEmailVerification()
                            .addOnCompleteListener(t -> guardarUsuarioEnFirestore(user));
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

                    markWizardCompleted();

                    CustomToast.success(this,
                            "¡Registro exitoso! Verifica tu correo.");

                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        CustomToast.error(this, "Error guardando usuario"));
    }

    // ======================================================
    private void mostrarLoading(boolean mostrar) {

        if (mostrar) {
            loadingOverlay.setVisibility(View.VISIBLE);
            btnPrimary.setEnabled(false);
        } else {
            loadingOverlay.setVisibility(View.GONE);
            btnPrimary.setEnabled(true);
        }
    }

    private void markWizardCompleted() {
        getSharedPreferences("kova_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("wizard_completed", true)
                .apply();
    }
}
