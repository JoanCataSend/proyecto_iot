package com.example.proyecto_iot;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

public class NotificacionesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflar el layout del fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_notificaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Datos de ejemplo
        List<Notificacion> notificaciones = new ArrayList<>();
        notificaciones.add(new Notificacion(
                "Ventana rota detectada",
                "El sensor del coche 'El Champon' ha detectado una vibración fuerte en la ventana delantera.",
                "18:42  12/10/25",
                R.drawable.ic_info
        ));
        notificaciones.add(new Notificacion(
                "Puertas desbloqueadas",
                "Las puertas del coche 'Speedy Azul' se han abierto correctamente desde la app.",
                "17:58  12/10/25",
                R.drawable.ic_info
        ));

        // Conectas el adapter
        NotificacionAdapter adapter = new NotificacionAdapter(notificaciones);
        recyclerView.setAdapter(adapter);
    }
}
