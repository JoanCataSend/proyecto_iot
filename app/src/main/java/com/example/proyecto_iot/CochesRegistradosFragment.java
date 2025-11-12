package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CochesRegistradosFragment extends Fragment {

    private RecyclerView recyclerView;
    private CocheAdapter adapter;
    private List<Coche> listaCoches;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.coches_registrados, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_cochesR);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 🧱 Datos de ejemplo
        listaCoches = new ArrayList<>();
        listaCoches.add(new Coche("El Champon"));
        listaCoches.add(new Coche("La Panterita"));
        listaCoches.add(new Coche("Speedy Azul"));
        listaCoches.add(new Coche("El Desfase"));

        adapter = new CocheAdapter(listaCoches);
        recyclerView.setAdapter(adapter);
    }

    // --- Clase interna del Adapter ---
    private class CocheAdapter extends RecyclerView.Adapter<CocheAdapter.CocheViewHolder> {

        private List<Coche> coches;

        public CocheAdapter(List<Coche> coches) {
            this.coches = coches;
        }

        @NonNull
        @Override
        public CocheViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_coche_registrado, parent, false);
            return new CocheViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull CocheViewHolder holder, int position) {
            Coche coche = coches.get(position);
            holder.nombreCoche.setText(coche.getNombre());

            holder.btnOpciones.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), v);
                popup.inflate(R.menu.menu_eliminar);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.opcion_eliminar) {

                            // --- INICIO DE LA CORRECCIÓN ---

                            // 1. Obtenemos la posición ACTUAL en el momento del clic.
                            int currentPosition = holder.getAdapterPosition();

                            // 2. Comprobamos que la posición es válida (no ha sido eliminada justo ahora)
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                Toast.makeText(requireContext(), "Eliminando " + coches.get(currentPosition).getNombre(), Toast.LENGTH_SHORT).show();
                                // 3. Usamos la posición actual y válida
                                eliminarCoche(currentPosition);
                                return true;
                            }
                            // --- FIN DE LA CORRECCIÓN ---
                        }
                        return false;
                    }
                });
                popup.show();
            });
        }

        @Override
        public int getItemCount() {
            return coches.size();
        }

        public void eliminarCoche(int position) {
            coches.remove(position);
            notifyItemRemoved(position);

            // --- MEJORA OPCIONAL PERO RECOMENDADA ---
            // Notifica al adapter que las posiciones de los items *debajo* del eliminado
            // han cambiado, para evitar errores de consistencia.
            notifyItemRangeChanged(position, coches.size() - position);
        }

        // --- ViewHolder ---
        public class CocheViewHolder extends RecyclerView.ViewHolder {
            TextView nombreCoche;
            ImageButton btnOpciones;

            public CocheViewHolder(@NonNull View itemView) {
                super(itemView);
                nombreCoche = itemView.findViewById(R.id.nombre_coche);
                btnOpciones = itemView.findViewById(R.id.btnOpciones);
            }
        }
    }

    // --- Modelo simple ---
    private static class Coche {
        private final String nombre;
        public Coche(String nombre) { this.nombre = nombre; }
        public String getNombre() { return nombre; }
    }
}