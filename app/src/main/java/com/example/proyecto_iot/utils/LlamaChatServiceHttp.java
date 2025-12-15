package com.example.proyecto_iot.utils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LlamaChatServiceHttp {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    private final String endpointBase; // ej: https://api.poligpt.upv.es
    private final String apiKey;       // tu clave
    private final String model;        // ej: vrain-llama3.3:70b

    private final List<Message> history = new ArrayList<>();

    public LlamaChatServiceHttp(String endpointBase, String apiKey, String model, String systemPrompt) {
        this.endpointBase = endpointBase.endsWith("/") ? endpointBase.substring(0, endpointBase.length() - 1) : endpointBase;
        this.apiKey = apiKey;
        this.model = model;

        // timeouts más generosos (la primera respuesta puede tardar)
        this.client = new OkHttpClient.Builder()
                .callTimeout(java.time.Duration.ofSeconds(60))
                .connectTimeout(java.time.Duration.ofSeconds(20))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .writeTimeout(java.time.Duration.ofSeconds(30))
                .build();

        history.add(new Message("system", systemPrompt));
    }

    /** Añade mensaje de usuario y devuelve respuesta del asistente */
    public String ask(String userText) throws IOException {
        history.add(new Message("user", userText));

        ChatRequest payload = new ChatRequest(model, history);

        String url = endpointBase + "/v1/chat/completions"; // si tu uni usa otra ruta, te digo abajo cómo cambiarlo
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + response.message() + " :: " + body);
            }

            String body = response.body() != null ? response.body().string() : "";
            ChatResponse parsed = gson.fromJson(body, ChatResponse.class);

            String assistant = (parsed != null
                    && parsed.choices != null
                    && !parsed.choices.isEmpty()
                    && parsed.choices.get(0).message != null)
                    ? parsed.choices.get(0).message.content
                    : "";

            if (assistant == null) assistant = "";
            history.add(new Message("assistant", assistant));
            return assistant.trim();
        }
    }

    // ====== DTOs ======

    static class ChatRequest {
        final String model;
        final List<Message> messages;
        ChatRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    static class Message {
        final String role;
        final String content;
        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    static class ChatResponse {
        List<Choice> choices;
    }

    static class Choice {
        Message message;
        @SerializedName("finish_reason")
        String finishReason;
    }
}
