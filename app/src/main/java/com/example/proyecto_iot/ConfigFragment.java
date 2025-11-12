package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class ConfigFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_config, container, false);

        // 🔹 Pantalla principal de Configuración: header sin flecha atrás
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(false);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // =========================
        // CERRAR SESIÓN (Google / Facebook / Firebase)
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

                try {
                    LoginManager.getInstance().logOut();
                } catch (Exception ignored) {}

                try {
                    FirebaseAuth.getInstance().signOut();
                } catch (Exception ignored) {}

                Intent i = new Intent(requireContext(), EntryActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                requireActivity().finish();
            });
        }

        // =========================
        // NAVEGACIÓN: Notificaciones
        // =========================
        LinearLayout layoutNotificaciones = view.findViewById(R.id.layout_notificaciones);
        if (layoutNotificaciones != null) {
            layoutNotificaciones.setOnClickListener(v -> {
                replaceFragment(new ConfigNotificacionesFragment());
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setBackButtonVisible(true);
                }
            });
        }

        // =========================
        // NAVEGACIÓN: Seguridad
        // =========================
        LinearLayout layoutSeguridad = view.findViewById(R.id.layoutSeguridad);
        if (layoutSeguridad != null) {
            layoutSeguridad.setOnClickListener(v -> {
                replaceFragment(new SeguridadFragment());
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setBackButtonVisible(true);
                }
            });
        }

        // =========================
        // NAVEGACIÓN: Coches registrados
        //   * Requiere que la fila tenga id @+id/coches_registrados en fragment_config.xml
        // =========================
        LinearLayout botonCoches = view.findViewById(R.id.coches_registrados);
        if (botonCoches != null) {
            botonCoches.setOnClickListener(v -> {
                replaceFragment(new CochesRegistradosFragment());
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setBackButtonVisible(true);
                }
            });
        }
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
    }



    // =========================
    // Helper: reemplazar fragment en el contenedor principal
    // =========================
    private void replaceFragment(@NonNull Fragment target) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, target);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
