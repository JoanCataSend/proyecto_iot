package com.example.proyecto_iot.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImpactCaptureHelper {

    private static final String TAG = "ImpactCapture";

    private static final String CAPTURE_URL_ESP32 =
            "http://172.20.10.5/capture";

    private static long lastCaptureTime = 0;
    private static final long COOLDOWN_MS = 10_000; // 10s

    public static void capture(Context context, String cocheId) {

        long now = System.currentTimeMillis();
        if (now - lastCaptureTime < COOLDOWN_MS) {
            Log.d(TAG, "Captura ignorada (cooldown)");
            return;
        }
        lastCaptureTime = now;

        new Thread(() -> {
            try {
                Log.d(TAG, "Iniciando captura por impacto");

                URL url = new URL(CAPTURE_URL_ESP32);
                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "ESP32 no responde");
                    return;
                }

                InputStream is = conn.getInputStream();

                File temp = File.createTempFile(
                        "impact_",
                        ".jpg",
                        context.getCacheDir()
                );

                FileOutputStream fos = new FileOutputStream(temp);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                is.close();
                conn.disconnect();

                subirAFirebase(temp, cocheId);

            } catch (Exception e) {
                Log.e(TAG, "Error en captura de impacto", e);
            }
        }).start();
    }

    private static void subirAFirebase(File archivo, String cocheId) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        StorageReference storageRef =
                FirebaseStorage.getInstance().getReference();

        String nombre = "impacto_" + System.currentTimeMillis() + ".jpg";

        StorageReference ref =
                storageRef.child("capturas/ZkB10ikraHvc11ig0vD0/" + nombre);

        ref.putFile(Uri.fromFile(archivo))
                .addOnSuccessListener(t ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {

                            Map<String, Object> data = new HashMap<>();
                            data.put("url", uri.toString());
                            data.put("nombre", nombre);
                            data.put("fecha", System.currentTimeMillis());


                            db.collection("Coches")
                                    .document("ZkB10ikraHvc11ig0vD0")
                                    .collection("capturas")
                                    .add(data);

                            archivo.delete();
                            Log.d(TAG, "Captura de impacto subida");
                        })
                )
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error subiendo impacto", e)
                );
    }
}
