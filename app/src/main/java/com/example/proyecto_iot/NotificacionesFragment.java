package com.example.proyecto_iot;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
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
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
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

    // filtros
    private String filterCar = "Todos";
    private String filterType = "Todos";
    private long filterFromTs = 0L;
    private long filterToTs = 0L;

    // ======================================================
    // ===================== LIFECYCLE ======================
    // ======================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        firestore = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recycler_notificaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificacionAdapter(notificaciones);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btnFilter).setOnClickListener(v -> openFilterPopup());

        view.findViewById(R.id.btnDownload).setOnClickListener(v -> {
            if (notificaciones.isEmpty()) {
                Toast.makeText(getContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            } else {
                generarPdfItext();
            }
        });

        cargarHistorialFirestore();
        return view;
    }

    // ======================================================
    // ===================== FIRESTORE ======================
    // ======================================================

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
                    HashMap<String, String> carIdToLoc = new HashMap<>();
                    List<String> carIds = new ArrayList<>();

                    for (QueryDocumentSnapshot carDoc : carsSnap) {
                        String carId = carDoc.getId();
                        String nombre = carDoc.getString("Nombre");
                        Double lat = carDoc.getDouble("lat");
                        Double lng = carDoc.getDouble("lng");

                        carIds.add(carId);
                        carIdToName.put(carId,
                                (nombre != null && !nombre.trim().isEmpty())
                                        ? nombre.trim()
                                        : "Vehículo");

                        carIdToLoc.put(carId, formatLoc(requireContext(), lat, lng));
                    }

                    List<Task<?>> tasks = new ArrayList<>();
                    ArrayList<ItemTemp> temp = new ArrayList<>();

                    for (String carId : carIds) {
                        tasks.add(
                                firestore.collection("Coches")
                                        .document(carId)
                                        .collection("eventos")
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .limit(300)
                                        .get()
                                        .addOnSuccessListener(events -> {
                                            for (QueryDocumentSnapshot ev : events) {
                                                Notificacion n = buildNotificacionFromEvento(
                                                        ev.getString("tipo"),
                                                        ev,
                                                        carIdToName.get(carId),
                                                        carIdToLoc.get(carId),
                                                        ev.getLong("timestamp")
                                                );
                                                if (n != null) {
                                                    temp.add(new ItemTemp(n, n.getTimestamp()));
                                                }
                                            }
                                        })
                        );
                    }

                    Tasks.whenAllComplete(tasks).addOnSuccessListener(done -> {
                        Collections.sort(temp, (a, b) -> Long.compare(b.ts, a.ts));
                        allNotificaciones.clear();
                        for (ItemTemp it : temp) allNotificaciones.add(it.notif);
                        applyFilters();
                    });
                });
    }

    // ======================================================
    // ===================== FILTROsS ========================
    // ======================================================

    private void applyFilters() {
        notificaciones.clear();

        for (Notificacion n : allNotificaciones) {

            boolean okCar = "Todos".equals(filterCar) || filterCar.equals(n.getCarName());
            boolean okType = "Todos".equals(filterType) || filterType.equals(n.getCategory());

            boolean okTime = true;
            if (filterFromTs > 0) okTime = n.getTimestamp() >= filterFromTs;
            if (okTime && filterToTs > 0) okTime = n.getTimestamp() <= filterToTs;

            if (okCar && okType && okTime) {
                notificaciones.add(n);
            }
        }

        adapter.notifyDataSetChanged();
    }

    // ======================================================
    // ===================== FILTER UI ======================
    // ======================================================

    private void openFilterPopup() {

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.filter_notifications, null);
        dialog.setContentView(sheet);

        Spinner spCar = sheet.findViewById(R.id.spFilterCar);
        Spinner spType = sheet.findViewById(R.id.spFilterType);

        TextView tvFrom = sheet.findViewById(R.id.tvFromDate);
        TextView tvTo = sheet.findViewById(R.id.tvToDate);

        TextView btnApply = sheet.findViewById(R.id.btnApplyFilters);
        TextView btnClear = sheet.findViewById(R.id.btnClearFilters);
        TextView btnClose = sheet.findViewById(R.id.btnCloseSheet);
        TextView btnClearDates = sheet.findViewById(R.id.tvQuickClearDates);

        // coches
        Set<String> carsSet = new LinkedHashSet<>();
        carsSet.add("Todos");
        for (Notificacion n : allNotificaciones) carsSet.add(n.getCarName());
        List<String> cars = new ArrayList<>(carsSet);

        spCar.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                cars
        ));
        spCar.setSelection(Math.max(0, cars.indexOf(filterCar)));

        // tipos
        List<String> types = List.of(
                "Todos", "Alarmas", "Puertas", "Impacto",
                "Seguridad", "Movimiento", "RFID", "Otros"
        );
        spType.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                types
        ));
        spType.setSelection(Math.max(0, types.indexOf(filterType)));

        tvFrom.setText(filterFromTs > 0 ? "Desde: " + formatOnlyDate(filterFromTs) : "Desde: --/--/----");
        tvTo.setText(filterToTs > 0 ? "Hasta: " + formatOnlyDate(filterToTs) : "Hasta: --/--/----");

        tvFrom.setOnClickListener(v ->
                openDatePicker(ts -> {
                    filterFromTs = startOfDay(ts);
                    tvFrom.setText("Desde: " + formatOnlyDate(filterFromTs));
                })
        );

        tvTo.setOnClickListener(v ->
                openDatePicker(ts -> {
                    filterToTs = endOfDay(ts);
                    tvTo.setText("Hasta: " + formatOnlyDate(filterToTs));
                })
        );

        btnClearDates.setOnClickListener(v -> {
            filterFromTs = 0L;
            filterToTs = 0L;
            tvFrom.setText("Desde: --/--/----");
            tvTo.setText("Hasta: --/--/----");
        });

        btnApply.setOnClickListener(v -> {
            filterCar = spCar.getSelectedItem().toString();
            filterType = spType.getSelectedItem().toString();
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

    // ======================================================
    // ====================== PDF ===========================
    // ======================================================

    private void generarPdfItext() {
        try {
            File dir = new File(requireContext().getCacheDir(), "reportes");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "Reporte_Notificaciones.pdf");

            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            doc.add(new Paragraph("REPORTE DE NOTIFICACIONES - IOT CAR")
                    .setBold().setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("Filtros: " + filterCar + " / " + filterType)
                    .setFontSize(11).setItalic().setMarginBottom(10));

            Table table = new Table(UnitValue.createPercentArray(new float[]{25f, 25f, 50f}))
                    .useAllAvailableWidth();

            table.addHeaderCell(header("Fecha"));
            table.addHeaderCell(header("Vehículo"));
            table.addHeaderCell(header("Evento"));

            for (Notificacion n : notificaciones) {
                table.addCell(n.getFecha());
                table.addCell(n.getCarName());
                table.addCell(n.getTitulo());
            }

            doc.add(table);
            doc.close();

            abrirPdf(file);

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Cell header(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
    }

    private void abrirPdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.proyecto_iot.fileprovider",
                    file
            );

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/pdf");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);

        } catch (Exception e) {
            Toast.makeText(getContext(), "No se pudo abrir el PDF", Toast.LENGTH_LONG).show();
        }
    }

    // ======================================================
    // ===================== BUILD EVENT ====================
    // ======================================================

    private Notificacion buildNotificacionFromEvento(
            String tipo,
            QueryDocumentSnapshot ev,
            String carName,
            String loc,
            Long ts
    ) {

        if (tipo == null || ts == null) return null;

        String fecha = formatFecha(ts);

        switch (tipo) {

            case "impacto":
                return new Notificacion(
                        "Impacto detectado",
                        "Impacto en " + carName + (loc.isEmpty() ? "" : "\n" + loc),
                        fecha,
                        R.drawable.ic_info,
                        carName,
                        "Impacto",
                        ts
                );

            case "alerta_sonido": {
                Long activar = ev.getLong("activar");
                if (activar == null || activar != 1) return null;

                return new Notificacion(
                        "Alertas activadas",
                        "Alertas activadas en " + carName + (loc.isEmpty() ? "" : "\n" + loc),
                        fecha,
                        R.drawable.ic_sonido,
                        carName,
                        "Alarmas",
                        ts
                );
            }

            case "safe_mode": {
                Long activar = ev.getLong("activar");
                boolean on = activar != null && activar == 1;

                return new Notificacion(
                        "Modo seguro",
                        (on ? "Activado" : "Desactivado") + " en " + carName + (loc.isEmpty() ? "" : "\n" + loc),
                        fecha,
                        R.drawable.ic_escudo,
                        carName,
                        "Seguridad",
                        ts
                );
            }

            case "movimiento":
                return new Notificacion(
                        "Movimiento detectado",
                        "Movimiento en " + carName + (loc.isEmpty() ? "" : "\n" + loc),
                        fecha,
                        R.drawable.ic_info,
                        carName,
                        "Movimiento",
                        ts
                );

            case "rfid": {
                Boolean autorizado = ev.getBoolean("autorizado");
                boolean ok = autorizado != null && autorizado;

                return new Notificacion(
                        ok ? "RFID autorizado" : "RFID denegado",
                        carName + (loc.isEmpty() ? "" : "\n" + loc),
                        fecha,
                        R.drawable.ic_info,
                        carName,
                        "RFID",
                        ts
                );
            }

            default:
                return new Notificacion(
                        "Evento: " + tipo,
                        "Evento en " + carName + (loc.isEmpty() ? "" : "\n" + loc),
                        fecha,
                        R.drawable.ic_info,
                        carName,
                        "Otros",
                        ts
                );
        }
    }

    // ======================================================
    // ===================== UTILS ==========================
    // ======================================================

    private void openDatePicker(DatePickedCallback cb) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (v, y, m, d) -> {
                    Calendar p = Calendar.getInstance();
                    p.set(y, m, d);
                    cb.onPicked(p.getTimeInMillis());
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private interface DatePickedCallback {
        void onPicked(long ts);
    }

    private String formatFecha(long ts) {
        return new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault()).format(new Date(ts));
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

    private String formatLoc(Context ctx, Double lat, Double lng) {
        if (lat == null || lng == null) return "";

        try {
            Geocoder g = new Geocoder(ctx, Locale.getDefault());
            List<Address> a = g.getFromLocation(lat, lng, 1);
            if (a != null && !a.isEmpty()) {
                Address ad = a.get(0);
                return ad.getThoroughfare() + " " +
                        ad.getSubThoroughfare() + ", " +
                        ad.getLocality();
            }
        } catch (Exception ignored) {}

        return String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
    }

    private static class ItemTemp {
        Notificacion notif;
        long ts;
        ItemTemp(Notificacion n, long t) { notif = n; ts = t; }
    }
}
