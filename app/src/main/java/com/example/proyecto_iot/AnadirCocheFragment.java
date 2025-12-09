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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AnadirCocheFragment extends Fragment {

    private EditText etMarca, etModelo, etMatricula, etNombre;
    private Button btnGuardar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.anadir_coche, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etMarca = view.findViewById(R.id.etMarcaEditar);
        etModelo = view.findViewById(R.id.etModeloEditar);
        etMatricula = view.findViewById(R.id.etMatriculaEditar);
        etNombre = view.findViewById(R.id.etNombreCocheEditar);

        btnGuardar = view.findViewById(R.id.btnGuardarCambios);

        btnGuardar.setOnClickListener(v -> guardarCoche());
    }

    private void guardarCoche() {

        String marca = etMarca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String matricula = etMatricula.getText().toString().trim().toUpperCase();
        String nombre = etNombre.getText().toString().trim();

        // ===== VALIDACIONES =====
        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            CustomToast.warning(requireActivity(), "Rellena todos los campos");
            return;
        }

        if (!esMatriculaValida(matricula)) {
            etMatricula.setError("Formato inválido. Ej: 1234 DKB");
            etMatricula.requestFocus();
            return;
        }

        matricula = matricula.replaceAll("\\s+", "");
        String finalMatricula = matricula;
        String uid = auth.getCurrentUser().getUid();

        // ===== COMPROBAR MATRÍCULA ÚNICA =====
        db.collection("Coches")
                .whereEqualTo("Matrícula", finalMatricula)
                .get()
                .addOnSuccessListener(query -> {

                    if (!query.isEmpty()) {
                        etMatricula.setError("Esta matrícula ya está registrada");
                        etMatricula.requestFocus();
                        CustomToast.error(requireActivity(), "La matrícula ya existe");
                        return;
                    }

                    // Guardar coche
                    Map<String, Object> coche = new HashMap<>();
                    coche.put("Marca", marca);
                    coche.put("Modelo", modelo);
                    coche.put("Matrícula", finalMatricula);
                    coche.put("Nombre", nombre);
                    coche.put("Propietario", java.util.Collections.singletonList(uid));

                    db.collection("Coches")
                            .add(coche)
                            .addOnSuccessListener(ref -> {
                                CustomToast.success(requireActivity(), "Coche añadido correctamente");
                                requireActivity().getSupportFragmentManager().popBackStack();
                            })
                            .addOnFailureListener(e ->
                                    CustomToast.error(requireActivity(), "Error al añadir un nuevo coche")
                            );
                })
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(), "Error comprobando matrícula")
                );
    }


    private boolean esMatriculaValida(String mat) {
        if (mat == null || mat.isEmpty()) return false;

        mat = mat.trim().toUpperCase(); // normalizar

        // Patrón → 4 dígitos + espacio opcional + 3 letras
        String patron = "^[0-9]{4}\\s?[A-Z]{3}$";

        return mat.matches(patron);
    }

}
