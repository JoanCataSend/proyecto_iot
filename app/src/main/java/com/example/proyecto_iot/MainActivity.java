package com.example.proyecto_iot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {

    private ImageButton navHome, navCar, navNotifications, navSettings, btnBack;
    private View indicatorHome, indicatorCar, indicatorNotifications, indicatorSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Referencias del navbar
        navHome = findViewById(R.id.nav_home);
        navCar = findViewById(R.id.nav_car);
        navNotifications = findViewById(R.id.nav_notifications);
        navSettings = findViewById(R.id.nav_settings);

        // Indicadores
        indicatorHome = findViewById(R.id.indicator_home);
        indicatorCar = findViewById(R.id.indicator_car);
        indicatorNotifications = findViewById(R.id.indicator_notifications);
        indicatorSettings = findViewById(R.id.indicator_settings);

        // Header
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> handleBack());

        // Clicks del navbar
        navHome.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_home);
            replaceFragment(new IntentoFragment(), false);
            setBackButtonVisible(false);
        });

        navCar.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_car);
            replaceFragment(new CarFragment(), true);
            setBackButtonVisible(false);
        });

        navNotifications.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_notifications);
            replaceFragment(new NotificacionesFragment(), true);
            setBackButtonVisible(false);
        });

        navSettings.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_settings);
            replaceFragment(new ConfigFragment(), true);
            setBackButtonVisible(false);
        });

        // Cargar fragment inicial solo una vez
        if (savedInstanceState == null) {
            updateNavbarSelection(R.id.nav_home);
            replaceFragment(new IntentoFragment(), false);
            setBackButtonVisible(false);
        }
    }

    /** Reemplaza el fragment visible en el contenedor principal */
    private void replaceFragment(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        if (addToBackstack) ft.addToBackStack(null);
        ft.commit();
    }

    /** Actualiza los indicadores de la barra inferior según la pestaña seleccionada */
    private void updateNavbarSelection(int selectedNavId) {
        if (indicatorHome == null) return;

        indicatorHome.setVisibility(selectedNavId == R.id.nav_home ? View.VISIBLE : View.INVISIBLE);
        indicatorCar.setVisibility(selectedNavId == R.id.nav_car ? View.VISIBLE : View.INVISIBLE);
        indicatorNotifications.setVisibility(selectedNavId == R.id.nav_notifications ? View.VISIBLE : View.INVISIBLE);
        indicatorSettings.setVisibility(selectedNavId == R.id.nav_settings ? View.VISIBLE : View.INVISIBLE);
    }

    /** Muestra u oculta la flecha atrás del header (el header siempre visible) */
    public void setBackButtonVisible(boolean visible) {
        if (btnBack != null) {
            btnBack.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** Gestiona la acción del botón atrás */
    private void handleBack() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }
}
