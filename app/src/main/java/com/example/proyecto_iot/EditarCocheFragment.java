package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iot.utils.CustomToast;
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

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }

        etMarca = view.findViewById(R.id.etMarca);
        etModelo = view.findViewById(R.id.etModeloE);
        etMatricula = view.findViewById(R.id.etMatriculaE);
        etNombre = view.findViewById(R.id.etNombreE);

        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios);
        btnEliminarCoche = view.findViewById(R.id.btnEliminarCoche);

        if (getArguments() != null) {
            cocheId = getArguments().getString("cocheId");
        }

        cargarDatosCoche();

        btnGuardarCambios.setOnClickListener(v -> guardarCambios());

        btnEliminarCoche.setOnClickListener(v -> mostrarDialogoEliminar());

        return view;
    }

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
                    CustomToast.success(requireActivity(),
                            "Cambios guardados correctamente");

                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(),
                                "Error: " + e.getMessage()));
    }

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

                        CustomToast.success(requireActivity(),
                                "Coche eliminado correctamente");

                        dialog.dismiss();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e ->
                            CustomToast.error(requireActivity(),
                                    "Error al eliminar: " + e.getMessage()));
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
    }
}
