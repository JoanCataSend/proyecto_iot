package com.example.proyecto_iot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.ViewHolder> {

    private List<Notificacion> listaNotificaciones;

    public NotificacionAdapter(List<Notificacion> listaNotificaciones) {
        this.listaNotificaciones = listaNotificaciones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout XML del ítem
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notificacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Asigna los datos del modelo a las vistas
        Notificacion notif = listaNotificaciones.get(position);
        holder.titulo.setText(notif.getTitulo());
        holder.mensaje.setText(notif.getMensaje());
        holder.fecha.setText(notif.getFecha());
        holder.icono.setImageResource(notif.getIcono());

        // ---- LÓGICA DEL SEPARADOR POR DÍA ----
        String fechaActual = extraerDia(notif.getFecha()); // ej: "12/10/25"

        if (position == 0) {
            // Primera notificación: siempre mostramos separador
            holder.headerDia.setVisibility(View.VISIBLE);
            holder.headerDia.setText(fechaActual);
        } else {
            String fechaAnterior = extraerDia(
                    listaNotificaciones.get(position - 1).getFecha()
            );

            if (!fechaActual.equals(fechaAnterior)) {
                // Cambia el día -> mostramos header
                holder.headerDia.setVisibility(View.VISIBLE);
                holder.headerDia.setText(fechaActual);
            } else {
                // Mismo día -> ocultamos header
                holder.headerDia.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return listaNotificaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, mensaje, fecha, headerDia;
        ImageView icono;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.titulo_notificacion);
            mensaje = itemView.findViewById(R.id.mensaje_notificacion);
            fecha = itemView.findViewById(R.id.fecha_notificacion);
            icono = itemView.findViewById(R.id.icono_notificacion);
            headerDia = itemView.findViewById(R.id.header_dia);
        }
    }

    /**
     * Extrae solo el día de la fecha.
     * Si el formato es "18:42  12/10/25", devolvemos "12/10/25".
     */
    private String extraerDia(String fechaCompleta) {
        if (fechaCompleta == null) return "";
        String[] partes = fechaCompleta.trim().split("\\s+");
        if (partes.length >= 2) {
            return partes[1]; // "12/10/25"
        } else {
            // Si por lo que sea no viene con hora, devolvemos todo
            return fechaCompleta.trim();
        }
    }
}

