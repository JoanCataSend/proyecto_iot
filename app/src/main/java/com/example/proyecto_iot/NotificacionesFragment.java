package com.example.proyecto_iot;

import android.app.DatePickerDialog;
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

import com.example.proyecto_iot.utils.ImpactCaptureHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    // rango fechas (0 = sin filtro)
    private long filterFromTs = 0L;
    private long filterToTs = 0L;

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

                long ahora = System.currentTimeMillis();

                // Solo impactos recientes (10 segundos)
                if (ahora - ts < 10_000) {
                    ImpactCaptureHelper.capture(requireContext(), carName);
                }

                String titulo = "Impacto detectado";
                String mensaje = "Tu vehículo " + carName + " ha recibido un impacto.";

                return new Notificacion(
                        titulo,
                        mensaje,
                        fecha,
                        R.drawable.ic_info,
                        carName,
                        "Impacto",
                        ts
                );
            }


            case "alerta_sonido": {
                Long activar = ev.getLong("activar");
                boolean on = activar != null && activar == 1;
                if (!on) return null; // IGNORA OFF

                String titulo = "Alertas activadas";
                String mensaje = "Se han activado las alertas luminosas y sonoras de " + carName + ".";
                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_sonido, carName, "Alarmas", ts);
            }


            case "safe_mode": {
                Long activar = ev.getLong("activar");
                boolean on = activar != null && activar == 1;

                String titulo = "Modo seguro";
                String mensaje = on
                        ? "Activado en " + carName + "."
                        : "Desactivado en " + carName + ".";

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_escudo, carName, "Seguridad", ts);
            }


            // Si en algún momento guardas eventos de puerta:
            // { tipo:"puerta", puerta:"open"/"closed", timestamp:... }
            case "puerta": {
                String puerta = ev.getString("estado");
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

        for (Notificacion n : allNotificaciones) {

            boolean okCar = "Todos".equals(filterCar) || filterCar.equals(n.getCarName());
            boolean okType = "Todos".equals(filterType) || filterType.equals(n.getCategory());

            boolean okTime = true;
            if (filterFromTs > 0L) okTime = n.getTimestamp() >= filterFromTs;
            if (okTime && filterToTs > 0L) okTime = n.getTimestamp() <= filterToTs;

            if (okCar && okType && okTime) {
                notificaciones.add(n);
            }
        }

        adapter.notifyDataSetChanged();
    }


    private void openFilterPopup() {

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext()).inflate(R.layout.filter_notifications, null);
        dialog.setContentView(sheet);

        Spinner spCar = sheet.findViewById(R.id.spFilterCar);
        Spinner spType = sheet.findViewById(R.id.spFilterType);

        TextView tvFrom = sheet.findViewById(R.id.tvFromDate);
        TextView tvTo = sheet.findViewById(R.id.tvToDate);

        TextView btnApply = sheet.findViewById(R.id.btnApplyFilters);
        TextView btnClear = sheet.findViewById(R.id.btnClearFilters);

        TextView btnClose = sheet.findViewById(R.id.btnCloseSheet);
        TextView btnClearDates = sheet.findViewById(R.id.tvQuickClearDates);

        // ---- coches ----
        Set<String> carsSet = new LinkedHashSet<>();
        carsSet.add("Todos");
        for (Notificacion n : allNotificaciones) {
            if (n.getCarName() != null && !n.getCarName().trim().isEmpty()) {
                carsSet.add(n.getCarName().trim());
            }
        }
        List<String> cars = new ArrayList<>(carsSet);

        // ---- tipos ----
        List<String> types = new ArrayList<>();
        types.add("Todos");
        types.add("Alarmas");
        types.add("Puertas");
        types.add("Impacto");
        types.add("Seguridad");

        ArrayAdapter<String> carAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, cars);
        carAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCar.setAdapter(carAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        spCar.setSelection(Math.max(0, cars.indexOf(filterCar)));
        spType.setSelection(Math.max(0, types.indexOf(filterType)));

        // mostrar fechas actuales
        tvFrom.setText(filterFromTs > 0 ? ("Desde: " + formatOnlyDate(filterFromTs)) : "Desde: --/--/----");
        tvTo.setText(filterToTs > 0 ? ("Hasta: " + formatOnlyDate(filterToTs)) : "Hasta: --/--/----");

        tvFrom.setOnClickListener(v -> openDatePicker(true, (ts) -> {
            filterFromTs = startOfDay(ts);
            tvFrom.setText("Desde: " + formatOnlyDate(filterFromTs));

            // si hasta < desde, lo limpiamos
            if (filterToTs > 0 && filterToTs < filterFromTs) {
                filterToTs = 0;
                tvTo.setText("Hasta: --/--/----");
            }
        }));

        tvTo.setOnClickListener(v -> openDatePicker(false, (ts) -> {
            // fin del día para incluir ese día completo
            filterToTs = endOfDay(ts);
            tvTo.setText("Hasta: " + formatOnlyDate(filterToTs));

            // si desde > hasta, lo limpiamos
            if (filterFromTs > 0 && filterToTs < filterFromTs) {
                filterFromTs = 0;
                tvFrom.setText("Desde: --/--/----");
            }
        }));

        btnClearDates.setOnClickListener(v -> {
            filterFromTs = 0L;
            filterToTs = 0L;
            tvFrom.setText("Desde: --/--/----");
            tvTo.setText("Hasta: --/--/----");
        });

        btnApply.setOnClickListener(v -> {
            filterCar = (String) spCar.getSelectedItem();
            filterType = (String) spType.getSelectedItem();

            applyFilters();
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            filterCar = "Todos";
            filterType = "Todos";
            filterFromTs = 0L;
            filterToTs = 0L;

            applyFilters();
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private interface DatePickedCallback {
        void onPicked(long ts);
    }

    private void openDatePicker(boolean isFrom, DatePickedCallback cb) {
        Calendar c = Calendar.getInstance();

        long base = isFrom ? filterFromTs : filterToTs;
        if (base > 0) c.setTimeInMillis(base);

        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    cb.onPicked(picked.getTimeInMillis());
                }, y, m, d);

        dialog.show();
    }

    private String formatOnlyDate(long ts) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(ts));
    }

    private long startOfDay(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long endOfDay(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }


}
