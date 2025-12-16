package com.example.proyecto_iot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iot.utils.CustomToast;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SeguridadFragment extends Fragment {

    // UI
    private CheckBox checkPuertas, checkVentanas, checkMovimiento,
            checkAlarma, checkLuces, checkBloqueo;
    private Button btnGuardar;

    // Sistema
    private Vibrator vibrator;
    private FirebaseFirestore firestore;

    // Coche activo
    private String carId;

    // SharedPreferences (solo para leer el coche activo)
    private static final String PREFS_NAME = "notificaciones_prefs";
    private static final String PREF_KEY_SELECTED_CAR_ID = "vehiculo_seleccionado_id";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_seguridad, container, false);

        // ====== UI ======
        checkPuertas    = view.findViewById(R.id.checkPuertas);
        checkVentanas   = view.findViewById(R.id.checkVentanas);
        checkMovimiento = view.findViewById(R.id.checkMovimiento);
        checkAlarma     = view.findViewById(R.id.checkAlarma);
        checkLuces      = view.findViewById(R.id.checkLuces);
        checkBloqueo    = view.findViewById(R.id.checkBloqueo);
        btnGuardar      = view.findViewById(R.id.btnGuardar);

        Context context = requireContext();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        firestore = FirebaseFirestore.getInstance();

        // ====== Obtener coche activo ======
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        carId = prefs.getString(PREF_KEY_SELECTED_CAR_ID, null);

        if (carId == null) {
            CustomToast.error(requireActivity(),
                    "No se ha podido determinar el vehículo activo");
            btnGuardar.setEnabled(false);
            return view;
        }

        // ====== Leer seguridad desde Firebase ======
        cargarSeguridadDesdeFirebase();

        btnGuardar.setOnClickListener(v -> {
            guardarSeguridadEnFirebase();
            vibrar();
            CustomToast.success(requireActivity(),
                    "Cambios guardados correctamente");
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }
    }

    // =========================
    //     FIREBASE SEGURIDAD
    // =========================
    private void cargarSeguridadDesdeFirebase() {

        firestore.collection("Coches")
                .document(carId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    Map<String, Object> seguridad =
                            (Map<String, Object>) doc.get("seguridad");

                    if (seguridad == null) {
                        // Primera vez → valores por defecto
                        setChecks(true);
                        return;
                    }

                    checkPuertas.setChecked(getBool(seguridad, "puertas"));
                    checkVentanas.setChecked(getBool(seguridad, "ventanas"));
                    checkMovimiento.setChecked(getBool(seguridad, "movimiento"));
                    checkAlarma.setChecked(getBool(seguridad, "alarma"));
                    checkLuces.setChecked(getBool(seguridad, "luces"));
                    checkBloqueo.setChecked(getBool(seguridad, "bloqueo"));
                });
    }

    private void guardarSeguridadEnFirebase() {

        Map<String, Object> seguridad = new HashMap<>();
        seguridad.put("puertas", checkPuertas.isChecked());
        seguridad.put("ventanas", checkVentanas.isChecked());
        seguridad.put("movimiento", checkMovimiento.isChecked());
        seguridad.put("alarma", checkAlarma.isChecked());
        seguridad.put("luces", checkLuces.isChecked());
        seguridad.put("bloqueo", checkBloqueo.isChecked());

        firestore.collection("Coches")
                .document(carId)
                .update("seguridad", seguridad);
    }

    // =========================
    //          HELPERS
    // =========================
    private boolean getBool(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Boolean ? (Boolean) v : true;
    }

    private void setChecks(boolean value) {
        checkPuertas.setChecked(value);
        checkVentanas.setChecked(value);
        checkMovimiento.setChecked(value);
        checkAlarma.setChecked(value);
        checkLuces.setChecked(value);
        checkBloqueo.setChecked(value);
    }

    private void vibrar() {
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(100,
                                VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }
}
