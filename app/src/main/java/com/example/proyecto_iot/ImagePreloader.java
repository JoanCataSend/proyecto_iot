package com.example.proyecto_iot;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ImagePreloader {

    private static Context appContext;

    private static String userImage = null;
    private static final List<String> carImages = new ArrayList<>();

    // Iniciar
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    // ======= PRECARGAR TODO ===========================================
    public static void precargarImagenes() {

        if (appContext == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            precargarImagenUsuario();
            precargarImagenesCoches();
        });
    }

    // ======= PRECARGAR FOTO DE USUARIO =================================
    private static void precargarImagenUsuario() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    String foto = doc.getString("Imagen");
                    userImage = foto;

                    if (foto != null && !foto.isEmpty()) {
                        Glide.with(appContext)
                                .downloadOnly()
                                .load(foto)
                                .submit();
                    }
                });
    }

    // ======= PRECARGAR FOTOS DE TODOS LOS COCHES =======================
    private static void precargarImagenesCoches() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("Coches")
                .whereArrayContains("Propietario", uid)
                .get()
                .addOnSuccessListener(query -> {

                    carImages.clear();

                    for (var doc : query.getDocuments()) {

                        String foto = doc.getString("Foto");

                        if (foto != null && !foto.isEmpty()) {
                            carImages.add(foto);

                            // AGL usar todos los tamaños usados en la app
                            precargarGlide(foto, 300, 300);
                            precargarGlide(foto, 120, 120);
                            precargarGlide(foto, 90, 90);
                        }
                    }
                });
    }

    // ======= FUNCIÓN QUE PRECARGA UN BITMAP ===========================

    private static void precargarGlide(String url, int w, int h) {
        Glide.with(appContext)
                .asBitmap()
                .load(url)
                .submit(w, h);
    }

    // ======= ACCESOS RÁPIDOS PARA LA APP ==============================

    public static String getUserImage() {
        return userImage;
    }

    public static List<String> getCarImages() {
        return carImages;
    }
}
