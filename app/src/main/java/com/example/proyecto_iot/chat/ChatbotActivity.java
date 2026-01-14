package com.example.proyecto_iot.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ChatAdapter adapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnBack;
    private View typingBar;
    private ProgressBar typingProgress;
    private TextView typingText;

    private final OpenAIService openAI = new OpenAIService();

    // ✅ Nombre del usuario (viene de Firestore)
    private String userName = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        recycler = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBackChat);
        typingBar = findViewById(R.id.typingBar);
        typingProgress = findViewById(R.id.typingProgress);
        typingText = findViewById(R.id.typingText);

        adapter = new ChatAdapter();

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        // ✅ Cargar nombre de usuario desde Firestore
        loadUserName();
    }

    // =========================
    // CARGAR NOMBRE DEL USUARIO
    // =========================
    private void loadUserName() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Usuarios").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String nombre = doc.getString("Usuario");
                        if (nombre != null && !nombre.trim().isEmpty()) {
                            userName = nombre.trim();

                            // (Opcional) Mensaje más personalizado al cargar nombre
                            adapter.add(new ChatMessage(
                                    ChatMessage.ROLE_ASSISTANT,
                                    "Perfecto, " + userName + ". Estoy listo para ayudarte con la seguridad y configuración de tu vehículo."
                            ));
                            scrollToBottom();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // no hacemos toast para no molestar, simplemente no habrá nombre
                });
    }

    // =========================
    // CONSTRUIR PROMPT DEL SISTEMA
    // =========================
    private String buildSystemPrompt() {
        String nombre = (userName == null || userName.isEmpty()) ? "Usuario" : userName;

        return "Eres un asistente virtual experto en seguridad de vehículos, especializado en el uso de esta aplicación de seguridad para coches.\n" +
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
                "- Mantén un tono cercano, profesional y humano.\n" +
                "- Sé claro, educado y tranquilizador, especialmente en situaciones de alerta o impacto.\n" +
                "- No inventes funcionalidades que no existen en la app.\n" +
                "- Si una función no está disponible, explícalo con claridad y ofrece alternativas dentro de la app.\n" +
                "\n" +
                "Tu misión es que el usuario se sienta seguro, informado y cómodo usando la aplicación en todo momento.\n" +
                "El nombre del usuario es: " + nombre + ".\n" +
                "Dirígete al usuario usando ese nombre.";
    }

    // =========================
    // ENVIAR MENSAJE
    // =========================
    private void sendMessage() {
        String text = etMessage.getText() != null
                ? etMessage.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(text)) return;

        etMessage.setText("");

        // Mensaje del usuario
        adapter.add(new ChatMessage(ChatMessage.ROLE_USER, text));
        scrollToBottom();

        setLoading(true);

        // Construir mensajes para OpenAI
        List<ChatMessage> messages = new ArrayList<>();

        // 1) Prompt del sistema SIEMPRE primero
        messages.add(new ChatMessage(ChatMessage.ROLE_SYSTEM, buildSystemPrompt()));

        // 2) Historial del chat
        messages.addAll(adapter.getItems());

        // Llamada a OpenAI
        openAI.send(messages, new OpenAIService.ResultCallback() {
            @Override
            public void onSuccess(String assistantText) {
                runOnUiThread(() -> {
                    setLoading(false);
                    adapter.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT, assistantText));
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ChatbotActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // =========================
    // UI HELPERS
    // =========================
    private void setLoading(boolean loading) {
        typingBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!loading);
        etMessage.setEnabled(!loading);
    }

    private void scrollToBottom() {
        int last = adapter.getItemCount() - 1;
        if (last < 0) return;
        recycler.post(() -> recycler.smoothScrollToPosition(last));
    }
}
