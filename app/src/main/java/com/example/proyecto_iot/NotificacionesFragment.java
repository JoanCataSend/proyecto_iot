package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificacionesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificacionAdapter adapter;

    private final ArrayList<Notificacion> notificaciones = new ArrayList<>();
    private final ArrayList<Notificacion> allNotificaciones = new ArrayList<>();

    private FirebaseFirestore firestore;

    // filtros actuales
    private String filterCar = "Todos";
    private String filterType = "Todos";
    private String filterRange = "Todo";

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

        View btnFilter = view.findViewById(R.id.btnFilter);
        View btnDownload  = view.findViewById(R.id.btnDownload);

        btnFilter.setOnClickListener(v -> openFilterPopup());
        btnDownload.setOnClickListener(v -> {
            // aquí va lo de descargar pdf
            recyclerView.smoothScrollToPosition(0);
        });

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

                                allNotificaciones.clear();
                                for (ItemTemp it : temp) {
                                    allNotificaciones.add(it.notif);
                                }
                                applyFilters();

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
                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info, carName, "Impacto", ts);
            }

            case "alerta_sonido": {
                Long activar = ev.getLong("activar"); // 1 o 0
                boolean on = activar != null && activar == 1;

                String titulo = "Alertas activadas";
                String mensaje = "Se han activado las alertas luminosas y sonoras de " + carName + ".";

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_sonido, carName, "Alarmas", ts);

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

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info, carName, "Puertas", ts);

            }

            default: {
                String titulo = "Evento: " + tipo;
                String mensaje = "Notificación del coche (" + carName + ").";
                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info, carName, "Otros", ts);
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

    private void applyFilters() {
        notificaciones.clear();

        long now = System.currentTimeMillis();
        long minTs = 0;

        // rango de tiempo (ejemplos)
        if ("24h".equals(filterRange)) minTs = now - 24L * 60 * 60 * 1000;
        else if ("7d".equals(filterRange)) minTs = now - 7L * 24 * 60 * 60 * 1000;
        else if ("30d".equals(filterRange)) minTs = now - 30L * 24 * 60 * 60 * 1000;

        for (Notificacion n : allNotificaciones) {

            // OJO: esto requiere que Notificacion tenga carName, category y timestamp
            boolean okCar = "Todos".equals(filterCar) || filterCar.equals(n.getCarName());
            boolean okType = "Todos".equals(filterType) || filterType.equals(n.getCategory());
            boolean okTime = "Todo".equals(filterRange) || n.getTimestamp() >= minTs;

            if (okCar && okType && okTime) {
                notificaciones.add(n);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void openFilterPopup() {

        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext()).inflate(R.layout.filter_notifications, null);
        dialog.setContentView(sheet);

        Spinner spCar = sheet.findViewById(R.id.spFilterCar);
        Spinner spType = sheet.findViewById(R.id.spFilterType);
        Spinner spRange = sheet.findViewById(R.id.spFilterRange);

        TextView btnApply = sheet.findViewById(R.id.btnApplyFilters);
        TextView btnClear = sheet.findViewById(R.id.btnClearFilters);

        // ---- LISTA COCHES (desde historial ya cargado) ----
        Set<String> carsSet = new LinkedHashSet<>();
        carsSet.add("Todos");
        for (Notificacion n : allNotificaciones) {
            if (n.getCarName() != null && !n.getCarName().trim().isEmpty()) {
                carsSet.add(n.getCarName().trim());
            }
        }
        List<String> cars = new ArrayList<>(carsSet);

        // ---- LISTA TIPOS ----
        List<String> types = new ArrayList<>();
        types.add("Todos");
        types.add("Alarmas");
        types.add("Puertas");
        types.add("Impacto");

        // ---- LISTA RANGOS ----
        List<String> ranges = new ArrayList<>();
        ranges.add("Todo");
        ranges.add("24h");
        ranges.add("7d");
        ranges.add("30d");

        // Adapters
        ArrayAdapter<String> carAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, cars);
        carAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCar.setAdapter(carAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        ArrayAdapter<String> rangeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, ranges);
        rangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRange.setAdapter(rangeAdapter);

        // Preseleccionar lo que ya estaba elegido
        spCar.setSelection(Math.max(0, cars.indexOf(filterCar)));
        spType.setSelection(Math.max(0, types.indexOf(filterType)));
        spRange.setSelection(Math.max(0, ranges.indexOf(filterRange)));

        btnApply.setOnClickListener(v -> {
            filterCar = (String) spCar.getSelectedItem();
            filterType = (String) spType.getSelectedItem();
            filterRange = (String) spRange.getSelectedItem();

            applyFilters();
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            filterCar = "Todos";
            filterType = "Todos";
            filterRange = "Todo";

            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }


}
