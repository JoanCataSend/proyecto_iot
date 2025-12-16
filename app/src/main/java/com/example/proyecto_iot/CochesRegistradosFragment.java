package com.example.proyecto_iot;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iot.utils.CustomToast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CochesRegistradosFragment extends Fragment {

    private RecyclerView recyclerView;
    private CocheAdapter adapter;
    private List<Coche> listaCoches;

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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        listaCoches = new ArrayList<>();
        adapter = new CocheAdapter(listaCoches);
        recyclerView.setAdapter(adapter);

        cargarCoches();

        // =============================================
        // BOTÓN "AÑADIR NUEVO COCHE"
        // =============================================
        View btnAnadirCoche = view.findViewById(R.id.btnAnadirCoche);
        btnAnadirCoche.setOnClickListener(v -> {

            AnadirCocheFragment fragment = new AnadirCocheFragment();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    // ===============================================================
    // CARGAR COCHES DEL USUARIO
    // ===============================================================
    private void cargarCoches() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("Coches")
                .whereArrayContains("Propietario", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listaCoches.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        listaCoches.add(new Coche(
                                doc.getId(),
                                doc.getString("Nombre")
                        ));
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(),
                                "Error al cargar coches: " + e.getMessage()));
    }


    // ===============================================================
    // MOSTRAR DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
    // ===============================================================
    private void mostrarDialogoEliminar(int position, Coche coche) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_eliminar_coche, null);

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());

        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Botón cancelar
        dialogView.findViewById(R.id.btnCancelar).setOnClickListener(v -> dialog.dismiss());

        // Botón eliminar definitivo (ID correcto: btnEliminarDef)
        dialogView.findViewById(R.id.btnEliminarDef).setOnClickListener(v -> {
            eliminarCocheDeUsuario(coche.getId(), position, dialog);
        });

        dialog.show();
    }


    // ===============================================================
    // ELIMINAR EN FIRESTORE (solo después de confirmar)
    // ===============================================================
    private void eliminarCocheDeUsuario(String cocheId, int position, androidx.appcompat.app.AlertDialog dialog) {

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Coches")
                .document(cocheId)
                .update("Propietario", FieldValue.arrayRemove(uid))
                .addOnSuccessListener(aVoid -> {

                    // Ahora comprobamos si el coche se ha quedado sin propietarios
                    db.collection("Coches")
                            .document(cocheId)
                            .get()
                            .addOnSuccessListener(doc -> {

                                List<String> propietarios = (List<String>) doc.get("Propietario");

                                if (propietarios == null || propietarios.isEmpty()) {
                                    // NADIE MÁS TIENE EL COCHE → eliminar documento
                                    db.collection("Coches").document(cocheId).delete();
                                }
                            });

                    dialog.dismiss();
                    CustomToast.success(requireActivity(), "Coche eliminado de tu cuenta");

                    listaCoches.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, listaCoches.size());
                })
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(), "Error eliminando coche: " + e.getMessage())
                );
    }



    // ===============================================================
    // ADAPTER
    // ===============================================================
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

                popup.setOnMenuItemClickListener(item -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION) return false;

                    Coche cocheActual = coches.get(currentPosition);

                    // ================= OPCIÓN ELIMINAR =================
                    if (item.getItemId() == R.id.opcion_eliminar) {
                        mostrarDialogoEliminar(currentPosition, cocheActual);
                        return true;
                    }

                    // ================= OPCIÓN PERSONALIZAR =================
                    if (item.getItemId() == R.id.opcion_personalizar) {

                        EditarCocheFragment fragment = new EditarCocheFragment();

                        Bundle args = new Bundle();
                        args.putString("cocheId", cocheActual.getId());
                        fragment.setArguments(args);

                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();

                        return true;
                    }

                    return false;
                });

                popup.show();
            });
        }

        @Override
        public int getItemCount() {
            return coches.size();
        }

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


    // ===============================================================
    // MODELO DE DATOS
    // ===============================================================
    private static class Coche {
        private final String id;
        private final String nombre;

        public Coche(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public String getId() { return id; }
        public String getNombre() { return nombre; }
    }

}
