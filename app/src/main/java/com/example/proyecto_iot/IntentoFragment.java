package com.example.proyecto_iot;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;
import uk.co.deanwild.materialshowcaseview.target.Target;
import uk.co.deanwild.materialshowcaseview.shape.Shape;

public class IntentoFragment extends Fragment {

    // =========================
    //      FIELDS / UI
    // =========================
    private FirebaseFirestore firestore;
    private ListenerRegistration estadoListener;
    
    // UI Elements
    private Spinner spinnerCars;
    private ImageView ivCar;
    private FrameLayout flLock;
    private ImageView ivLock;
    private TextView tvLockState;
    private ImageView ivSignals;
    private FrameLayout flSignals;
    private TextView tvAddress;
    private View addressPillView;
    private View viewSystemDot;
    private TextView tvSystemStatus;
    private TextView tvLastUpdate;
    private TextView tvWeatherTitle;
    private TextView tvWeatherDesc;
    private ImageView ivWeather;

    // Data
    private List<String> carNames = new ArrayList<>();
    private List<String> popupItems = new ArrayList<>();
    private static final String PREF_KEY_SELECTED_CAR = "selected_car_index";

    // =========================
    //      ESTADO
    // =========================
    private boolean puertasHabilitadas = true;
    private boolean alarmaHabilitada = true;


    private boolean isLocked = true;
    private int currentCarIndex = 0;
    private boolean spinnerInicializado = false;

    private String ultimaPuerta = null;
    private boolean ultimoImpacto = false;

    // Listener robusto (Código2)
    private boolean primerSnapshotEstado = true;
    private String ultimoCarIdListener = null;

    // =========================
    //      SEÑALES
    // =========================
    private ValueAnimator signalsBlinkAnimator;
    private Handler signalsStopHandler = new Handler(Looper.getMainLooper());
    private Runnable signalsStopRunnable;
    private Vibrator vibrator;

    // =========================
    //      MODO SEGURO
    // =========================
    private LinearLayout layoutSafeMode;
    private ImageView ivSafe;
    private TextView tvSafeState;
    private LinearLayout layoutCamarasRef; // para poder desactivar cámaras desde aquí
    private boolean safeModeEnabled = true; // por defecto ON (cámbialo si quieres)
    private ListenerRegistration safeModeListener;


    // =========================
    //      LIFECYCLE
    // =========================
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intento, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (estadoListener != null) {
            estadoListener.remove();
            estadoListener = null;
        }

        stopSignalsEffects();
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (safeModeListener != null) {
            safeModeListener.remove();
            safeModeListener = null;
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Mantener comportamiento del Código1
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        firestore = FirebaseFirestore.getInstance();
        Context context = requireContext();
        SharedPreferences prefs = getPrefs(context);

        bindViews(view);
        setupSpinner(context, prefs);
        setupClicks(context, prefs);

        int savedPosition = prefs.getInt(PREF_KEY_SELECTED_CAR, 0);
        currentCarIndex = savedPosition;

        // Estado candado inicial
        isLocked = prefs.getBoolean(getLockPrefKeyForIndex(currentCarIndex), true);
        updateLockUi();

        loadCarsFromFirestore(savedPosition, (ArrayAdapter<String>) spinnerCars.getAdapter());

        // Mostrar tutorial si es la primera vez
        showTutorial(context, prefs);
    }

    private void showTutorial(Context context, SharedPreferences prefs) {
        // boolean tutorialShown = prefs.getBoolean("tutorial_shown", false);
        // if (tutorialShown) return;

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500); // medio segundo de espera
        config.setShapePadding(10); // padding general

        // ID dinámico para que se muestre siempre (modo pruebas), cámbialo a fijo luego
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(requireActivity(), String.valueOf(System.currentTimeMillis()));
        sequence.setConfig(config);

        // 1. Actions Row
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(requireActivity())
                        .setTarget(requireView().findViewById(R.id.actionsRow))
                        .setDismissText("ENTENDIDO")
                        .setContentText("Panel de Control\nEsta es la barra de acciones, aquí podrás controlar el coche y verificar su estado en tiempo real.")
                        .setShape(new RoundedRectangleShape(50)) // Radio 50
                        .setDismissOnTouch(true)
                        .setMaskColour(getResources().getColor(R.color.tutorial_mask, null))
                        .build()
        );

        // 2. Vehicle Selector
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(requireActivity())
                        .setTarget(requireView().findViewById(R.id.vehicleSelector))
                        .setDismissText("ENTENDIDO")
                        .setContentText("Selección de Vehículo\nPulsa aquí para cambiar de vehículo si tienes más de uno configurado.")
                        .setShape(new RoundedRectangleShape(50))
                        .setDismissOnTouch(true)
                        .setMaskColour(getResources().getColor(R.color.tutorial_mask, null))
                        .build()
        );

        // 3. Address Pill
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(requireActivity())
                        .setTarget(requireView().findViewById(R.id.addressPill))
                        .setDismissText("ENTENDIDO")
                        .setContentText("Ubicación en vivo\nAquí puedes ver la ubicación exacta de tu coche en tiempo real.")
                        .setShape(new RoundedRectangleShape(50))
                        .setDismissOnTouch(true)
                        .setMaskColour(getResources().getColor(R.color.tutorial_mask, null))
                        .build()
        );

        sequence.start();
    }

    /**
     * Clase auxiliar para rectángulos redondeados con MaterialShowcaseView
     */
    private static class RoundedRectangleShape implements uk.co.deanwild.materialshowcaseview.shape.Shape {
        private final int radius;
        private int width = 0;
        private int height = 0;
        private final android.graphics.RectF rect = new android.graphics.RectF();
        private int padding;

        public RoundedRectangleShape(int radius) {
            this.radius = radius;
        }

        @Override
        public void setPadding(int padding) {
            this.padding = padding;
        }

        @Override
        public void updateTarget(uk.co.deanwild.materialshowcaseview.target.Target target) {
            if (target != null && target.getBounds() != null) {
                width = target.getBounds().width();
                height = target.getBounds().height();
            }
        }

        @Override
        public void draw(android.graphics.Canvas canvas, android.graphics.Paint paint, int x, int y) {
            if (width > 0 && height > 0) {
                rect.set(x - width / 2 - padding, y - height / 2 - padding, x + width / 2 + padding, y + height / 2 + padding);
                canvas.drawRoundRect(rect, radius, radius, paint);
            }
        }

        @Override
        public int getWidth() { return width; }

        @Override
        public int getHeight() { return height; }

        // Note: Library 1.3.7 might require this method.
        // Returning radius related to size (e.g. for ripple calc).
        // Since it's a rectangle, half of diagonal or max dim is reasonable.
        public int getTotalRadius() {
            return (int) Math.sqrt(width * width + height * height) / 2;
        }
    }



    // =========================
    //      BIND / SETUP
    // =========================
    private void bindViews(View view) {
        spinnerCars = view.findViewById(R.id.spinnerCars);
        ivCar = view.findViewById(R.id.ivCar);

        flLock = view.findViewById(R.id.flLock);
        ivLock = view.findViewById(R.id.ivLock);
        tvLockState = view.findViewById(R.id.tvLockState);

        ivSignals = view.findViewById(R.id.ivSignals);
        flSignals = (FrameLayout) ivSignals.getParent();

        tvAddress = view.findViewById(R.id.tvAddress);
        addressPillView = view.findViewById(R.id.addressPill);

        layoutSafeMode = view.findViewById(R.id.layoutSafeMode);
        ivSafe = view.findViewById(R.id.ivSafe);
        tvSafeState = view.findViewById(R.id.tvSafeState);

        // referencia para poder desactivar/activar cámaras
        layoutCamarasRef = view.findViewById(R.id.layoutCamaras);
        viewSystemDot = view.findViewById(R.id.viewSystemDot);
        tvSystemStatus = view.findViewById(R.id.tvSystemStatus);
        tvLastUpdate = view.findViewById(R.id.tvLastUpdate);

        tvWeatherTitle = view.findViewById(R.id.tvWeatherTitle);
        tvWeatherDesc = view.findViewById(R.id.tvWeatherDesc);
        ivWeather = view.findViewById(R.id.ivWeather);

    }
    private void actualizarUiClima(String clima) {

        if (clima == null) clima = "";
        clima = clima.toLowerCase(Locale.ROOT);
        if (
                clima.contains("rain") ||
                        clima.contains("drizzle") ||
                        clima.contains("lluvia") ||
                        clima.contains("lluvioso") ||
                        clima.contains("storm") ||
                        clima.contains("thunder") ||
                        clima.contains("tormenta") ||
                        clima.contains("snow") ||
                        clima.contains("nieve") ||
                        clima.contains("fog") ||
                        clima.contains("mist") ||
                        clima.contains("haze") ||
                        clima.contains("smoke") ||
                        clima.contains("niebla") ||
                        clima.contains("bruma")
        ) {
            tvWeatherDesc.setText("Conduce con precaución");
            ivWeather.setImageResource(R.drawable.ic_tiempo3);
        }
        else if (
                clima.contains("cloud") ||
                        clima.contains("clouds") ||
                        clima.contains("overcast") ||
                        clima.contains("broken") ||
                        clima.contains("scattered") ||
                        clima.contains("few clouds") ||
                        clima.contains("nube") ||
                        clima.contains("nubes") ||
                        clima.contains("nuboso") ||
                        clima.contains("cubierto")
        ) {
            tvWeatherDesc.setText("Condiciones normales");
            ivWeather.setImageResource(R.drawable.ic_tiempo2);
        }
        else if (
                clima.contains("clear") ||
                        clima.contains("clear sky") ||
                        clima.contains("sun") ||
                        clima.contains("sunny") ||
                        clima.contains("cielo") ||
                        clima.contains("despejado") ||
                        clima.contains("soleado")
        ) {
            tvWeatherDesc.setText("Puedes conducir con seguridad");
            ivWeather.setImageResource(R.drawable.ic_tiempo);
        }

        else {
            tvWeatherDesc.setText("Consulta el estado del clima");
            ivWeather.setImageResource(R.drawable.ic_warning);
        }
    }
    private void setupSpinner(Context context, SharedPreferences prefs) {

        ArrayAdapter<String> carsAdapter = new ArrayAdapter<String>(
                context, R.layout.spinner_coches, carNames
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Mantener el “valor visible” como el coche actual (Código1/2)
                return super.getView(currentCarIndex, convertView, parent);
            }

            @Override
            public int getCount() {
                return popupItems.size();
            }

            @Override
            public String getItem(int position) {
                return popupItems.get(position);
            }
        };
        carsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCars.setAdapter(carsAdapter);
    }

    // =========================
    //      MISSING METHODS IMPLEMENTATION
    // =========================

    private SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("ProyectoIoT_Prefs", Context.MODE_PRIVATE);
    }

    private void createNotificationChannel() {
        // Implementación básica del canal de notificaciones si es necesario
    }

    private void stopSignalsEffects() {
        if (signalsBlinkAnimator != null) {
            signalsBlinkAnimator.cancel();
            signalsBlinkAnimator = null;
        }
        if (signalsStopHandler != null && signalsStopRunnable != null) {
            signalsStopHandler.removeCallbacks(signalsStopRunnable);
        }
    }

    private void setupClicks(Context context, SharedPreferences prefs) {
        // Implementación de los clicks listeners para botones como flLock, etc.
        flLock.setOnClickListener(v -> toggleLock(prefs));
    }

    private void toggleLock(SharedPreferences prefs) {
        isLocked = !isLocked;
        prefs.edit().putBoolean(getLockPrefKeyForIndex(currentCarIndex), isLocked).apply();
        updateLockUi();
    }

    private void loadCarsFromFirestore(int savedPosition, ArrayAdapter<String> adapter) {
         // Placeholder: Cargar coches desde Firestore y actualizar la lista carNames
         // Por ahora añadimos un coche dummy si está vacía
         if(carNames.isEmpty()) {
             carNames.add("Mi Coche");
             popupItems.add("Mi Coche");
             adapter.notifyDataSetChanged();
         }
    }

    private String getLockPrefKeyForIndex(int index) {
        return "lock_state_" + index;
    }

    private void updateLockUi() {
        if (isLocked) {
           tvLockState.setText("Bloqueado");
           ivLock.setImageResource(R.drawable.ic_candado_cerrado); 
           // Color o estilo visual
        } else {
           tvLockState.setText("Desbloqueado");
           ivLock.setImageResource(R.drawable.ic_candado_abierto);
        }
    }
}
