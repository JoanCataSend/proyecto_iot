package com.example.proyecto_iot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iot.utils.CustomToast;

public class ConfigNotificacionesFragment extends Fragment {

    private MaterialSwitch swVentanaRota, swPuertasAbiertas, swCamara, swMovimiento, swSonido, swUbicacion, swApertura;
    private MaterialButton btnGuardar;
    private SharedPreferences prefs;
    private boolean cambiosPendientes = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_config_notificaciones, container, false);

        // Header visible y flecha atrás activa
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }

        prefs = requireActivity().getSharedPreferences("notificaciones_prefs", Context.MODE_PRIVATE);

        // Referencias
        swVentanaRota = view.findViewById(R.id.swVentanaRota);
        swPuertasAbiertas = view.findViewById(R.id.swPuertasAbiertas);
        swCamara = view.findViewById(R.id.swCamara);
        swMovimiento = view.findViewById(R.id.swMovimiento);
        swSonido = view.findViewById(R.id.swSonido);
        swUbicacion = view.findViewById(R.id.swUbicacion);
        swApertura = view.findViewById(R.id.swApertura);
        btnGuardar = view.findViewById(R.id.btnGuardarNotificaciones);

        loadSwitchStates();

        // Listener para detectar cambios
        MaterialSwitch[] switches = {
                swVentanaRota, swPuertasAbiertas, swCamara, swMovimiento,
                swSonido, swUbicacion, swApertura
        };

        for (MaterialSwitch sw : switches) {
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                cambiosPendientes = true;
                actualizarBotonGuardar();
            });
        }

        // Guardar cambios
        btnGuardar.setOnClickListener(v -> {
            saveSwitchStates();
            cambiosPendientes = false;
            actualizarBotonGuardar();
            CustomToast.success(requireActivity(), "Cambios guardados correctamente");
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Flecha atrás del header
        View btnBack = requireActivity().findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }

        return view;
    }

    // ======================================================
    private void actualizarBotonGuardar() {
        if (btnGuardar == null) return;
        btnGuardar.setEnabled(cambiosPendientes);
        btnGuardar.setAlpha(cambiosPendientes ? 1f : 0.5f);
    }

    // ======================================================
    private void saveSwitchStates() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("swVentanaRota", swVentanaRota.isChecked());
        editor.putBoolean("swPuertasAbiertas", swPuertasAbiertas.isChecked());
        editor.putBoolean("swCamara", swCamara.isChecked());
        editor.putBoolean("swMovimiento", swMovimiento.isChecked());
        editor.putBoolean("swSonido", swSonido.isChecked());
        editor.putBoolean("swUbicacion", swUbicacion.isChecked());
        editor.putBoolean("swApertura", swApertura.isChecked());
        editor.apply();
    }

    // ======================================================
    private void loadSwitchStates() {
        swVentanaRota.setChecked(prefs.getBoolean("swVentanaRota", true));
        swPuertasAbiertas.setChecked(prefs.getBoolean("swPuertasAbiertas", true));
        swCamara.setChecked(prefs.getBoolean("swCamara", true));
        swMovimiento.setChecked(prefs.getBoolean("swMovimiento", true));
        swSonido.setChecked(prefs.getBoolean("swSonido", true));
        swUbicacion.setChecked(prefs.getBoolean("swUbicacion", true));
        swApertura.setChecked(prefs.getBoolean("swApertura", true));
    }
}
