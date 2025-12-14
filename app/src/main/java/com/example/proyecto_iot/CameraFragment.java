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

import java.io.BufferedInputStream;
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

    private final String cocheId = "ZkB10ikraHvc11ig0vD0";

    private WebView webCamView;
    private WebView webCamView2;

    private TabLayout tabCamera;
    private LinearLayout layoutStreaming;
    private LinearLayout layoutCapturas;
    private Button btnGuardarFoto;

    private RecyclerView recyclerCapturas;
    private CapturasAdapter adapter;
    private final ArrayList<CapturaItem> listaCapturas = new ArrayList<>();

    private FirebaseFirestore db;
    private StorageReference storageRef;

    private static final String STREAM_URL_ESP32 = "http://192.168.0.117:81/stream";
    private static final String STREAM_URL_RPI   = "http://192.168.1.91:8080/?action=stream";
    private static final String CAPTURE_URL_ESP32 = "http://192.168.0.117/capture";

    public CameraFragment() {}

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
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        webCamView = view.findViewById(R.id.webCamView);
        webCamView2 = view.findViewById(R.id.webCamView2);

        tabCamera = view.findViewById(R.id.tabCamera);
        layoutStreaming = view.findViewById(R.id.layoutStreaming);
        layoutCapturas = view.findViewById(R.id.layoutCapturas);
        btnGuardarFoto = view.findViewById(R.id.btnGuardarFoto);

        recyclerCapturas = view.findViewById(R.id.recyclerCapturas);
        recyclerCapturas.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CapturasAdapter(listaCapturas, getContext());
        recyclerCapturas.setAdapter(adapter);

        // 🔥 CONECTAR ADAPTER CON DESCARGA (antes faltaba)
        adapter.setOnDescargarListener((url, nombre) -> {
            descargarConDownloadManager(url, nombre);
        });

        configurarWebCam(webCamView, STREAM_URL_ESP32);
        configurarWebCam(webCamView2, STREAM_URL_RPI);

        setupTabs();

        btnGuardarFoto.setOnClickListener(v -> {
            btnGuardarFoto.setEnabled(false);
            mostrarToast("Capturando...");
            capturarFotoDesdeESP32();
        });

        cargarCapturas();
    }

    private void setupTabs() {
        if (tabCamera.getTabCount() == 0) {
            tabCamera.addTab(tabCamera.newTab().setText("Streaming"));
            tabCamera.addTab(tabCamera.newTab().setText("Capturas"));
        }

        tabCamera.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutStreaming.setVisibility(View.VISIBLE);
                    layoutCapturas.setVisibility(View.GONE);
                } else {
                    layoutStreaming.setVisibility(View.GONE);
                    layoutCapturas.setVisibility(View.VISIBLE);
                    cargarCapturas();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        layoutStreaming.setVisibility(View.VISIBLE);
        layoutCapturas.setVisibility(View.GONE);
    }

    private void configurarWebCam(WebView webView, String url) {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    private void capturarFotoDesdeESP32() {
        new Thread(() -> {
            try {
                URL url = new URL(CAPTURE_URL_ESP32);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(10000);
                connection.connect();

                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    runOnUiThreadSafe(() -> {
                        mostrarToast("No se pudo capturar (HTTP " + code + ")");
                        btnGuardarFoto.setEnabled(true);
                    });
                    return;
                }

                InputStream is = new BufferedInputStream(connection.getInputStream());

                File temp = File.createTempFile("capture_", ".jpg", requireContext().getCacheDir());
                FileOutputStream fos = new FileOutputStream(temp);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                fos.close();
                is.close();
                connection.disconnect();

                subirAFirebase(temp);

            } catch (Exception e) {
                runOnUiThreadSafe(() -> {
                    mostrarToast("Error capturando imagen");
                    btnGuardarFoto.setEnabled(true);
                });
            }
        }).start();
    }

    private void subirAFirebase(File archivoLocal) {
        try {
            String filename = "captura_" + System.currentTimeMillis() + ".jpg";
            StorageReference fileRef =
                    storageRef.child("capturas/" + cocheId + "/" + filename);

            Uri uri = Uri.fromFile(archivoLocal);
            fileRef.putFile(uri)
                    .addOnSuccessListener(t -> fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        guardarEnFirestore(downloadUri.toString(), filename);
                        archivoLocal.delete();
                        runOnUiThreadSafe(() -> {
                            mostrarToast("Foto subida");
                            btnGuardarFoto.setEnabled(true);
                            TabLayout.Tab tab = tabCamera.getTabAt(1);
                            if (tab != null) tab.select();
                        });
                    }))
                    .addOnFailureListener(e ->
                            runOnUiThreadSafe(() -> {
                                mostrarToast("Error subiendo foto");
                                btnGuardarFoto.setEnabled(true);
                            })
                    );

        } catch (Exception e) {
            runOnUiThreadSafe(() -> {
                mostrarToast("Error subiendo foto");
                btnGuardarFoto.setEnabled(true);
            });
        }
    }

    private void guardarEnFirestore(String url, String nombre) {
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("nombre", nombre);
        data.put("fecha", System.currentTimeMillis());

        db.collection("Coches")
                .document(cocheId)
                .collection("capturas")
                .document()
                .set(data)
                .addOnSuccessListener(a -> cargarCapturas())
                .addOnFailureListener(e -> Log.e(TAG, "Error guardando metadata", e));
    }

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
                        if (url != null) listaCapturas.add(new CapturaItem(url, nombre));
                    });
                    adapter.notifyDataSetChanged();
                });
    }

    public void descargarConDownloadManager(String url, String nombreArchivo) {
        try {
            DownloadManager dm = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            request.setTitle(nombreArchivo);
            request.setDescription("Descargando captura");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            File destFolder = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Proyecto_IoT");

            if (!destFolder.exists()) destFolder.mkdirs();

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_PICTURES,
                    "Proyecto_IoT/" + nombreArchivo
            );

            dm.enqueue(request);

            mostrarToast("Descarga iniciada");

        } catch (Exception e) {
            mostrarToast("Error iniciando descarga");
        }
    }

    private void mostrarToast(String msg) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private void runOnUiThreadSafe(Runnable r) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(r);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webCamView != null) webCamView.onPause();
        if (webCamView2 != null) webCamView2.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webCamView != null) webCamView.onResume();
        if (webCamView2 != null) webCamView2.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webCamView != null) webCamView.destroy();
        if (webCamView2 != null) webCamView2.destroy();
    }
}
