package com.example.proyecto_iot.utils;

import com.example.proyecto_iot.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LlamaChatServiceHttp {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient();

    private final String endpoint = BuildConfig.LLAMA_ENDPOINT;     // https://api.poligpt.upv.es
    private final String apiKey = BuildConfig.LLAMA_API_KEY;
    private final String deployment = BuildConfig.LLAMA_MODEL;      // ej: llama3.3:70b

    // si da 404, probamos otro api-version luego
    private final String apiVersion = "2024-02-15-preview";

    private final List<JSONObject> history = new ArrayList<>();

    public LlamaChatServiceHttp(String systemPrompt) {
        history.add(msg("system", systemPrompt));
    }

    public synchronized String ask(String userText) throws IOException {
        try {
            history.add(msg("user", userText));

            JSONObject body = new JSONObject();
            body.put("messages", new JSONArray(history));
            body.put("temperature", 0.4);

            String depEnc = URLEncoder.encode(deployment, StandardCharsets.UTF_8);
            String url = endpoint + "/openai/deployments/" + depEnc
                    + "/chat/completions?api-version=" + apiVersion;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("api-key", apiKey)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            try (Response response = http.newCall(request).execute()) {
                String raw = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + " -> " + raw);
                }

                JSONObject json = new JSONObject(raw);
                String assistant = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                history.add(msg("assistant", assistant));
                return assistant;
            }

        } catch (Exception e) {
            // Convertimos cualquier JSONException en IOException
            throw new IOException("JSON error: " + e.getMessage(), e);
        }
    }

    private static JSONObject msg(String role, String content) {
        try {
            JSONObject o = new JSONObject();
            o.put("role", role);
            o.put("content", content);
            return o;
        } catch (Exception e) {
            // Esto NO debería pasar nunca, pero Java obliga
            throw new RuntimeException("Error creando JSON message", e);
        }
    }
}
