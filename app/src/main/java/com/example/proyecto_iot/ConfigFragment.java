package com.example.proyecto_iot;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // <-- Importación necesaria

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
        // Asumo que tu layout XML se llama 'fragment_config.xml'
        return inflater.inflate(R.layout.fragment_config, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Tu código de CERRAR SESIÓN (Existente) ---
        View logout = view.findViewById(R.id.btn_logout);
        if (logout != null) {
            logout.setOnClickListener(v -> {
                try {
                    GoogleSignInClient gsc = GoogleSignIn.getClient(requireContext(),
                            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(getString(R.string.default_web_client_id))
                                    .requestEmail()
                                    .build());
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

        // --- CÓDIGO AÑADIDO PARA IR A 'COCHES REGISTRADOS' ---

        // 1. Encontrar el LinearLayout
        LinearLayout botonCoches = view.findViewById(R.id.coches_registrados);

        // 2. Asignar el listener
        if (botonCoches != null) {
            botonCoches.setOnClickListener(v -> {

                // 4. Preparamos el nuevo fragmento
                CochesRegistradosFragment cochesFragment = new CochesRegistradosFragment();
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();

                // 5. Reemplazamos el contenedor (usando el ID 'fragment_container' de tu MainActivity)
                transaction.replace(R.id.fragment_container, cochesFragment);

                // 6. Añadimos a la pila para poder "volver"
                transaction.addToBackStack(null);

                // 7. Ejecutamos
                transaction.commit();
            });
        }
    }
}