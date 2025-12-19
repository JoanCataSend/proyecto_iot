package com.example.proyecto_iot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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

    // ===== TAGS ROOT =====
    private static final String TAG_HOME = "root_home";
    private static final String TAG_CAR = "root_car";
    private static final String TAG_NOTIFICATIONS = "root_notifications";
    private static final String TAG_SETTINGS = "root_settings";

    // ===== FRAGMENTS EN MEMORIA =====
    private Fragment homeFragment;
    private Fragment carFragment;
    private Fragment notificationsFragment;
    private Fragment settingsFragment;

    private Fragment activeFragment;

    // ✅ FIX: listener para saber cuándo cambia el backstack y actualizar UI
    private final FragmentManager.OnBackStackChangedListener backStackListener =
            () -> setBackButtonVisible(getSupportFragmentManager().getBackStackEntryCount() > 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== UI =====
        navHome = findViewById(R.id.nav_home);
        navCar = findViewById(R.id.nav_car);
        navNotifications = findViewById(R.id.nav_notifications);
        navSettings = findViewById(R.id.nav_settings);

        indicatorHome = findViewById(R.id.indicator_home);
        indicatorCar = findViewById(R.id.indicator_car);
        indicatorNotifications = findViewById(R.id.indicator_notifications);
        indicatorSettings = findViewById(R.id.indicator_settings);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> handleBack());

        attachIconTouchAnimation(navHome);
        attachIconTouchAnimation(navCar);
        attachIconTouchAnimation(navNotifications);
        attachIconTouchAnimation(navSettings);

        FragmentManager fm = getSupportFragmentManager();

        // ✅ FIX: escuchar cambios en backstack para mostrar/ocultar flecha correctamente
        fm.addOnBackStackChangedListener(backStackListener);

        // ===== RESTAURAR FRAGMENTS SI EXISTEN =====
        homeFragment = fm.findFragmentByTag(TAG_HOME);
        carFragment = fm.findFragmentByTag(TAG_CAR);
        notificationsFragment = fm.findFragmentByTag(TAG_NOTIFICATIONS);
        settingsFragment = fm.findFragmentByTag(TAG_SETTINGS);

        if (homeFragment == null) homeFragment = new IntentoFragment();
        if (carFragment == null) carFragment = new UbicacionFragment();
        if (notificationsFragment == null) notificationsFragment = new NotificacionesFragment();
        if (settingsFragment == null) settingsFragment = new ConfigFragment();

        if (savedInstanceState == null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.setReorderingAllowed(true);

            ft.add(R.id.fragment_container, homeFragment, TAG_HOME);
            ft.add(R.id.fragment_container, carFragment, TAG_CAR).hide(carFragment);
            ft.add(R.id.fragment_container, notificationsFragment, TAG_NOTIFICATIONS).hide(notificationsFragment);
            ft.add(R.id.fragment_container, settingsFragment, TAG_SETTINGS).hide(settingsFragment);
            ft.commit();

            activeFragment = homeFragment;
            updateNavbarSelection(R.id.nav_home);
            setBackButtonVisible(false);
        } else {
            // ✅ FIX: en restauración, intenta recuperar el activeFragment de forma segura
            activeFragment = fm.findFragmentByTag(TAG_HOME);
            if (activeFragment == null) activeFragment = homeFragment;

            // ✅ FIX: si ya hay backstack, mostrar flecha
            setBackButtonVisible(fm.getBackStackEntryCount() > 0);
        }

        // ===== NAVBAR =====
        navHome.setOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                // ✅ FIX: si estás en una pantalla secundaria, limpiamos backstack al cambiar de tab
                clearBackStack();
            }
            updateNavbarSelection(R.id.nav_home);
            switchFragment(homeFragment);
            setBackButtonVisible(false);
        });

        navCar.setOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                clearBackStack();
            }
            updateNavbarSelection(R.id.nav_car);
            switchFragment(carFragment);
            setBackButtonVisible(false);
        });

        navNotifications.setOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                clearBackStack();
            }
            updateNavbarSelection(R.id.nav_notifications);
            switchFragment(notificationsFragment);
            setBackButtonVisible(false);
        });

        navSettings.setOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                clearBackStack();
            }
            updateNavbarSelection(R.id.nav_settings);
            switchFragment(settingsFragment);
            setBackButtonVisible(false);
        });
    }

    // =========================
    //  CAMBIO DE FRAGMENT CACHE
    // =========================
    private void switchFragment(Fragment target) {
        if (activeFragment == target) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setReorderingAllowed(true);
        ft.hide(activeFragment);
        ft.show(target);
        ft.commit();

        activeFragment = target;
    }

    // =========================
    //  ANIMACIÓN ICONOS
    // =========================
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

    // =========================
    //  MÉTODO PARA ABRIR PANTALLAS "SECUNDARIAS"
    // =========================
    public void replaceFragment(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setReorderingAllowed(true);
        ft.replace(R.id.fragment_container, fragment);

        if (addToBackstack) {
            ft.addToBackStack(null);
            // ✅ FIX: mostrar flecha al entrar a secundaria
            setBackButtonVisible(true);
        }

        ft.commit();
    }

    // ✅ FIX: limpiar el backstack cuando cambias de tab
    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // ✅ FIX: re-montar tabs (porque replace pudo haber quitado el root visual)
        FragmentTransaction ft = fm.beginTransaction();
        ft.setReorderingAllowed(true);

        // Asegura que están añadidos
        if (fm.findFragmentByTag(TAG_HOME) == null) ft.add(R.id.fragment_container, homeFragment, TAG_HOME).hide(homeFragment);
        if (fm.findFragmentByTag(TAG_CAR) == null) ft.add(R.id.fragment_container, carFragment, TAG_CAR).hide(carFragment);
        if (fm.findFragmentByTag(TAG_NOTIFICATIONS) == null) ft.add(R.id.fragment_container, notificationsFragment, TAG_NOTIFICATIONS).hide(notificationsFragment);
        if (fm.findFragmentByTag(TAG_SETTINGS) == null) ft.add(R.id.fragment_container, settingsFragment, TAG_SETTINGS).hide(settingsFragment);

        // vuelve a dejar visible el activeFragment
        ft.show(activeFragment);
        ft.commit();

        setBackButtonVisible(false);
    }

    // =========================
    //  UI HELPERS
    // =========================
    private void updateNavbarSelection(int selectedNavId) {
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

    // =========================
    //  BACK CENTRALIZADO
    // =========================
    private void handleBack() {

        FragmentManager fm = getSupportFragmentManager();

        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            // ✅ FIX: el listener se encarga de mostrar/ocultar la flecha
            return;
        }

        if (activeFragment != homeFragment) {
            updateNavbarSelection(R.id.nav_home);
            switchFragment(homeFragment);
            setBackButtonVisible(false);
            return;
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ FIX: evitar leaks
        getSupportFragmentManager().removeOnBackStackChangedListener(backStackListener);
    }
}
