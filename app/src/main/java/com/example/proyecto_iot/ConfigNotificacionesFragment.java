package com.example.proyecto_iot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.google.android.material.materialswitch.MaterialSwitch;

public class ConfigNotificacionesFragment extends Fragment {

    private static final String PREFS_NAME = "notificaciones_prefs";

    private MaterialSwitch swVentanaRota;
    private MaterialSwitch swPuertasAbiertas;
    private MaterialSwitch swCamara;
    private MaterialSwitch swMovimiento;
    private MaterialSwitch swSonido;
    private MaterialSwitch swUbicacion;
    private MaterialSwitch swApertura;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config_notificaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swVentanaRota = view.findViewById(R.id.swVentanaRota);
        swPuertasAbiertas = view.findViewById(R.id.swPuertasAbiertas);
        swCamara = view.findViewById(R.id.swCamara);
        swMovimiento = view.findViewById(R.id.swMovimiento);
        swSonido = view.findViewById(R.id.swSonido);
        swUbicacion = view.findViewById(R.id.swUbicacion);

        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (swVentanaRota != null) {
            swVentanaRota.setChecked(prefs.getBoolean("swVentanaRota", true));
            swVentanaRota.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("swVentanaRota", isChecked).apply();
                }
            });
        }

        if (swPuertasAbiertas != null) {
            swPuertasAbiertas.setChecked(prefs.getBoolean("swPuertasAbiertas", true));
            swPuertasAbiertas.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("swPuertasAbiertas", isChecked).apply();
                }
            });
        }

        if (swCamara != null) {
            swCamara.setChecked(prefs.getBoolean("swCamara", true));
            swCamara.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("swCamara", isChecked).apply();
                }
            });
        }

        if (swMovimiento != null) {
            swMovimiento.setChecked(prefs.getBoolean("swMovimiento", true));
            swMovimiento.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("swMovimiento", isChecked).apply();
                }
            });
        }

        if (swSonido != null) {
            swSonido.setChecked(prefs.getBoolean("swSonido", true));
            swSonido.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("swSonido", isChecked).apply();
                }
            });
        }

        if (swUbicacion != null) {
            swUbicacion.setChecked(prefs.getBoolean("swUbicacion", true));
            swUbicacion.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("swUbicacion", isChecked).apply();
                }
            });
        }

        if (swApertura != null) {
            swApertura.setChecked(prefs.getBoolean("swApertura", true));
            swApertura.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("swApertura", isChecked).apply();
                }
            });
        }
    }
}
