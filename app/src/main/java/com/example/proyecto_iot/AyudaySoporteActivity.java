package com.example.proyecto_iot;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class AyudaySoporteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayudaysoporte);

        ImageView btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());
    }
}
