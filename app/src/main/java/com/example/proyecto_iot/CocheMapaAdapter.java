package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CocheMapaAdapter extends RecyclerView.Adapter<CocheMapaAdapter.CocheHolder> {

    private final ArrayList<UbicacionFragment.CocheMapa> coches;
    private final UbicacionFragment fragmentPadre;

    public CocheMapaAdapter(ArrayList<UbicacionFragment.CocheMapa> coches,
                            UbicacionFragment fragmentPadre) {
        this.coches = coches;
        this.fragmentPadre = fragmentPadre;
    }

    @NonNull
    @Override
    public CocheHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coche, parent, false);
        return new CocheHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CocheHolder holder, int position) {

        UbicacionFragment.CocheMapa coche = coches.get(position);

        holder.nombre.setText(coche.nombre);

        // Esto ya funciona porque coche sí tiene "direccion"
        holder.ubicacion.setText(coche.direccion != null ? coche.direccion : "");

        // ------------------- Abrir EDITAR COCHE -------------------
        holder.btnEditar.setOnClickListener(v -> {

            EditarCocheFragment fragment = new EditarCocheFragment();

            Bundle args = new Bundle();
            args.putString("cocheId", coche.id);
            fragment.setArguments(args);

            fragmentPadre.requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return coches.size();
    }

    // ------------------- HOLDER -------------------
    static class CocheHolder extends RecyclerView.ViewHolder {

        TextView nombre, ubicacion;
        ImageButton btnEditar;

        public CocheHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.nombre_coche);
            ubicacion = itemView.findViewById(R.id.ubicacion_coche);
            btnEditar = itemView.findViewById(R.id.btn_editar_coche);
        }
    }
}
