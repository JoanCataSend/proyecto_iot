package com.example.proyecto_iot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SeguridadFragment extends Fragment {

    private CheckBox checkPuertas, checkVentanas, checkMovimiento, checkAlarma, checkLuces, checkBloqueo;
    private Button btnGuardar;
    private SharedPreferences prefs;
    private Vibrator vibrator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_seguridad, container, false);

        // Inicialización de vistas
        checkPuertas = view.findViewById(R.id.checkPuertas);
        checkVentanas = view.findViewById(R.id.checkVentanas);
        checkMovimiento = view.findViewById(R.id.checkMovimiento);
        checkAlarma = view.findViewById(R.id.checkAlarma);
        checkLuces = view.findViewById(R.id.checkLuces);
        checkBloqueo = view.findViewById(R.id.checkBloqueo);
        btnGuardar = view.findViewById(R.id.btnGuardar);

        Context context = requireContext();
        prefs = context.getSharedPreferences("seguridad_prefs", Context.MODE_PRIVATE);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // Restaurar estados guardados
        checkPuertas.setChecked(prefs.getBoolean("checkPuertas", true));
        checkVentanas.setChecked(prefs.getBoolean("checkVentanas", true));
        checkMovimiento.setChecked(prefs.getBoolean("checkMovimiento", true));
        checkAlarma.setChecked(prefs.getBoolean("checkAlarma", true));
        checkLuces.setChecked(prefs.getBoolean("checkLuces", true));
        checkBloqueo.setChecked(prefs.getBoolean("checkBloqueo", true));

        // Botón Guardar cambios
        btnGuardar.setOnClickListener(v -> {
            guardarPreferencias();
            vibrar();
            Toast.makeText(context, "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
        });

        // Flecha volver

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Mostrar flecha en MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Ocultar flecha al salir
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }
    }
    private void guardarPreferencias() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("checkPuertas", checkPuertas.isChecked());
        editor.putBoolean("checkVentanas", checkVentanas.isChecked());
        editor.putBoolean("checkMovimiento", checkMovimiento.isChecked());
        editor.putBoolean("checkAlarma", checkAlarma.isChecked());
        editor.putBoolean("checkLuces", checkLuces.isChecked());
        editor.putBoolean("checkBloqueo", checkBloqueo.isChecked());
        editor.apply();
    }
    private void vibrar() {
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }
}
