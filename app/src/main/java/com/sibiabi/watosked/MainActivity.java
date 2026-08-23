package com.sibiabi.watosked;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sibiabi.watosked.ui.HistoryFragment;
import com.sibiabi.watosked.ui.HomeFragment;
import com.sibiabi.watosked.ui.SchedulesFragment;
import com.sibiabi.watosked.ui.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottomNav);

        // Show HomeFragment on launch
        if (savedInstanceState == null) {
            showFragment(new HomeFragment());
        }

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showFragment(new HomeFragment());
            } else if (id == R.id.nav_schedules) {
                showFragment(new SchedulesFragment());
            } else if (id == R.id.nav_history) {
                showFragment(new HistoryFragment());
            } else if (id == R.id.nav_settings) {
                showFragment(new SettingsFragment());
            }
            return true;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}