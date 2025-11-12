package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class ConfigFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_config, container, false);

        // 🔹 Mostrar header sin flecha en pantalla de configuración principal
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        // 🔸 Opción: Notificaciones
        LinearLayout layoutNotificaciones = view.findViewById(R.id.layout_notificaciones);
        if (layoutNotificaciones != null) {
            layoutNotificaciones.setOnClickListener(v -> {
                FragmentTransaction transaction = requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.fragment_container, new ConfigNotificacionesFragment());
                transaction.addToBackStack(null);
                transaction.commit();

                ((MainActivity) requireActivity()).setBackButtonVisible(true);
            });
        }

        LinearLayout layoutSeguridad = view.findViewById(R.id.layoutSeguridad);
        if (layoutSeguridad != null) {
            layoutSeguridad.setOnClickListener(v -> {
                FragmentTransaction transaction = requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.fragment_container, new SeguridadFragment());
                transaction.addToBackStack(null);
                transaction.commit();

                ((MainActivity) requireActivity()).setBackButtonVisible(true);
            });
        }

        return view;
    }
}
