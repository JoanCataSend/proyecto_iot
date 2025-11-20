package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment {

    private WebView webCamView;
    private WebView webCamView2;

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

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }

        webCamView = view.findViewById(R.id.webCamView);
        webCamView2 = view.findViewById(R.id.webCamView2);
        //Camara ESP32
        configurarWebCam(webCamView, "http://172.20.10.4:81/stream");
        //Camara Raspberry Pi
        configurarWebCam(webCamView2, "http://192.168.1.91:8080/?action=stream");
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
