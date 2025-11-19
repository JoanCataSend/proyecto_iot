package com.example.proyecto_iot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AyudaySoporteFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ayudaysoporte, container, false);

        // Mostrar la flecha atrás en el header principal
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }

        return view;
    }
}
