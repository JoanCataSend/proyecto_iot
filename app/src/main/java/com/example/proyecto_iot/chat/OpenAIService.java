package com.example.proyecto_iot.chat;

import androidx.annotation.NonNull;

import com.example.proyecto_iot.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenAIService {

    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();

    public interface ResultCallback {
        void onSuccess(String assistantText);
        void onError(String errorMessage);
    }

    /**
     * Envía TODO el historial (incluyendo el último mensaje del usuario) y devuelve la respuesta del asistente.
     * Formato basado en el endpoint /v1/responses. citeturn1view0turn4view0
     */
    public void send(@NonNull List<ChatMessage> fullHistory,
                     @NonNull ResultCallback cb) {

        String apiKey = BuildConfig.OPENAI_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            cb.onError("Falta la API key. Añade OPENAI_API_KEY en local.properties/gradle.properties.");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", BuildConfig.OPENAI_MODEL);

        // Instrucciones (system/developer)
        body.addProperty("instructions",
                "Eres un asistente útil dentro de una app IoT. Responde en español, claro y directo. " +
                "Si te falta info, pregunta. Si la respuesta puede ser técnica, añade pasos.");

        JsonArray input = new JsonArray();

        // Historial
        for (ChatMessage m : fullHistory) {
            JsonObject item = new JsonObject();
            item.addProperty("role", m.getRole());
            item.addProperty("content", m.getText());
            input.add(item);
        }

        body.add("input", input);
        body.addProperty("max_output_tokens", 500);
        body.addProperty("temperature", 0.7);

        Request req = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onError("Error de red: " + e.getMessage());
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String raw = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    cb.onError("HTTP " + response.code() + ": " + raw);
                    return;
                }

                try {
                    JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
                    String assistant = parseAssistantText(root);
                    if (assistant == null || assistant.trim().isEmpty()) {
                        cb.onError("Respuesta vacía del modelo.");
                    } else {
                        cb.onSuccess(assistant.trim());
                    }
                } catch (Exception ex) {
                    cb.onError("Error parseando JSON: " + ex.getMessage());
                }
            }
        });
    }

    private String parseAssistantText(JsonObject root) {
        if (!root.has("output") || !root.get("output").isJsonArray()) return null;
        JsonArray output = root.getAsJsonArray("output");

        StringBuilder sb = new StringBuilder();
        for (JsonElement el : output) {
            if (!el.isJsonObject()) continue;
            JsonObject item = el.getAsJsonObject();
            if (!item.has("type") || !"message".equals(item.get("type").getAsString())) continue;
            if (!item.has("role") || !"assistant".equals(item.get("role").getAsString())) continue;

            if (!item.has("content") || !item.get("content").isJsonArray()) continue;
            JsonArray content = item.getAsJsonArray("content");
            for (JsonElement cEl : content) {
                if (!cEl.isJsonObject()) continue;
                JsonObject c = cEl.getAsJsonObject();
                if (c.has("type") && "output_text".equals(c.get("type").getAsString()) && c.has("text")) {
                    sb.append(c.get("text").getAsString());
                }
            }
        }
        return sb.toString();
    }
}
