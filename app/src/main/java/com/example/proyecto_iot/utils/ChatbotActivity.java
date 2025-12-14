package com.example.proyecto_iot.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.core.widget.NestedScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.proyecto_iot.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private NestedScrollView scrollView;
    private EditText etMessage;

    private String userName = "Usuario";
    private String gpsContext = "";
    private LlamaChatServiceHttp llamaService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scrollView);
        etMessage = findViewById(R.id.etMessage);
        com.google.android.material.button.MaterialButton btnSend = findViewById(R.id.btnSend);
        ImageButton btnClose = findViewById(R.id.btnClose);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 1) Cargar nombre del usuario desde Firestore
        cargarNombreUsuario(() -> {
            // 2) Cargar GPS (opcional)
            cargarGpsContext(() -> {

                String systemPrompt =
                        "Eres un asistente virtual experto en seguridad de vehículos, especializado en el uso de esta aplicación de seguridad para coches.\n" +
                                "\n" +
                                "Conoces la aplicación al 100% y actúas como si formaras parte del equipo que la ha desarrollado. Tu objetivo principal es ayudar al usuario a entender y utilizar la app de la forma más fácil, clara y cómoda posible.\n" +
                                "\n" +
                                "La aplicación permite al usuario:\n" +
                                "- Conocer en todo momento el estado de su vehículo.\n" +
                                "- Recibir notificaciones en el móvil cuando el coche sufre un impacto o golpe.\n" +
                                "- Ver la ubicación del vehículo en tiempo real mediante GPS.\n" +
                                "- Acceder a una cámara del vehículo para ver lo que ocurre en directo.\n" +
                                "- Controlar sensores del coche como apertura y cierre de puertas.\n" +
                                "- Encender y apagar luces del vehículo desde la app.\n" +
                                "- Gestionar uno o varios vehículos desde una misma cuenta.\n" +
                                "- Recibir alertas relacionadas con la seguridad del coche.\n" +
                                "\n" +
                                "Cuando el usuario haga preguntas, debes:\n" +
                                "- Responder siempre con conocimiento completo de la app y sus funcionalidades.\n" +
                                "- Explicar las cosas de forma clara, sencilla y paso a paso si es necesario.\n" +
                                "- Dar consejos prácticos relacionados con la seguridad del vehículo.\n" +
                                "- Ayudar al usuario a interpretar alertas, notificaciones e información del GPS.\n" +
                                "- Recomendar buenas prácticas de seguridad según la situación del usuario.\n" +
                                "- Resolver dudas técnicas de uso de la app sin usar lenguaje excesivamente técnico.\n" +
                                "\n" +
                                "Comportamiento del asistente:\n" +
                                "- Dirígete siempre al usuario por su nombre cuando esté disponible.\n" +
                                "- Mantén un tono cercano, profesional y humano, como si fueras un experto en seguridad de vehículos hablando con el propietario del coche.\n" +
                                "- Sé claro, educado y tranquilizador, especialmente en situaciones de alerta o impacto.\n" +
                                "- No inventes funcionalidades que no existen en la app.\n" +
                                "- Si una función no está disponible, explícalo con claridad y ofrece alternativas dentro de la app.\n" +
                                "\n" +
                                "Si se te proporciona información de ubicación (GPS del usuario o del vehículo):\n" +
                                "- Úsala para dar recomendaciones útiles (por ejemplo, seguridad de la zona, comprobar el estado del coche, revisar alertas recientes).\n" +
                                "- Nunca alarmes innecesariamente; ofrece siempre soluciones o pasos a seguir.\n" +
                                "\n" +
                                "Tu misión es que el usuario se sienta seguro, informado y cómodo usando la aplicación en todo momento.\n" +
                                "Dirígete al usuario por su nombre: " + userName + ". " +
                                (TextUtils.isEmpty(gpsContext) ? "" : ("Contexto de ubicación actual: " + gpsContext + ". "));

                llamaService = new LlamaChatServiceHttp(systemPrompt);

                addBotMessage("Hola " + userName + ", soy tu asistente. Pregúntame cualquier duda sobre la app o seguridad.");
            });
        });

        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(msg)) return;

            addUserMessage(msg);
            etMessage.setText("");

            // Mensaje “pensando…”
            TextView typing = addBotMessageReturn("IA: ...");

            executor.execute(() -> {
                try {
                    String response = llamaService.ask(msg);

                    runOnUiThread(() -> {
                        chatContainer.removeView(typing);
                        addBotMessage(response);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        chatContainer.removeView(typing);
                        addBotMessage("Ahora mismo no puedo responder (error de conexión o API). Prueba de nuevo.");
                    });
                }
            });
        });

        btnClose.setOnClickListener(v -> finish());
    }

    // =======================
    // Nombre desde Firestore
    // =======================
    private void cargarNombreUsuario(Runnable onDone) {
        if (mAuth.getCurrentUser() == null) {
            onDone.run();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("Usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombre = doc.getString("Usuario");
                        if (!TextUtils.isEmpty(nombre)) userName = nombre;
                    }
                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }

    // =======================
    // GPS contexto (móvil)
    // =======================
    private void cargarGpsContext(Runnable onDone) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            onDone.run();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        gpsContext = formatLocation(location);
                    }
                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }
    /*private void cargarGpsContext(Runnable onDone) {
        try {
            // tu código actual
        } catch (Exception e) {
            onDone.run();
        }
    }*/


    private String formatLocation(Location loc) {
        return "lat=" + loc.getLatitude() + ", lon=" + loc.getLongitude();
    }

    // =======================
    // UI: mensajes
    // =======================
    private void addUserMessage(String text) {
        TextView tv = new TextView(this);
        tv.setText(userName + ": " + text);
        tv.setTextSize(14f);
        tv.setPadding(24, 16, 24, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(80, 16, 16, 8);
        params.gravity = Gravity.END;
        tv.setLayoutParams(params);

        tv.setBackgroundColor(0xFFE0F7FA);
        chatContainer.addView(tv);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        addBotMessageReturn("IA: " + text);
    }

    private TextView addBotMessageReturn(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setPadding(24, 16, 24, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 80, 8);
        params.gravity = Gravity.START;
        tv.setLayoutParams(params);

        tv.setBackgroundColor(0xFFF1F1F1);
        chatContainer.addView(tv);
        scrollToBottom();
        return tv;
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
