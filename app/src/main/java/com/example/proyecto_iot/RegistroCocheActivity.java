package com.example.proyecto_iot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistroCocheActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_coche); // Carga el layout de registro_coche.xml

        // Referencias a los campos de entrada de texto y botón
        EditText marca = findViewById(R.id.eTxtMarca);
        EditText modelo = findViewById(R.id.eTxtModelo);
        EditText matricula = findViewById(R.id.eTxtMatricula);
        EditText nombre = findViewById(R.id.eTxtNombreCoche);
        Button btnRegistrar = findViewById(R.id.btnRegistrarCoche);

        // Acción del botón
        btnRegistrar.setOnClickListener(v -> {
            // Se convierten a texto y se quitan los espacios
            String txtMarca = marca.getText().toString().trim();
            String txtModelo = modelo.getText().toString().trim();
            String txtMatricula = matricula.getText().toString().trim();
            String txtNombre = nombre.getText().toString().trim();

            // Se comprueba si hay algún campo vacío
            if (txtMarca.isEmpty() || txtModelo.isEmpty() || txtMatricula.isEmpty() || txtNombre.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            } else {
                // Si no hay campos vacíos, se muestra un mensaje de éxito
                Toast.makeText(this, "Coche registrado correctamente", Toast.LENGTH_SHORT).show();
            }
        });


    }


}
