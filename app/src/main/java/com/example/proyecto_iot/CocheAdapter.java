package com.example.proyecto_iot;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iot.utils.CustomToast;

import java.util.List;

public class CocheAdapter extends RecyclerView.Adapter<CocheAdapter.CocheViewHolder> {

    private Context context;
    private List<Coche> listaCoches;

    public CocheAdapter(Context context, List<Coche> listaCoches) {
        this.context = context;
        this.listaCoches = listaCoches;
    }

    @NonNull
    @Override
    public CocheViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_coche_registrado, parent, false);
        return new CocheViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CocheViewHolder holder, int position) {

        Coche coche = listaCoches.get(position);
        holder.nombreCoche.setText(coche.getNombre());

        holder.btnOpciones.setOnClickListener(view -> {

            PopupMenu popup = new PopupMenu(context, view);
            popup.inflate(R.menu.menu_eliminar);

            popup.setOnMenuItemClickListener(item -> {

                if (item.getItemId() == R.id.opcion_eliminar) {

                    int currentPosition = holder.getAdapterPosition();

                    if (currentPosition != RecyclerView.NO_POSITION) {

                        Coche cocheActual = listaCoches.get(currentPosition);

                        CustomToast.warning((Activity) context,"Eliminando " + cocheActual.getNombre());

                        eliminarCoche(currentPosition);
                        return true;
                    }
                }
                return false;
            });

            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return listaCoches.size();
    }

    public void eliminarCoche(int position) {
        if (position >= 0 && position < listaCoches.size()) {

            listaCoches.remove(position);
            notifyItemRemoved(position);

            notifyItemRangeChanged(position, listaCoches.size() - position);
        }
    }

    public static class CocheViewHolder extends RecyclerView.ViewHolder {
        TextView nombreCoche;
        ImageButton btnOpciones;

        public CocheViewHolder(@NonNull View itemView) {
            super(itemView);
            nombreCoche = itemView.findViewById(R.id.nombre_coche);
            btnOpciones = itemView.findViewById(R.id.btnOpciones);
        }
    }
}
