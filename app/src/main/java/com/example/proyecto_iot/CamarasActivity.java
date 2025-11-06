package com.example.proyecto_iot;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class CamarasActivity extends AppCompatActivity {
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camaras);

        btnBack = findViewById(R.id.btnBack);

        // --- VOLVER ---
        btnBack.setOnClickListener(v -> finish());
    }
}
