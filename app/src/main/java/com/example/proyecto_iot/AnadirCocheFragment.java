package com.example.proyecto_iot;

import android.app.Activity;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.anadir_coche, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔹 OCULTAR HEADER DEL XML (Porque el Fragment usa el de MainActivity)
        View headerEnLayout = view.findViewById(R.id.header_incluido);
        if (headerEnLayout != null) {
            headerEnLayout.setVisibility(View.GONE);
        }

        // Activar flecha atrás global
        Activity act = getActivity();
        if (act instanceof MainActivity) {
            ((MainActivity) act).setBackButtonVisible(true);
        }

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

        if (marca.isEmpty() || modelo.isEmpty() || matricula.isEmpty() || nombre.isEmpty()) {
            CustomToast.warning(requireActivity(), "Rellena todos los campos");
            return;
        }

        matricula = matricula.replaceAll("\\s+", "");
        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> coche = new HashMap<>();
        coche.put("Marca", marca);
        coche.put("Modelo", modelo);
        coche.put("Matrícula", matricula);
        coche.put("Nombre", nombre);
        coche.put("Propietario", java.util.Collections.singletonList(uid));

        db.collection("Coches").add(coche).addOnSuccessListener(ref -> {
            CustomToast.success(requireActivity(), "Coche añadido");
            getParentFragmentManager().popBackStack();
        });
    }

    private boolean esMatriculaValida(String mat) {
        return mat.matches("^[0-9]{4}\\s?[A-Z]{3}$");
    }
}