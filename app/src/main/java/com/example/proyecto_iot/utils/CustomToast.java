package com.example.proyecto_iot.utils;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proyecto_iot.R;

public class CustomToast {

    public static void success(Activity activity, String mensaje) {
        show(activity, mensaje, R.drawable.bg_toast_bien, R.drawable.ic_tick);
    }

    public static void error(Activity activity, String mensaje) {
        show(activity, mensaje, R.drawable.bg_toast_error, R.drawable.ic_warning);
    }

    public static void warning(Activity activity, String mensaje) {
        show(activity, mensaje, R.drawable.bg_toast_warning, R.drawable.ic_warning);
    }

    private static void show(Activity activity, String mensaje, int fondo, int icono) {

        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);

        LinearLayout container = layout.findViewById(R.id.toastContainer);
        ImageView img = layout.findViewById(R.id.imgToast);
        TextView txt = layout.findViewById(R.id.txtToast);

        txt.setText(mensaje);
        img.setImageResource(icono);
        container.setBackgroundResource(fondo);

        Toast toast = new Toast(activity.getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);

        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 160);

        layout.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.toast_enter));

        toast.show();
    }
}
