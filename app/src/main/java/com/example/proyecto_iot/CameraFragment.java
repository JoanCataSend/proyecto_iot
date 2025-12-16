package com.example.proyecto_iot;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CameraFragment extends Fragment {


    private static final String TAG = "CameraFragment";

    // ID DEL COCHE
    private final String cocheId = "ZkB10ikraHvc11ig0vD0";

    // STREAMS
    private static final String STREAM_URL_ESP32 = "http://172.20.10.5:81/stream";
    private static final String STREAM_URL_RPI   = "http://192.168.1.91:8080/?action=stream";

    // CAPTURA ESP32
    private static final String CAPTURE_URL_ESP32 = "http://172.20.10.5/capture";

    // UI
    private WebView webCamView, webCamView2;
    private TabLayout tabCamera;
    private LinearLayout layoutStreaming, layoutCapturas;
    private Button btnGuardarFoto;

    private RecyclerView recyclerCapturas;
    private TextView txtVacio;

    // LISTA
    private CapturasAdapter adapter;
    private final ArrayList<CapturaItem> listaCapturas = new ArrayList<>();

    // FIREBASE
    private FirebaseFirestore db;
    private StorageReference storageRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        // Firebase
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Views
        webCamView = view.findViewById(R.id.webCamView);
        webCamView2 = view.findViewById(R.id.webCamView2);

        tabCamera = view.findViewById(R.id.tabCamera);
        layoutStreaming = view.findViewById(R.id.layoutStreaming);
        layoutCapturas = view.findViewById(R.id.layoutCapturas);
        btnGuardarFoto = view.findViewById(R.id.btnGuardarFoto);

        // Recycler
        recyclerCapturas = view.findViewById(R.id.recyclerCapturas);
        recyclerCapturas.setLayoutManager(new LinearLayoutManager(getContext()));

        txtVacio = new TextView(getContext());
        txtVacio.setText("No hay capturas guardadas");
        txtVacio.setPadding(20, 40, 20, 40);
        layoutCapturas.addView(txtVacio);

        adapter = new CapturasAdapter(listaCapturas, getContext());
        recyclerCapturas.setAdapter(adapter);

        // LISTENER DE ACCIONES (DESCARGAR / ELIMINAR)

        adapter.setOnAccionesListener(new CapturasAdapter.OnAccionesListener() {
            @Override
            public void onDescargar(String url, String nombre) {
                descargarConDownloadManager(url, nombre);
            }

            @Override
            public void onEliminar(CapturaItem item) {
                // Confirmamos eliminación
                eliminarCaptura(item);
            }
        });

        // Webcams
        configurarWebCam(webCamView, STREAM_URL_ESP32);
        configurarWebCam(webCamView2, STREAM_URL_RPI);

        setupTabs();

        // BOTÓN CAPTURAR
        btnGuardarFoto.setOnClickListener(v -> {
            btnGuardarFoto.setEnabled(false);
            Toast.makeText(getContext(), "Capturando...", Toast.LENGTH_SHORT).show();
            capturarFotoDesdeESP32();
        });

        cargarCapturas();
    }

    // ---------------- TABS ----------------

    private void setupTabs() {
        if (tabCamera.getTabCount() == 0) {
            tabCamera.addTab(tabCamera.newTab().setText("Streaming"));
            tabCamera.addTab(tabCamera.newTab().setText("Capturas"));
        }

        tabCamera.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                layoutStreaming.setVisibility(tab.getPosition() == 0 ? View.VISIBLE : View.GONE);
                layoutCapturas.setVisibility(tab.getPosition() == 1 ? View.VISIBLE : View.GONE);
                if (tab.getPosition() == 1) cargarCapturas();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ---------------- WEBCAM ----------------

    private void configurarWebCam(WebView webView, String url) {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    // ---------------- CAPTURA ----------------

    private void capturarFotoDesdeESP32() {
        new Thread(() -> {
            try {
                URL url = new URL(CAPTURE_URL_ESP32);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("ESP32 no responde");
                }

                InputStream is = conn.getInputStream();
                File temp = File.createTempFile("captura_", ".jpg",
                        requireContext().getCacheDir());

                FileOutputStream fos = new FileOutputStream(temp);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                is.close();
                conn.disconnect();

                subirAFirebase(temp);

            } catch (Exception e) {
                Log.e(TAG, "Error capturando", e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(),
                            "Error capturando imagen",
                            Toast.LENGTH_SHORT).show();
                    btnGuardarFoto.setEnabled(true);
                });
            }
        }).start();
    }

    // ---------------- SUBIDA ----------------

    private void subirAFirebase(File archivo) {
        String nombre = "captura_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref =
                storageRef.child("capturas/" + cocheId + "/" + nombre);

        ref.putFile(Uri.fromFile(archivo))
                .addOnSuccessListener(t ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {

                            Map<String, Object> data = new HashMap<>();
                            data.put("url", uri.toString());
                            data.put("nombre", nombre);
                            data.put("fecha", System.currentTimeMillis());

                            db.collection("Coches")
                                    .document(cocheId)
                                    .collection("capturas")
                                    .add(data);

                            archivo.delete();

                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(),
                                        "Foto guardada",
                                        Toast.LENGTH_SHORT).show();
                                btnGuardarFoto.setEnabled(true);
                                tabCamera.getTabAt(1).select();
                            });
                        })
                )
                .addOnFailureListener(e ->
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(),
                                    "Error subiendo foto",
                                    Toast.LENGTH_SHORT).show();
                            btnGuardarFoto.setEnabled(true);
                        })
                );
    }

    // ---------------- LISTADO ----------------

    private void cargarCapturas() {
        db.collection("Coches")
                .document(cocheId)
                .collection("capturas")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    listaCapturas.clear();
                    qs.forEach(doc -> {
                        String url = doc.getString("url");
                        String nombre = doc.getString("nombre");
                        if (url != null && nombre != null) {
                            listaCapturas.add(new CapturaItem(url, nombre));
                        }
                    });

                    txtVacio.setVisibility(listaCapturas.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerCapturas.setVisibility(listaCapturas.isEmpty() ? View.GONE : View.VISIBLE);

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error cargando capturas", e));
    }

    // ---------------- ELIMINAR (NUEVO) ----------------
    // Borra de Storage y luego busca el documento por nombre en Firestore para borrarlo
    private void eliminarCaptura(CapturaItem item) {
        Toast.makeText(getContext(), "Eliminando...", Toast.LENGTH_SHORT).show();

        String nombreArchivo = item.getNombre();

        // 1. Borrar imagen física en Storage
        StorageReference refImagen = storageRef.child("capturas/" + cocheId + "/" + nombreArchivo);

        refImagen.delete().addOnSuccessListener(aVoid -> {
            // 2. Buscar en Firestore el documento que tiene ese nombre y borrarlo
            db.collection("Coches")
                    .document(cocheId)
                    .collection("capturas")
                    .whereEqualTo("nombre", nombreArchivo)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                            doc.getReference().delete();
                        }
                        Toast.makeText(getContext(), "Eliminado correctamente", Toast.LENGTH_SHORT).show();
                        cargarCapturas(); // Recargar lista
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error buscando doc para borrar", e)
                    );

        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error al borrar imagen", Toast.LENGTH_SHORT).show();
        });
    }

    // ---------------- DESCARGA ----------------

    public void descargarConDownloadManager(String url, String nombre) {
        try {
            DownloadManager dm =
                    (DownloadManager) requireContext()
                            .getSystemService(Context.DOWNLOAD_SERVICE);

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle(nombre);
            req.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            req.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_PICTURES,
                    "Proyecto_IoT/" + nombre
            );

            dm.enqueue(req);
            Toast.makeText(getContext(),
                    "Descarga iniciada",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Error al descargar",
                    Toast.LENGTH_SHORT).show();
        }
    }
}