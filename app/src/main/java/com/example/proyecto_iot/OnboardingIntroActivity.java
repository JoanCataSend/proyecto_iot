package com.example.proyecto_iot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

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

        // Crear páginas
        pages = new ArrayList<>();
        pages.add(new OnboardingPage(
                "Convierte tu coche en un vehículo inteligente",
                "Controla, localiza y protege tu vehículo en tiempo real desde tu móvil.",
                R.drawable.ic_coche));
        pages.add(new OnboardingPage(
                "Protección 24/7",
                "Recibe alertas instantáneas ante golpes, vibraciones o intentos de acceso.",
                R.drawable.ic_escudo));
        pages.add(new OnboardingPage(
                "Control total",
                "Abre o cierra tu coche, activa el modo vigilancia y emite señales remotas.",
                R.drawable.ic_wifi));
        pages.add(new OnboardingPage(
                "Siempre conectado",
                "Consulta la ubicación de tu coche y gestiona varios vehículos desde KöVa.",
                R.drawable.ic_ubicacion));

        adapter = new OnboardingPagerAdapter(pages);
        viewPager.setAdapter(adapter);

        // Vincular con TabLayout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

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

// El texto secundario SIEMPRE lleva a login
        tvSecondary.setOnClickListener(v -> goToLogin());

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
