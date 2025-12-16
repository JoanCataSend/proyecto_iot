package com.example.proyecto_iot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CapturasAdapter extends RecyclerView.Adapter<CapturasAdapter.ViewHolder> {

    private final List<CapturaItem> lista;
    private final Context context;
    private final Executor executor = Executors.newFixedThreadPool(5);

    // Interfaz para comunicar acciones al Fragment
    public interface OnAccionesListener {
        void onDescargar(String url, String nombre);
        void onEliminar(CapturaItem item);
    }

    private OnAccionesListener listener;

    public void setOnAccionesListener(OnAccionesListener listener) {
        this.listener = listener;
    }

    public CapturasAdapter(List<CapturaItem> lista, Context context) {
        this.lista = lista;
        this.context = context;
    }

    @NonNull
    @Override
    public CapturasAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_captura, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CapturasAdapter.ViewHolder holder, int position) {
        CapturaItem item = lista.get(position);

        holder.imagen.setImageDrawable(null);

        // Cargar imagen en segundo plano
        executor.execute(() -> {
            try {
                URL url = new URL(item.getUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(10000);
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                conn.disconnect();

                if (bmp != null) {
                    holder.imagen.post(() -> holder.imagen.setImageBitmap(bmp));
                }
            } catch (Exception ignored) {
            }
        });

        // Click en DESCARGAR
        holder.btnDownloadIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDescargar(item.getUrl(), item.getNombre());
            }
        });

        // Click en ELIMINAR
        holder.btnDeleteIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEliminar(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagen;
        ImageView btnDownloadIcon;
        ImageView btnDeleteIcon;

        public ViewHolder(@NonNull View v) {
            super(v);
            imagen = v.findViewById(R.id.imgMiniatura);
            btnDownloadIcon = v.findViewById(R.id.btnDownloadIcon);
            btnDeleteIcon = v.findViewById(R.id.btnDeleteIcon);
        }
    }
}