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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class CuentaYPerfilFragment extends Fragment {
    private static final int PICK_IMAGE = 1000; // Código para abrir la galería
    private static final int PERMISO_GALERIA = 2000; // Para permiso

    // EditTexts e imagen del perfil
    private EditText etNombre, etCorreo;
    private ImageView fotoPerfil;
    private ImageButton btnEditarFoto;
    private Button btnCambiarContrasena, btnGuardar;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    // ==============================================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cuentayperfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        // Si no hay usuario logueado, salimos
        if (user == null) {
            Toast.makeText(getContext(), "No hay usuario logueado", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        // Inicializamos vistas
        fotoPerfil = view.findViewById(R.id.fotoPerfil);
        btnEditarFoto = view.findViewById(R.id.btnEditarFoto);
        etNombre = view.findViewById(R.id.etModeloEditar);
        etCorreo = view.findViewById(R.id.etCorreo);
        btnCambiarContrasena = view.findViewById(R.id.btnCambiarContrasena);
        btnGuardar = view.findViewById(R.id.btnGuardar);

        // Bloqueamos edición del correo
        etCorreo.setFocusable(false);
        etCorreo.setClickable(true);
        etCorreo.setLongClickable(false);
        etCorreo.setCursorVisible(false);
        etCorreo.setOnClickListener(v ->
                Toast.makeText(getContext(), "El correo no se puede cambiar", Toast.LENGTH_SHORT).show()
        );

        // Cargar datos del usuario
        cargarDatosUsuario();

        // Botón guardar cambios
        btnGuardar.setOnClickListener(v -> guardarCambios());

        // Botón editar foto
        btnEditarFoto.setOnClickListener(v -> abrirGaleriaConPermiso());

        // Botón cambiar contraseña
        btnCambiarContrasena.setOnClickListener(v -> enviarCorreoCambioContrasena());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISO_GALERIA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirGaleria();
            } else {
                Toast.makeText(getContext(), "Permiso denegado para acceder a la galería", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Abrir almacenamiento para elegir imagen
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
            if (imageUri != null && user != null) {
                subirFotoFirebase(imageUri);
            }
        }
    }

    private void subirFotoFirebase(Uri imageUri) {
        if (imageUri == null || user == null) return;

        String userId = user.getUid();
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child(user.getUid() + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Obtener la URL de descarga desde el metadata
                    taskSnapshot.getStorage().getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                Glide.with(this).load(uri).into(fotoPerfil);
                                Toast.makeText(getContext(), "Imagen actualizada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error obteniendo URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error subiendo imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }



    private void cargarDatosUsuario() {
        if (user == null) return;

        // El correo que viene de FirebaseAuth siempre lo podemos mostrar
        etCorreo.setText(user.getEmail());

        // Traemos datos desde Firestore
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

                        // Imagen de perfil
                        if (imagenUrl != null && !imagenUrl.isEmpty()) {
                            Glide.with(this)
                                .load(imagenUrl)
                                .placeholder(R.drawable.ic_perfil2)
                                .into(fotoPerfil);
                        }

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

        // Nuevo nombre escrito por el usuario
        String nuevoNombre = etNombre.getText().toString().trim();

        // Creamos un mapa para actualizar Firestore
        Map<String, Object> datos = new HashMap<>();
        datos.put("Usuario", nuevoNombre);

        // Actualizamos Firestore
        db.collection("Usuarios")
                .document(user.getUid())
                .update(datos)
                .addOnSuccessListener(v -> Toast.makeText(getContext(), "Datos guardados", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show());

    }

    private void abrirGaleriaConPermiso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, PERMISO_GALERIA);
            } else {
                abrirGaleria();
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISO_GALERIA);
            } else {
                abrirGaleria();
            }
        }
    }

    private void enviarCorreoCambioContrasena() {
        String correo = etCorreo.getText().toString().trim();

        if (correo.isEmpty()) {
            Toast.makeText(getContext(), "No hay correo disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.sendPasswordResetEmail(correo)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Correo enviado, revisa tu bandeja", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al enviar correo: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

}
