package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CuentaYPerfilFragment extends Fragment {

    private EditText etNombre, etCorreo, etContrasena;
    private ImageView fotoPerfil;
    private Button btnCerrarSesion, btnGuardar, btnEditarFoto;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cuentayperfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "No hay usuario logueado", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        fotoPerfil = view.findViewById(R.id.imageView2);
        // btnEditarFoto = view.findViewById(R.id.btnEditarFoto);

        etNombre = view.findViewById(R.id.etModeloEditar);
        etCorreo = view.findViewById(R.id.etCorreo);
        etContrasena = view.findViewById(R.id.etContrasena);

        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        btnGuardar = view.findViewById(R.id.btnGuardar);

        // --- BLOQUEAR EDITTEXT CORREO ---
        etCorreo.setFocusable(false);
        etCorreo.setClickable(true);
        etCorreo.setLongClickable(false);
        etCorreo.setCursorVisible(false);

        etCorreo.setOnClickListener(v ->
                Toast.makeText(getContext(), "El correo no se puede cambiar", Toast.LENGTH_SHORT).show()
        );

        cargarDatosUsuario();

        btnGuardar.setOnClickListener(v -> guardarCambios());

        btnCerrarSesion.setOnClickListener(v -> {
            auth.signOut();
            requireActivity().finish();
        });
    }

    private void cargarDatosUsuario() {
        if (user == null) return;

        // Correo FirebaseAuth por defecto
        etCorreo.setText(user.getEmail());

        // Traemos datos desde Firestore
        db.collection("Usuarios")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {

                        String usuario = doc.getString("Usuario");
                        String correo = doc.getString("Correo");
                        // String contrasena = doc.getString("Contraseña");
                        String imagenUrl = doc.getString("Imagen");

                        etNombre.setText(usuario);
                        etCorreo.setText(correo);
                        // etContrasena.setText(contrasena);

//                        if (imagenUrl != null && !imagenUrl.isEmpty()) {
//                            Glide.with(this)
//                                    .load(imagenUrl)
//                                    .placeholder(R.drawable.ic_perfil2)
//                                    .into(fotoPerfil);
//                        }

                    } else {
                        etNombre.setText("Usuario desconocido");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show()
                );
    }

    private void guardarCambios() {
        if (user == null) return;

        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevaContrasena = etContrasena.getText().toString().trim();

        Map<String, Object> datos = new HashMap<>();
        datos.put("Usuario", nuevoNombre);

        db.collection("Usuarios")
                .document(user.getUid())
                .update(datos)
                .addOnSuccessListener(v -> Toast.makeText(getContext(), "Datos guardados", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show());

        if (!nuevaContrasena.isEmpty()) {
            user.updatePassword(nuevaContrasena)
                    .addOnSuccessListener(unused -> Toast.makeText(getContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "No se pudo cambiar la contraseña", Toast.LENGTH_SHORT).show());
        }
    }
}
