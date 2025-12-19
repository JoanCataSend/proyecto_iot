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

import com.example.proyecto_iot.utils.ImpactCaptureHelper;
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
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        firestore = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recycler_notificaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificacionAdapter(notificaciones);
        recyclerView.setAdapter(adapter);

        View btnFilter = view.findViewById(R.id.btnFilter);
        View btnDownload = view.findViewById(R.id.btnDownload);

        btnFilter.setOnClickListener(v -> openFilterPopup());

        btnDownload.setOnClickListener(v -> {
            recyclerView.smoothScrollToPosition(0);
            if (notificaciones.isEmpty()) {
                Toast.makeText(getContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            } else {
                generarPdfItext();
            }
        });

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

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
                        allNotificaciones.clear();
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

                        carIdToName.put(
                                carId,
                                (nombre != null && !nombre.trim().isEmpty())
                                        ? nombre.trim()
                                        : "Vehículo"
                        );

                        carIdToLoc.put(carId, formatLoc(requireContext(), lat, lng));
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
                                        String loc = carIdToLoc.get(carId);

                                        Notificacion n = buildNotificacionFromEvento(
                                                tipo,
                                                ev,
                                                carName,
                                                loc,
                                                ts
                                        );

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
                                for (ItemTemp it : temp) allNotificaciones.add(it.notif);

                                applyFilters();
                            })
                            .addOnFailureListener(e -> {
                                notificaciones.clear();
                                allNotificaciones.clear();
                                adapter.notifyDataSetChanged();
                            });
                })
                .addOnFailureListener(e -> {
                    notificaciones.clear();
                    allNotificaciones.clear();
                    adapter.notifyDataSetChanged();
                });
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

        if (carName == null || carName.trim().isEmpty()) carName = "Vehículo";
        if (loc == null) loc = "";

        String fecha = formatFecha(ts);

        switch (tipo) {

            case "impacto": {

                // Mantener funcionalidad extra: captura sólo si el impacto es muy reciente
                long ahora = System.currentTimeMillis();
                if (ahora - ts < 10_000) {
                    ImpactCaptureHelper.capture(requireContext(), carName);
                }

                String titulo = "Impacto detectado";
                String mensaje = "Tu vehículo " + carName + " ha recibido un impacto.";
                if (!loc.isEmpty()) mensaje += "\n" + loc;

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
                if (!loc.isEmpty()) mensaje += "\n" + loc;

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_sonido, carName, "Alarmas", ts);
            }

            case "safe_mode": {
                Long activar = ev.getLong("activar");
                boolean on = activar != null && activar == 1;

                String titulo = "Modo seguro";
                String mensaje = on ? "Activado en " + carName + "." : "Desactivado en " + carName + ".";
                if (!loc.isEmpty()) mensaje += "\n" + loc;

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_escudo, carName, "Seguridad", ts);
            }

            case "movimiento": {
                String titulo = "Movimiento detectado";
                String mensaje = "El sensor de movimiento detectó actividad en " + carName + ".";
                if (!loc.isEmpty()) mensaje += "\n" + loc;

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info, carName, "Movimiento", ts);
            }

            case "puerta": {
                String estado = ev.getString("estado");
                boolean open = "open".equals(estado);

                String titulo = open ? "Puertas abiertas" : "Puertas bloqueadas";
                String mensaje = open
                        ? "El coche (" + carName + ") se ha desbloqueado."
                        : "El coche (" + carName + ") se ha bloqueado.";
                if (!loc.isEmpty()) mensaje += "\n" + loc;

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info, carName, "Puertas", ts);
            }

            case "rfid": {
                Boolean autorizado = ev.getBoolean("autorizado");
                boolean ok = autorizado != null && autorizado;

                String titulo = ok ? "RFID: acceso permitido" : "RFID: acceso denegado";
                String mensaje = ok
                        ? "Se ha autorizado el acceso en " + carName + "."
                        : "Se ha denegado el acceso en " + carName + ".";
                if (!loc.isEmpty()) mensaje += "\n" + loc;

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info, carName, "RFID", ts);
            }

            default: {
                String titulo = "Evento: " + tipo;
                String mensaje = "Notificación del coche (" + carName + ").";
                if (!loc.isEmpty()) mensaje += "\n" + loc;

                return new Notificacion(titulo, mensaje, fecha, R.drawable.ic_info, carName, "Otros", ts);
            }
        }
    }

    // ======================================================
    // ===================== FILTROS ========================
    // ======================================================

    private void applyFilters() {
        notificaciones.clear();

        for (Notificacion n : allNotificaciones) {

            boolean okCar = "Todos".equals(filterCar) || filterCar.equals(n.getCarName());
            boolean okType = "Todos".equals(filterType) || filterType.equals(n.getCategory());

            boolean okTime = true;
            if (filterFromTs > 0L) okTime = n.getTimestamp() >= filterFromTs;
            if (okTime && filterToTs > 0L) okTime = n.getTimestamp() <= filterToTs;

            if (okCar && okType && okTime) notificaciones.add(n);
        }

        adapter.notifyDataSetChanged();
    }

    // ======================================================
    // ===================== FILTER UI ======================
    // ======================================================

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

        // coches
        Set<String> carsSet = new LinkedHashSet<>();
        carsSet.add("Todos");
        for (Notificacion n : allNotificaciones) {
            if (n.getCarName() != null && !n.getCarName().trim().isEmpty()) {
                carsSet.add(n.getCarName().trim());
            }
        }
        List<String> cars = new ArrayList<>(carsSet);

        // tipos
        List<String> types = new ArrayList<>();
        types.add("Todos");
        types.add("Alarmas");
        types.add("Puertas");
        types.add("Impacto");
        types.add("Seguridad");
        types.add("Movimiento");
        types.add("RFID");
        types.add("Otros");

        ArrayAdapter<String> carAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                cars
        );
        carAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCar.setAdapter(carAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                types
        );
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

            if (filterToTs > 0 && filterToTs < filterFromTs) {
                filterToTs = 0;
                tvTo.setText("Hasta: --/--/----");
            }
        }));

        tvTo.setOnClickListener(v -> openDatePicker(false, (ts) -> {
            filterToTs = endOfDay(ts);
            tvTo.setText("Hasta: " + formatOnlyDate(filterToTs));

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

            doc.add(new Paragraph("REPORTE DE NOTIFICACIONES - KöVA")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            String filtrosTxt = "Filtros: " + filterCar + " / " + filterType;
            if (filterFromTs > 0 || filterToTs > 0) {
                String desde = (filterFromTs > 0) ? formatOnlyDate(filterFromTs) : "--/--/----";
                String hasta = (filterToTs > 0) ? formatOnlyDate(filterToTs) : "--/--/----";
                filtrosTxt += "  ( " + desde + " - " + hasta + " )";
            }

            doc.add(new Paragraph(filtrosTxt)
                    .setFontSize(11)
                    .setItalic()
                    .setMarginBottom(10));

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

            Toast.makeText(getContext(), "PDF generado con éxito", Toast.LENGTH_SHORT).show();
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
    // ===================== UTILS ==========================
    // ======================================================

    private String formatFecha(long ts) {
        return new SimpleDateFormat("HH:mm  dd/MM/yy", Locale.getDefault())
                .format(new Date(ts));
    }

    private String formatOnlyDate(long ts) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new Date(ts));
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
            Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);

                String street = a.getThoroughfare();
                String number = a.getSubThoroughfare();
                String city = a.getLocality();

                StringBuilder sb = new StringBuilder();

                if (street != null) {
                    sb.append(street);
                    if (number != null) sb.append(" ").append(number);
                }
                if (city != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(city);
                }

                return sb.toString();
            }
        } catch (Exception ignored) {}

        return String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
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
