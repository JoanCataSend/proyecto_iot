package com.example.proyecto_iot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iot.R;

public class AparienciaFragment extends Fragment {

    private static final String PREFS_NAME = "TemaPrefs";
    private static final String THEME_KEY = "tema_seleccionado";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.apariencia_fragment, container, false);

        RadioGroup radioGroup = view.findViewById(R.id.radio_group_tema);

        cargarTemaGuardado(radioGroup);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int modo;

            if (checkedId == R.id.radio_sistema) {
                modo = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            } else if (checkedId == R.id.radio_claro) {
                modo = AppCompatDelegate.MODE_NIGHT_NO;
            } else {
                modo = AppCompatDelegate.MODE_NIGHT_YES;
            }

            aplicarNuevoTema(modo);
        });

        return view;
    }

    private void aplicarNuevoTema(int modo) {
        AppCompatDelegate.setDefaultNightMode(modo);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        prefs.edit().putInt(THEME_KEY, modo).apply();
    }

    private void cargarTemaGuardado(RadioGroup radioGroup) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int modoGuardado = prefs.getInt(
                THEME_KEY,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );

        int radioId;
        if (modoGuardado == AppCompatDelegate.MODE_NIGHT_NO) {
            radioId = R.id.radio_claro;
        } else if (modoGuardado == AppCompatDelegate.MODE_NIGHT_YES) {
            radioId = R.id.radio_oscuro;
        } else {
            radioId = R.id.radio_sistema;
        }

        radioGroup.check(radioId);
    }
}
