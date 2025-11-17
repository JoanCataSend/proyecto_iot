package com.example.proyecto_iot;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeDemoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout sencillo en código sin XML, solo para probar
        TextView textView = new TextView(this);
        textView.setText("Modo demostración activo 🚗\nAquí irá el Home real.");
        textView.setTextSize(18);
        textView.setGravity(android.view.Gravity.CENTER);
        setContentView(textView);
    }
}
