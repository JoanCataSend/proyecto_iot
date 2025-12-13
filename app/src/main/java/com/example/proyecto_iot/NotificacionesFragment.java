package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class NotificacionesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificacionAdapter adapter;

    private final ArrayList<Notificacion> notificaciones = new ArrayList<>();

    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        firestore = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recycler_notificaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificacionAdapter(notificaciones);
        recyclerView.setAdapter(adapter);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        cargarHistorialFirestore();

        return view;
    }

    private void cargarHistorialFirestore() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("Coches")
                .whereArrayContains("Propietario", uid)
                .get()
                .addOnSuccessListener(carsSnap -> {

                    if (carsSnap.isEmpty()) {
                        notificaciones.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    HashMap<String, String> carIdToName = new HashMap<>();
                    List<String> carIds = new ArrayList<>();

                    for (QueryDocumentSnapshot carDoc : carsSnap) {
                        String carId = carDoc.getId();
                        String nombre = carDoc.getString("Nombre");
                        carIds.add(carId);
                        carIdToName.put(carId, (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : "Vehículo");
                    }

                    List<Task<?>> tasks = new ArrayList<>();
                    ArrayList<ItemTemp> temp = new ArrayList<>();

                    for (String carId : carIds) {
                        Task<?> t = firestore.collection("Coches")
                                .document(carId)
                                .collection("eventos")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(300)
                                .get()
                                .addOnSuccessListener(eventsSnap -> {
                                    for (QueryDocumentSnapshot ev : eventsSnap) {
                                        String tipo = ev.getString("tipo");
                                        Long ts = ev.getLong("timestamp");

                                        String carName = carIdToName.get(carId);

                                        Notificacion n = buildNotificacionFromEvento(tipo, ev, carName, ts);
                                        if (n != null && ts != null) {
                                            temp.add(new ItemTemp(n, ts));
                                        }
                                    }
                                });

                        tasks.add(t);
                    }

                    Tasks.whenAllComplete(tasks)
                            .addOnSuccessListener(done -> {

                                Collections.sort(temp, (a, b) -> Long.compare(b.ts, a.ts));

                                notificaciones.clear();
                                for (ItemTemp it : temp) {
                                    notificaciones.add(it.notif);
                                }

                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                notificaciones.clear();
                                adapter.notifyDataSetChanged();
                            });

                })
                .addOnFailureListener(e -> {
                    notificaciones.clear();
                    adapter.notifyDataSetChanged();
                });
    }

    private Notificacion buildNotificacionFromEvento(String tipo,
                                                     QueryDocumentSnapshot ev,
                                                     String carName,
                                                     Long ts) {

        if (tipo == null || ts == null) return null;

        String fecha = formatFecha(ts);

        switch (tipo) {

            case "impacto": {
                String titulo = "Impacto detectado";
                String mensaje = "Tu vehículo " + carName + " ha recibido un impacto.";
                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info);
            }

            case "alerta_sonido": {
                Long activar = ev.getLong("activar"); // 1 o 0
                boolean on = activar != null && activar == 1;

                String titulo = "Alertas activadas";
                String mensaje = "Se han activado las alertas luminosas y sonoras de " + carName + ".";

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_sonido);
            }

            // Si en algún momento guardas eventos de puerta:
            // { tipo:"puerta", puerta:"open"/"closed", timestamp:... }
            case "puerta": {
                String puerta = ev.getString("puerta");
                boolean open = "open".equals(puerta);

                String titulo = open ? "Puertas abiertas" : "Puertas bloqueadas";
                String mensaje = open
                        ? "El coche (" + carName + ") se ha desbloqueado."
                        : "El coche (" + carName + ") se ha bloqueado.";

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info);
            }

            default: {
                String titulo = "Evento: " + tipo;
                String mensaje = "Notificación del coche (" + carName + ").";
                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info);
            }
        }
    }

    private String formatFecha(long ts) {
        return new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault())
                .format(new Date(ts));
    }

    private static class ItemTemp {
        Notificacion notif;
        long ts;

        ItemTemp(Notificacion n, long ts) {
            this.notif = n;
            this.ts = ts;
        }
    }
}
