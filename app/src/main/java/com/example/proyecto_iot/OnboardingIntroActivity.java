package com.example.proyecto_iot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class OnboardingIntroActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private OnboardingPagerAdapter adapter;
    private MaterialButton btnPrimary;
    private androidx.appcompat.widget.AppCompatTextView tvSecondary;
    private List<OnboardingPage> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_intro);
        SharedPreferences prefs = getSharedPreferences("kova_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_seen", true).apply();


        viewPager = findViewById(R.id.viewPagerOnboarding);
        btnPrimary = findViewById(R.id.btnPrimary);
        tvSecondary = findViewById(R.id.tvSecondaryAction);
        TabLayout tabLayout = findViewById(R.id.tabDots);
        TextView tvSkip = findViewById(R.id.tvSkip);

        // Crear páginas
        pages = new ArrayList<>();
        pages.add(new OnboardingPage(
                "Convierte tu coche en un vehículo inteligente",
                "Convierte tu coche en un vehículo conectado. Instala Köva y controla todo desde tu móvil, estés donde estés.",
                R.drawable.primerpaso));
        pages.add(new OnboardingPage(
                "Protección activa las 24 horas",
                "Detecta golpes, vibraciones e intentos de acceso al instante. Köva te avisa siempre que algo importante ocurre.",
                R.drawable.segundopaso));
        pages.add(new OnboardingPage(
                "Control total desde tu móvil",
                "Abre, cierra, activa vigilancia o emite señales remotas. Tu coche responde a ti, incluso cuando estás lejos.",
                R.drawable.tercerpaso));
        pages.add(new OnboardingPage(
                "Siempre localizado y bajo tu control",
                "Consulta la ubicación en tiempo real, revisa rutas y gestiona varios vehículos en una sola app, de forma sencilla y segura.",
                R.drawable.cuartopaso));

        adapter = new OnboardingPagerAdapter(pages);
        viewPager.setAdapter(adapter);

        // Vincular con TabLayout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setCustomView(getLayoutInflater().inflate(R.layout.item_tab_dot, null));
        }).attach();

        updateUiForPosition(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateUiForPosition(position);
            }
        });

        btnPrimary.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pages.size() - 1) {
                // Pasa a la siguiente página del onboarding
                viewPager.setCurrentItem(current + 1);
            } else {
                // Última página → crear cuenta / registrarse
                goToRegister();
            }
        });

        // Ya tengo cuenta
        tvSecondary.setPaintFlags(tvSecondary.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvSecondary.setOnClickListener(v -> goToLogin());

        // Omitit
        tvSkip.setPaintFlags(tvSkip.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

    }

    private void updateUiForPosition(int position) {
        if (position == pages.size() - 1) {
            btnPrimary.setText("Comenzar el registro");
            tvSecondary.setText("Ya tengo cuenta");
        } else {
            btnPrimary.setText("Siguiente");
            tvSecondary.setText("Ya tengo cuenta");
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToRegister() {
        Intent intent = new Intent(this, RegisterWizardActivity.class);

        startActivity(intent);
        finish();
    }



    private void goToDemo() {
        markOnboardingSeen();
        startActivity(new Intent(this, HomeDemoActivity.class)); // cámbiala cuando exista
        finish();
    }

    private void markOnboardingSeen() {
        SharedPreferences prefs = getSharedPreferences("kova_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_seen", true).apply();
    }
}
