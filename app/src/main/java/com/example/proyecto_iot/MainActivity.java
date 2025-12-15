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

    // Tags para reutilizar fragments raíz y evitar recreación (evita “recarga” y crashes)
    private static final String TAG_HOME = "root_home";
    private static final String TAG_CAR = "root_car";
    private static final String TAG_NOTIFICATIONS = "root_notifications";
    private static final String TAG_SETTINGS = "root_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Clicks del navbar → NO recargar si ya estás en el mismo fragment (evita crash por listeners duplicados)
        navHome.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_home);
            showRootFragment(IntentoFragment.class, TAG_HOME, new IntentoFragment());
            setBackButtonVisible(false);
        });

        navCar.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_car);

            /*
             * ORIGINAL (comentado): hacía 2 replaces seguidos, provocando recargas dobles y riesgo de crash.
             * replaceFragment(new UbicacionFragment(), true);
             * replaceFragment(new UbicacionFragment(), false);
             */

            // Correcto: fragment raíz sin backstack y sin recrearlo si ya está visible
            showRootFragment(UbicacionFragment.class, TAG_CAR, new UbicacionFragment());
            setBackButtonVisible(false);
        });

        navNotifications.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_notifications);
            showRootFragment(NotificacionesFragment.class, TAG_NOTIFICATIONS, new NotificacionesFragment());
            setBackButtonVisible(false);
        });

        navSettings.setOnClickListener(v -> {
            updateNavbarSelection(R.id.nav_settings);
            showRootFragment(ConfigFragment.class, TAG_SETTINGS, new ConfigFragment());
            setBackButtonVisible(false);
        });

        if (savedInstanceState == null) {
            updateNavbarSelection(R.id.nav_home);
            showRootFragment(IntentoFragment.class, TAG_HOME, new IntentoFragment());
            setBackButtonVisible(false);
        }
    }

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

    /**
     * Mantengo tu método público para no romper nada existente.
     * (Otros fragments/pantallas pueden estar llamándolo).
     */
    public void replaceFragment(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        if (addToBackstack) ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * ✅ Clave del fix:
     * - Si el fragment actual YA ES del mismo tipo → no hacemos replace (no se recarga).
     * - Si existe una instancia ya creada con tag → la reutilizamos (mantiene estado).
     * - No se añade al backstack porque son “raíces” del navbar.
     */
    private void showRootFragment(Class<? extends Fragment> fragmentClass, String tag, Fragment newInstance) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current != null && fragmentClass.isInstance(current)) {
            // Ya estás en este fragment → NO recargar datos
            return;
        }

        Fragment existing = getSupportFragmentManager().findFragmentByTag(tag);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setReorderingAllowed(true); // optimiza transacciones y reduce efectos raros

        if (existing != null) {
            ft.replace(R.id.fragment_container, existing, tag);
        } else {
            ft.replace(R.id.fragment_container, newInstance, tag);
        }

        // Importante: raíz sin backstack
        ft.commit();
    }

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

    /** BOTÓN ATRÁS CENTRALIZADO */
    private void handleBack() {

        // 1️⃣ Si hay algo en el backstack → volver atrás
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();

            // Asegura que el Fragment actual sea el correcto tras el pop
            getSupportFragmentManager().executePendingTransactions();

            Fragment newCurrent = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

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

        // Si no estamos en Home → ir a Home (sin recrear si ya existe)
        if (!(current instanceof IntentoFragment)) {
            updateNavbarSelection(R.id.nav_home);
            showRootFragment(IntentoFragment.class, TAG_HOME, new IntentoFragment());
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
