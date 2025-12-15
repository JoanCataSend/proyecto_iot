package com.example.proyecto_iot;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AyudaySoporteFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ayudaysoporte, container, false);

        // Mostrar flecha atrás
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }

        // 🔗 ACTIVAR LINKS
        activarLinks(view);

        return view;
    }

    /**
     * Activa los enlaces HTML (<a href="...">) dentro de los TextView
     */
    private void activarLinks(View view) {

        int[] linkIds = {
                R.id.linkAddCar,
                R.id.linkActivarSensores,
                R.id.linkPerderCoche,
                R.id.linkTerminos
        };

        for (int id : linkIds) {
            TextView tv = view.findViewById(id);
            if (tv != null) {
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }
}
