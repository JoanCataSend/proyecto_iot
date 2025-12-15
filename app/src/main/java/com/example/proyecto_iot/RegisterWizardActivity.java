package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterWizardActivity extends AppCompatActivity {

    private LinearLayout stepNombre, stepEmail, stepPassword;
    private EditText etNombre, etEmail, etPassword, etRepeatPassword;
    private int currentStep = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_wizard);

        stepNombre = findViewById(R.id.stepNombre);
        stepEmail = findViewById(R.id.stepEmail);
        stepPassword = findViewById(R.id.stepPassword);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);

        findViewById(R.id.btnPrimary).setOnClickListener(v -> nextStep());
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void nextStep() {
        if (currentStep == 1) {
            stepNombre.setVisibility(View.GONE);
            stepEmail.setVisibility(View.VISIBLE);
            currentStep = 2;
        } else if (currentStep == 2) {
            stepEmail.setVisibility(View.GONE);
            stepPassword.setVisibility(View.VISIBLE);
            currentStep = 3;
        } else {
            finish(); // aquí luego registras usuario
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStep == 2) {
            stepEmail.setVisibility(View.GONE);
            stepNombre.setVisibility(View.VISIBLE);
            currentStep = 1;
        } else if (currentStep == 3) {
            stepPassword.setVisibility(View.GONE);
            stepEmail.setVisibility(View.VISIBLE);
            currentStep = 2;
        } else {
            super.onBackPressed();
        }
    }
}
