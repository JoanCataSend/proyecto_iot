package com.example.proyecto_iot;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.proyecto_iot.utils.CustomToast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class CuentaYPerfilFragment extends Fragment {

    private static final int PICK_IMAGE = 1000;
    private static final int PERMISO_GALERIA = 2000;

    private EditText etNombre, etCorreo;
    private ImageView fotoPerfil;
    private ImageButton btnEditarFoto;
    private Button btnCambiarContrasena, btnGuardar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    // ======================================================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.cuentayperfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            CustomToast.error(requireActivity(), "No hay usuario logueado");
            requireActivity().onBackPressed();
            return;
        }

        fotoPerfil = view.findViewById(R.id.fotoPerfil);
        btnEditarFoto = view.findViewById(R.id.btnEditarFoto);
        etNombre = view.findViewById(R.id.etModeloEditar);
        etCorreo = view.findViewById(R.id.etCorreo);
        btnCambiarContrasena = view.findViewById(R.id.btnCambiarContrasena);
        btnGuardar = view.findViewById(R.id.btnGuardar);

        etCorreo.setFocusable(false);
        etCorreo.setClickable(true);
        etCorreo.setLongClickable(false);
        etCorreo.setCursorVisible(false);
        etCorreo.setOnClickListener(v ->
                CustomToast.warning(requireActivity(), "El correo no se puede cambiar")
        );

        cargarDatosUsuario();

        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnEditarFoto.setOnClickListener(v -> abrirGaleriaConPermiso());
        btnCambiarContrasena.setOnClickListener(v -> enviarCorreoCambioContrasena());
    }

    // ======================================================
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE &&
                resultCode == Activity.RESULT_OK &&
                data != null &&
                data.getData() != null) {

            subirFotoFirebase(data.getData());
        }
    }

    // ======================================================
    // OBTENER EXTENSIÓN REAL DE LA FOTO
    // ======================================================
    private String getFileExtension(Uri uri) {
        String extension = null;

        try {
            String mime = requireContext().getContentResolver().getType(uri);
            if (mime != null) {
                extension = mime.substring(mime.lastIndexOf("/") + 1);
            }
        } catch (Exception ignored) {}

        if (extension == null) {
            String path = uri.getPath();
            if (path != null && path.contains(".")) {
                extension = path.substring(path.lastIndexOf(".") + 1);
            }
        }

        if (extension == null || extension.isEmpty()) {
            extension = "jpg";
        }

        return extension.toLowerCase();
    }

    // ======================================================
    // SUBIR FOTO SOLO A fotos_perfil/
    // ======================================================
    private void subirFotoFirebase(Uri imageUri) {

        if (imageUri == null || user == null) return;

        String extension = getFileExtension(imageUri);

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("fotos_perfil/" + user.getUid() + "." + extension);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl()
                            .addOnSuccessListener(uri -> {

                                // Cargar en la UI
                                Glide.with(CuentaYPerfilFragment.this)
                                        .load(uri)
                                        .into(fotoPerfil);

                                // Guardar URL en Firestore
                                db.collection("Usuarios")
                                        .document(user.getUid())
                                        .update("Imagen", uri.toString())
                                        .addOnSuccessListener(v ->
                                                CustomToast.success(requireActivity(), "Imagen actualizada")
                                        )
                                        .addOnFailureListener(e ->
                                                CustomToast.error(requireActivity(), "Error guardando URL")
                                        );
                            })
                            .addOnFailureListener(e ->
                                    CustomToast.error(requireActivity(), "Error obteniendo URL")
                            );
                })
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(), "Error subiendo imagen")
                );
    }

    // ======================================================
    // CARGAR DATOS USUARIO
    // ======================================================
    private void cargarDatosUsuario() {

        if (user == null) return;

        etCorreo.setText(user.getEmail());

        db.collection("Usuarios")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        String usuario = doc.getString("Usuario");
                        String correo = doc.getString("Correo");
                        String imagenUrl = doc.getString("Imagen");

                        etNombre.setText(usuario);
                        etCorreo.setText(correo);

                        if (imagenUrl != null && !imagenUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imagenUrl)
                                    .placeholder(R.drawable.ic_perfil2)
                                    .into(fotoPerfil);
                        }

                    } else {
                        etNombre.setText("Usuario desconocido");
                        CustomToast.warning(requireActivity(), "No se encontraron datos");
                    }
                })
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(), "Error al cargar datos")
                );
    }

    // ======================================================
    private void guardarCambios() {

        if (user == null) return;

        String nuevoNombre = etNombre.getText().toString().trim();

        Map<String, Object> datos = new HashMap<>();
        datos.put("Usuario", nuevoNombre);

        db.collection("Usuarios")
                .document(user.getUid())
                .update(datos)
                .addOnSuccessListener(v ->
                        CustomToast.success(requireActivity(), "Datos guardados")
                )
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(), "Error al guardar")
                );
    }

    // ======================================================
    private void abrirGaleriaConPermiso() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISO_GALERIA
                );
            } else {
                abrirGaleria();
            }

        } else {

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISO_GALERIA
                );
            } else {
                abrirGaleria();
            }
        }
    }

    // ======================================================
    private void enviarCorreoCambioContrasena() {

        String correo = etCorreo.getText().toString().trim();

        if (correo.isEmpty()) {
            CustomToast.warning(requireActivity(), "No hay correo disponible");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.sendPasswordResetEmail(correo)
                .addOnSuccessListener(unused ->
                        CustomToast.success(requireActivity(), "Correo enviado")
                )
                .addOnFailureListener(e ->
                        CustomToast.error(requireActivity(), "Error al enviar correo")
                );
    }
}
