package com.example.proyecto_iot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {

    private ImageButton navHome, navCar, navNotifications, navSettings, btnBack;
    private View indicatorHome, indicatorCar, indicatorNotifications, indicatorSettings;

    private final AccelerateInterpolator inInterpolator = new AccelerateInterpolator();
    private final OvershootInterpolator outInterpolator = new OvershootInterpolator(2f);

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

        // Animación táctil en íconos
        attachIconTouchAnimation(navHome);
        attachIconTouchAnimation(navCar);
        attachIconTouchAnimation(navNotifications);
        attachIconTouchAnimation(navSettings);

        // Clicks del navbar → siempre sin backstack
        navHome.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_home);
            replaceFragment(new IntentoFragment(), false);
            setBackButtonVisible(false);
        });

        navCar.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_car);
            replaceFragment(new UbicacionFragment(), false);
            setBackButtonVisible(false);
        });

        navNotifications.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_notifications);
            replaceFragment(new NotificacionesFragment(), false);
            setBackButtonVisible(false);
        });

        navSettings.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_settings);
            replaceFragment(new ConfigFragment(), false);
            setBackButtonVisible(false);
        });

        // Cargar fragment inicial solo una vez
        if (savedInstanceState == null) {
            updateNavbarSelection(R.id.nav_home);
            replaceFragment(new IntentoFragment(), false);
            setBackButtonVisible(false);
        }
    }

    /** Animación sutil de presión y rebote en íconos */
    private void attachIconTouchAnimation(ImageButton btn) {
        btn.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    v.animate()
                            .scaleX(0.88f).scaleY(0.88f).alpha(0.9f)
                            .setDuration(10)
                            .setInterpolator(inInterpolator)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().cancel();
                    v.animate()
                            .scaleX(1f).scaleY(1f).alpha(1f)
                            .setDuration(10)
                            .setInterpolator(outInterpolator)
                            .start();
                    break;
            }
            return false;
        });
    }

    public void replaceFragment(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);

        if (addToBackstack) ft.addToBackStack(null);

        ft.commit();
    }

    private void updateNavbarSelection(int selectedNavId) {
        if (indicatorHome == null) return;

        indicatorHome.setVisibility(selectedNavId == R.id.nav_home ? View.VISIBLE : View.INVISIBLE);
        indicatorCar.setVisibility(selectedNavId == R.id.nav_car ? View.VISIBLE : View.INVISIBLE);
        indicatorNotifications.setVisibility(selectedNavId == R.id.nav_notifications ? View.VISIBLE : View.INVISIBLE);
        indicatorSettings.setVisibility(selectedNavId == R.id.nav_settings ? View.VISIBLE : View.INVISIBLE);
    }

    public void setBackButtonVisible(boolean visible) {
        if (btnBack != null) {
            btnBack.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** BOTÓN ATRÁS CENTRALIZADO */
    private void handleBack() {

        // 1️⃣ Si hay algo en el backstack (pantallas como ConfigNotificacionesFragment) → volver atrás
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();

            // Actualizar visibilidad del botón según nuevo fragment
            Fragment newCurrent = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            if (newCurrent instanceof IntentoFragment ||
                    newCurrent instanceof UbicacionFragment ||
                    newCurrent instanceof NotificacionesFragment ||
                    newCurrent instanceof ConfigFragment) {
                setBackButtonVisible(false);
            } else {
                setBackButtonVisible(true);
            }
            return;
        }

        // 2️⃣ Sin backstack: estamos en uno de los fragments "raíz" del navbar
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Si no estamos en Home → ir a Home
        if (!(current instanceof IntentoFragment)) {
            updateNavbarSelection(R.id.nav_home);
            replaceFragment(new IntentoFragment(), false);
            setBackButtonVisible(false);
            return;
        }

        // 3️⃣ Ya estamos en Home → cerrar app
        finish();
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }
}
