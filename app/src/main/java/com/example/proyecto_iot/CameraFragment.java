package com.example.proyecto_iot;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CameraFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBackButtonVisible(true);
        }
        TextView tvInfo = view.findViewById(R.id.tvCameraInfo);
        if (tvInfo != null) {
            tvInfo.setText("Vista de cámara: aquí se mostrará la imagen o transmisión en vivo.");
        }
    }
}
