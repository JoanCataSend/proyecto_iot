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
import java.util.concurrent.atomic.AtomicInteger;

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";

    // ID DEL COCHE
    private final String cocheId = "ZkB10ikraHvc11ig0vD0";

    // STREAMS
    private static final String STREAM_URL_ESP32 = "http://172.20.10.3:81/stream";
    private static final String STREAM2_URL_ESP32 = "http://172.20.10.4:81/stream";

    // CAPTURA ESP32
    private static final String CAPTURE_URL_ESP32 = "http://172.20.10.3/capture";
    private static final String CAPTURE2_URL_ESP32 = "http://172.20.10.4/capture";

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

    // 🚨 AUTO CAPTURE
    private boolean autoCapture = false;
    private boolean autoCaptureExecuted = false;

    private View overlayCam1, overlayCam2;


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

        // 🚨 LEER FLAG AUTO_CAPTURE
        if (getArguments() != null) {
            autoCapture = getArguments().getBoolean("AUTO_CAPTURE", false);
        }

        // Firebase
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Views
        overlayCam1 = view.findViewById(R.id.overlayCam1);
        overlayCam2 = view.findViewById(R.id.overlayCam2);
        webCamView = view.findViewById(R.id.webCamView);
        webCamView2 = view.findViewById(R.id.webCamView2);


        // De inicio: mostramos overlays hasta que cargue algo
        overlayCam1.setVisibility(View.VISIBLE);
        overlayCam2.setVisibility(View.VISIBLE);


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

        adapter.setOnAccionesListener(new CapturasAdapter.OnAccionesListener() {
            @Override
            public void onDescargar(String url, String nombre) {
                descargarConDownloadManager(url, nombre);
            }

            @Override
            public void onEliminar(CapturaItem item) {
                eliminarCaptura(item);
            }
        });

        // STREAMING
        configurarWebCam(webCamView, STREAM_URL_ESP32, overlayCam1);
        configurarWebCam(webCamView2, STREAM2_URL_ESP32, overlayCam2);


        setupTabs();

        btnGuardarFoto.setOnClickListener(v -> {
            btnGuardarFoto.setEnabled(false);
            Toast.makeText(getContext(), "Capturando ambas cámaras...", Toast.LENGTH_SHORT).show();
            capturarAmbasCamaras();
        });

        cargarCapturas();

        // 🚨 AUTO CAPTURE REAL (1 sola vez)
        if (autoCapture && !autoCaptureExecuted) {
            autoCaptureExecuted = true;
            view.postDelayed(() -> {
                if (!isAdded()) return;
                btnGuardarFoto.setEnabled(false);
                Toast.makeText(getContext(),
                        "Impacto detectado · Captura automática (2 cámaras)",
                        Toast.LENGTH_SHORT).show();
                capturarAmbasCamaras();
            }, 1000); // ⏱️ tiempo seguro
        }

        overlayCam1.setOnClickListener(v -> {
            overlayCam1.setVisibility(View.VISIBLE);
            if (webCamView != null) webCamView.reload();
        });

        overlayCam2.setOnClickListener(v -> {
            overlayCam2.setVisibility(View.VISIBLE);
            if (webCamView2 != null) webCamView2.reload();
        });

    }

    // ================= STREAM CLEANUP =================

    @Override
    public void onPause() {
        super.onPause();
        detenerStreaming();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detenerStreaming();
    }

    private void detenerStreaming() {
        cerrarWebView(webCamView);
        cerrarWebView(webCamView2);
        webCamView = null;
        webCamView2 = null;
    }

    private void cerrarWebView(WebView webView) {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.clearCache(true);
            webView.onPause();
            webView.removeAllViews();
            webView.destroy();
        }
    }

    // ================= TABS =================

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

    // ================= WEBCAM =================

    private void configurarWebCam(WebView webView, String url, View overlay) {
        if (webView == null || overlay == null) return;
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                // Si llega a terminar de cargar, asumimos que hay contenido
                overlay.setVisibility(View.GONE);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                overlay.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view,
                                        android.webkit.WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                overlay.setVisibility(View.VISIBLE);
            }
        });

        webView.loadUrl(url);

        // Fallback: si en X segundos no se ha podido cargar bien, mostramos overlay
        webView.postDelayed(() -> {
            if (isAdded() && overlay.getVisibility() != View.GONE) {
                overlay.setVisibility(View.VISIBLE);
            }
        }, 2500);
    }


    // ================= CAPTURA (DOBLE) =================
    private void capturarAmbasCamaras() {
        AtomicInteger pendientes = new AtomicInteger(2);

        capturarFotoDesdeUrl(CAPTURE_URL_ESP32, "cam1", pendientes);
        capturarFotoDesdeUrl(CAPTURE2_URL_ESP32, "cam2", pendientes);
    }

    private void capturarFotoDesdeUrl(String captureUrl, String camTag, AtomicInteger pendientes) {
        new Thread(() -> {
            try {
                URL url = new URL(captureUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("ESP32 no responde: " + camTag);
                }

                InputStream is = conn.getInputStream();
                File temp = File.createTempFile("captura_" + camTag + "_", ".jpg",
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

                // Subir indicando qué cámara es
                subirAFirebase(temp, camTag, pendientes);

            } catch (Exception e) {
                Log.e(TAG, "Error capturando " + camTag, e);

                // Mensaje de error por cámara, pero NO bloquea la otra
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Error capturando " + camTag,
                                    Toast.LENGTH_SHORT).show()
                    );
                }

                finalizarCapturaDoble(pendientes);
            }
        }).start();
    }
    private void capturarFotoDesdeESP32() {
        AtomicInteger pendientes = new AtomicInteger(1);
        capturarFotoDesdeUrl(CAPTURE_URL_ESP32, "cam1", pendientes);
    }

    // ================= SUBIDA =================

    private void subirAFirebase(File archivo, String camTag, AtomicInteger pendientes) {

        String nombre = "captura_" + camTag + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref =
                storageRef.child("capturas/" + cocheId + "/" + nombre);

        ref.putFile(Uri.fromFile(archivo))
                .addOnSuccessListener(t ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {

                            Map<String, Object> data = new HashMap<>();
                            data.put("url", uri.toString());
                            data.put("nombre", nombre);
                            data.put("fecha", System.currentTimeMillis());
                            data.put("camara", camTag); // ✅ NUEVO

                            db.collection("Coches")
                                    .document(cocheId)
                                    .collection("capturas")
                                    .add(data);

                            archivo.delete();

                            // No re-habilitamos aún: esperamos a las 2
                            finalizarCapturaDoble(pendientes);
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error subiendo foto " + camTag, e);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(),
                                        "Error subiendo " + camTag,
                                        Toast.LENGTH_SHORT).show()
                        );
                    }

                    finalizarCapturaDoble(pendientes);
                });
    }

    private void subirAFirebase(File archivo) {
        AtomicInteger pendientes = new AtomicInteger(1);
        subirAFirebase(archivo, "cam1", pendientes);
    }

    private void finalizarCapturaDoble(AtomicInteger pendientes) {
        if (pendientes.decrementAndGet() == 0) {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(),
                        "Captura completada",
                        Toast.LENGTH_SHORT).show();
                btnGuardarFoto.setEnabled(true);
                cargarCapturas();
                if (tabCamera != null && tabCamera.getTabCount() > 1 && tabCamera.getTabAt(1) != null) {
                    tabCamera.getTabAt(1).select();
                }
            });
        }
    }

    // ================= LISTADO =================

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

    // ================= ELIMINAR =================

    private void eliminarCaptura(CapturaItem item) {
        Toast.makeText(getContext(), "Eliminando...", Toast.LENGTH_SHORT).show();

        String nombreArchivo = item.getNombre();
        StorageReference refImagen =
                storageRef.child("capturas/" + cocheId + "/" + nombreArchivo);

        refImagen.delete().addOnSuccessListener(aVoid -> {
            db.collection("Coches")
                    .document(cocheId)
                    .collection("capturas")
                    .whereEqualTo("nombre", nombreArchivo)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                            doc.getReference().delete();
                        }
                        Toast.makeText(getContext(),
                                "Eliminado correctamente",
                                Toast.LENGTH_SHORT).show();
                        cargarCapturas();
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error buscando doc para borrar", e)
                    );
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(),
                        "Error al borrar imagen",
                        Toast.LENGTH_SHORT).show()
        );
    }

    // ================= DESCARGA =================

    public void descargarConDownloadManager(String url, String nombre) {
        try {
            DownloadManager dm =
                    (DownloadManager) requireContext()
                            .getSystemService(Context.DOWNLOAD_SERVICE);

            DownloadManager.Request req =
                    new DownloadManager.Request(Uri.parse(url));
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
