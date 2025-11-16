package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditarCocheFragment extends Fragment {

    private EditText etMarca, etModelo, etMatricula, etNombre;
    private Button btnGuardarCambios, btnEliminarCoche;

    private String cocheId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.editar_coche, container, false);

        // Mostrar flecha atrás en el header
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }

        // ======================
        // 1. ENLAZAR VISTAS
        // ======================
        etMarca = view.findViewById(R.id.etMarca);
        etModelo = view.findViewById(R.id.etModeloE);
        etMatricula = view.findViewById(R.id.etMatriculaE);
        etNombre = view.findViewById(R.id.etNombreE);

        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios);
        btnEliminarCoche = view.findViewById(R.id.btnEliminarCoche);

        // ======================
        // 2. OBTENER ID DEL COCHE
        // ======================
        if (getArguments() != null) {
            cocheId = getArguments().getString("cocheId");
        }

        // ======================
        // 3. CARGAR DATOS DEL COCHE
        // ======================
        cargarDatosCoche();

        // ======================
        // 4. GUARDAR CAMBIOS
        // ======================
        btnGuardarCambios.setOnClickListener(v -> guardarCambios());

        // ======================
        // 5. ELIMINAR CON DIÁLOGO
        // ======================
        btnEliminarCoche.setOnClickListener(v -> mostrarDialogoEliminar());
        return view;
    }

    // ============================================================
    // CARGAR DATOS DESDE FIRESTORE
    // ============================================================
    private void cargarDatosCoche() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("Coches").document(cocheId);

        ref.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etMarca.setText(doc.getString("Marca"));
                etModelo.setText(doc.getString("Modelo"));
                etMatricula.setText(doc.getString("Matrícula"));
                etNombre.setText(doc.getString("Nombre"));
            }
        });
    }

    // ============================================================
    // GUARDAR CAMBIOS EN FIRESTORE
    // ============================================================
    private void guardarCambios() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Coches").document(cocheId)
                .update(
                        "Marca", etMarca.getText().toString(),
                        "Modelo", etModelo.getText().toString(),
                        "Matrícula", etMatricula.getText().toString(),
                        "Nombre", etNombre.getText().toString()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(),
                            "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ============================================================
    // DIÁLOGO PERSONALIZADO PARA ELIMINAR COCHE
    // ============================================================
    private void mostrarDialogoEliminar() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_eliminar_coche, null);

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.DialogStyle);

        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        Button btnEliminarDef = dialogView.findViewById(R.id.btnEliminarDef);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);

        btnEliminarDef.setOnClickListener(v -> {

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Coches").document(cocheId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {

                        Toast.makeText(requireContext(),
                                "Coche eliminado correctamente", Toast.LENGTH_SHORT).show();

                        dialog.dismiss();

                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();

                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Error al eliminar: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
    }
}
