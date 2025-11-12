package com.example.proyecto_iot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        // Obtenemos el coche para la posición de dibujado
        Coche coche = listaCoches.get(position);
        holder.nombreCoche.setText(coche.getNombre());

        holder.btnOpciones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(context, view);
                popup.inflate(R.menu.menu_eliminar);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.opcion_eliminar) {

                            // --- INICIO DE LA CORRECCIÓN ---

                            // 1. Obtenemos la posición ACTUAL en el momento del clic.
                            int currentPosition = holder.getAdapterPosition();

                            // 2. Comprobamos que la posición es válida
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                // 3. Obtenemos el coche correcto usando la posición actual
                                Coche cocheActual = listaCoches.get(currentPosition);
                                Toast.makeText(context, "Eliminando " + cocheActual.getNombre(), Toast.LENGTH_SHORT).show();

                                // 4. Usamos la posición actual y válida
                                eliminarCoche(currentPosition);
                                return true;
                            }
                            // --- FIN DE LA CORRECCIÓN ---
                        }
                        return false;
                    }
                });

                popup.show();
            }
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

            // --- MEJORA RECOMENDADA ---
            // Notifica al adapter que las posiciones de los items *debajo* del eliminado
            // han cambiado, para evitar errores de consistencia.
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