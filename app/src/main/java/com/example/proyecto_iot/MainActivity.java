package com.example.proyecto_iot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class MainActivity extends AppCompatActivity {

    private ImageButton navHome;
    private ImageButton navCar;
    private ImageButton navNotifications;
    private ImageButton navSettings;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            String open = getIntent() != null ? getIntent().getStringExtra("openFragment") : null;
            if ("intento".equals(open)) {
                replaceFragment(new IntentoFragment(), false);
            }
        }

        navHome = findViewById(R.id.nav_home);
        navCar = findViewById(R.id.nav_car);
        navNotifications = findViewById(R.id.nav_notifications);
        navSettings = findViewById(R.id.nav_settings);
        btnBack = findViewById(R.id.btnBack);
        navHome.setOnClickListener(v -> replaceFragment(new IntentoFragment(), true));
        navCar.setOnClickListener(v -> replaceFragment(new CarFragment(), true));
        navNotifications.setOnClickListener(v -> replaceFragment(new NotificationsFragment(), true));
        navSettings.setOnClickListener(v -> replaceFragment(new ConfigFragment(), true));
        btnBack.setOnClickListener(v -> handleBack());
    }

    private void replaceFragment(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        if (addToBackstack) ft.addToBackStack(null);
        ft.commit();
    }

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
