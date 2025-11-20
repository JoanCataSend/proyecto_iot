package com.example.proyecto_iot;

import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CochesRegistradosFragment extends Fragment {

    private RecyclerView recyclerView;
    private CocheAdapter adapter;
    private List<Coche> listaCoches;

    // BBDD
    private FirebaseAuth auth;
    private FirebaseFirestore db;


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

        // Inicializar bbdd
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🧱 Datos de ejemplo
        listaCoches = new ArrayList<>();
        adapter = new CocheAdapter(listaCoches);
        recyclerView.setAdapter(adapter);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            db.collection("Coches")
                    .whereArrayContains("Propietario", uid)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        listaCoches.clear(); // ya seguro que existe
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String id = doc.getId();
                            String nombre = doc.getString("Nombre");
                            listaCoches.add(new Coche(id, nombre));
                        }
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(),
                                "Error al cargar coches: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
        adapter = new CocheAdapter(listaCoches);
        recyclerView.setAdapter(adapter);

        // Boton añadir coche
        View btnAnadirCoche = view.findViewById(R.id.btnAnadirCoche);
        btnAnadirCoche.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AnadirCoche.class);
            startActivity(intent);
        });


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
                            int currentPosition = holder.getAdapterPosition();
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                Toast.makeText(requireContext(), "Eliminando " + coches.get(currentPosition).getNombre(), Toast.LENGTH_SHORT).show();
                                eliminarCoche(currentPosition);
                                return true;
                            }
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
            Coche coche = coches.get(position);

            db.collection("Coches")
                    .document(coche.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Coche eliminado", Toast.LENGTH_SHORT).show();
                        coches.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, coches.size() - position);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
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
        private final String id; // ID del documento
        private final String nombre;

        public Coche(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public String getId() { return id; }
        public String getNombre() { return nombre; }
    }

}