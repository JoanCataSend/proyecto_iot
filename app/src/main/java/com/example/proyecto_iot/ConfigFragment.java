package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ConfigFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_config, container, false);

        // Pantalla principal de Configuración: header sin flecha
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // =========================
        // CERRAR SESIÓN
        // =========================
        View logout = view.findViewById(R.id.btn_logout);
        if (logout != null) {
            logout.setOnClickListener(v -> {
                try {
                    GoogleSignInClient gsc = GoogleSignIn.getClient(
                            requireContext(),
                            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(getString(R.string.default_web_client_id))
                                    .requestEmail()
                                    .build()
                    );
                    gsc.signOut();
                } catch (Exception ignored) {}

                try { LoginManager.getInstance().logOut(); } catch (Exception ignored) {}
                try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) {}

                Intent i = new Intent(requireContext(), EntryActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                requireActivity().finish();
            });
        }


        // =========================
        // NAVEGACIÓN: NOTIFICACIONES
        // =========================
        LinearLayout layoutNotificaciones = view.findViewById(R.id.layout_notificaciones);
        if (layoutNotificaciones != null) {
            layoutNotificaciones.setOnClickListener(v -> {
                replaceFragment(new ConfigNotificacionesFragment());
                ((MainActivity) requireActivity()).setBackButtonVisible(true);
            });
        }


        // =========================
        // NAVEGACIÓN: SEGURIDAD
        // =========================
        LinearLayout layoutSeguridad = view.findViewById(R.id.layoutSeguridad);
        if (layoutSeguridad != null) {
            layoutSeguridad.setOnClickListener(v -> {
                replaceFragment(new SeguridadFragment());
                ((MainActivity) requireActivity()).setBackButtonVisible(true);
            });
        }


        // =========================
        // NAVEGACIÓN: COCHES REGISTRADOS
        // =========================
        LinearLayout botonCoches = view.findViewById(R.id.coches_registrados);
        if (botonCoches != null) {
            botonCoches.setOnClickListener(v -> {
                replaceFragment(new CochesRegistradosFragment());
                ((MainActivity) requireActivity()).setBackButtonVisible(true);
            });
        }


        // =========================
        // NAVEGACIÓN: AYUDA Y SOPORTE
        // =========================
        LinearLayout layoutAyudaySoporte = view.findViewById(R.id.layoutAyudaySoporte);
        if (layoutAyudaySoporte != null) {
            layoutAyudaySoporte.setOnClickListener(v -> {
                FragmentTransaction transaction = requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.fragment_container, new AyudaySoporteFragment());
                transaction.addToBackStack(null);
                transaction.commit();

                ((MainActivity) requireActivity()).setBackButtonVisible(true);
            });
        }


        // =========================
        // CARGAR DATOS DEL USUARIO (Firestore)
        // =========================

        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvEmail = view.findViewById(R.id.tv_email);
        ImageView profileImage = view.findViewById(R.id.profile_image);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Usuarios").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {

                        String nombre = doc.getString("Usuario");
                        String correo = doc.getString("Correo");
                        String imagenUrl = doc.getString("Imagen");

                        tvName.setText(nombre);
                        tvEmail.setText(correo);

                        if (imagenUrl != null && !imagenUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imagenUrl)
                                    .placeholder(R.drawable.ic_perfil2)
                                    .into(profileImage);
                        }

                    } else {
                        tvName.setText("Usuario desconocido");
                    }
                })
                .addOnFailureListener(e -> tvName.setText("Error al cargar"));
    }


    // =========================
    // Helper: Reemplazar fragment
    // =========================
    private void replaceFragment(@NonNull Fragment target) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, target);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
