package com.example.proyecto_iot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.UUID;

public class EditarCocheFragment extends Fragment {

    private static final int PICK_IMAGE = 1001;

    private EditText edtMarca, edtModelo, edtMatricula, edtNombre;
    private Button btnGuardar, btnEliminar, btnCambiarFoto;

    private String cocheId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_editar_coche, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }

        // === FIND VIEWS ===
        edtMarca = view.findViewById(R.id.edtMarca);
        edtModelo = view.findViewById(R.id.edtModelo);
        edtMatricula = view.findViewById(R.id.edtMatricula);
        edtNombre = view.findViewById(R.id.edtNombre);

        btnGuardar = view.findViewById(R.id.btnGuardar);
        btnEliminar = view.findViewById(R.id.btnEliminar);
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto);

        if (getArguments() != null)
            cocheId = getArguments().getString("cocheId");

        cargarDatosCoche();

        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnEliminar.setOnClickListener(v -> mostrarDialogoEliminar());
        btnCambiarFoto.setOnClickListener(v -> abrirGaleria());

        return view;
    }

    // ============================================================
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            subirFoto(imageUri);
        }
    }

    private void subirFoto(Uri uri) {
        if (uri == null || cocheId == null) return;

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("fotos_coches/" + UUID.randomUUID());

        ref.putFile(uri)
                .addOnSuccessListener(task ->
                        task.getStorage().getDownloadUrl()
                                .addOnSuccessListener(downloadUrl -> {

                                    FirebaseFirestore.getInstance()
                                            .collection("Coches")
                                            .document(cocheId)
                                            .update("Foto", downloadUrl.toString())
                                            .addOnSuccessListener(a ->
                                                    CustomToast.success(requireActivity(),
                                                            "Foto actualizada"))
                                            .addOnFailureListener(e ->
                                                    CustomToast.error(requireActivity(),
                                                            "Error guardando URL"));
                                }))
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(), "Error subiendo foto"));
    }

    // ============================================================
    private void cargarDatosCoche() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("Coches").document(cocheId);

        ref.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                edtMarca.setText(doc.getString("Marca"));
                edtModelo.setText(doc.getString("Modelo"));
                edtMatricula.setText(doc.getString("Matrícula"));
                edtNombre.setText(doc.getString("Nombre"));
            }
        });
    }

    // ============================================================
    // VALIDACIÓN DE MATRÍCULA
    private boolean esMatriculaValida(String mat) {
        if (mat == null || mat.isEmpty()) return false;

        mat = mat.trim().toUpperCase();

        // Formato: 4 números + opcional espacio + 3 letras
        String patron = "^[0-9]{4}\\s?[A-Z]{3}$";

        return mat.matches(patron);
    }

    private void guardarCambios() {

        String marca = edtMarca.getText().toString().trim();
        String modelo = edtModelo.getText().toString().trim();
        String matricula = edtMatricula.getText().toString().trim().toUpperCase();
        String nombre = edtNombre.getText().toString().trim();

        // ---- VALIDACIONES ----
        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            CustomToast.warning(requireActivity(), "Rellena todos los campos");
            return;
        }

        if (!esMatriculaValida(matricula)) {
            CustomToast.error(requireActivity(), "Matrícula inválida (Formato: 1234 ABC)");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Coches").document(cocheId)
                .update(
                        "Marca", marca,
                        "Modelo", modelo,
                        "Matrícula", matricula,
                        "Nombre", nombre
                )
                .addOnSuccessListener(aVoid -> {
                    CustomToast.success(requireActivity(), "Cambios guardados");
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(), "Error: " + e.getMessage()));
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

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1️⃣ Quitar al usuario del array Propietario
            db.collection("Coches").document(cocheId)
                    .update("Propietario", FieldValue.arrayRemove(uid))
                    .addOnSuccessListener(aVoid -> {

                        // 2️⃣ Volver a leer el documento
                        db.collection("Coches").document(cocheId)
                                .get()
                                .addOnSuccessListener(doc -> {

                                    List<String> propietarios = (List<String>) doc.get("Propietario");

                                    // 3️⃣ Si el coche ya no tiene dueños → eliminarlo de Firestore
                                    if (propietarios == null || propietarios.isEmpty()) {
                                        db.collection("Coches").document(cocheId).delete();
                                    }

                                    CustomToast.success(requireActivity(),
                                            "Coche eliminado de tu cuenta");

                                    dialog.dismiss();
                                    requireActivity()
                                            .getSupportFragmentManager()
                                            .popBackStack();
                                });
                    })
                    .addOnFailureListener(e ->
                            CustomToast.error(requireActivity(),
                                    "Error al eliminar: " + e.getMessage()));
        });


        btnCancelar.setOnClickListener(v -> dialog.dismiss());
    }
}
