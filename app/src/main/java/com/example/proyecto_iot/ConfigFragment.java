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

        // Header visible, sin flecha atrás
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        // Navegar a ConfigNotificacionesFragment al pulsar "Notificaciones"
        LinearLayout layoutNotificaciones = view.findViewById(R.id.layout_notificaciones);
        layoutNotificaciones.setOnClickListener(v -> {
            FragmentTransaction transaction = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();
            transaction.replace(R.id.fragment_container, new ConfigNotificacionesFragment());
            transaction.addToBackStack(null);
            transaction.commit();

            // Mostrar flecha atrás en el nuevo fragment
            ((MainActivity) requireActivity()).setBackButtonVisible(true);
        });

        return view;
    }
}
