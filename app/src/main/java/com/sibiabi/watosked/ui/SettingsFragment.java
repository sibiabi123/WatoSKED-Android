package com.sibiabi.watosked.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.sibiabi.watosked.R;
import com.sibiabi.watosked.service.WatoForegroundService;
import com.sibiabi.watosked.service.WhatsAppAccessibilityService;

public class SettingsFragment extends Fragment {

    private TextInputEditText etPin;
    private SwitchMaterial    switchFgService;
    private SharedPreferences prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("watosked_prefs", Context.MODE_PRIVATE);

        etPin          = view.findViewById(R.id.etPin);
        switchFgService = view.findViewById(R.id.switchFgService);

        // Load saved PIN
        etPin.setText(prefs.getString("screen_pin", ""));

        // Persistent background preference
        switchFgService.setChecked(prefs.getBoolean("fg_service_enabled", true));
        switchFgService.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("fg_service_enabled", checked).apply();
            Toast.makeText(requireContext(), checked ? "Background service preference enabled" : "Disabled", Toast.LENGTH_SHORT).show();
        });

        // Save PIN
        view.findViewById(R.id.btnSavePin).setOnClickListener(v -> {
            String pin = etPin.getText() != null ? etPin.getText().toString().trim() : "";
            prefs.edit().putString("screen_pin", pin).apply();
            Toast.makeText(requireContext(),
                    TextUtils.isEmpty(pin) ? "PIN cleared" : "PIN saved!", Toast.LENGTH_SHORT).show();
        });

        // Accessibility settings
        view.findViewById(R.id.btnAccessibility).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        // Battery optimization
        view.findViewById(R.id.btnBattery).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(requireContext().getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                } else {
                    Toast.makeText(requireContext(), "Battery optimization already disabled ✅", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update accessibility status in view
        View view = getView();
        if (view != null) {
            com.google.android.material.card.MaterialCardView cardStatus = view.findViewById(R.id.cardAccessStatus);
            android.widget.TextView tvStatus = view.findViewById(R.id.tvAccessStatus);
            boolean enabled = isAccessibilityEnabled();
            if (enabled) {
                cardStatus.setStrokeColor(0xFF4CAF50);
                tvStatus.setText("✅ Accessibility Service: ACTIVE");
            } else {
                cardStatus.setStrokeColor(0xFFFF5252);
                tvStatus.setText("⚠️ Accessibility Service: NOT ENABLED — Tap below to enable");
            }
        }
    }

    private boolean isAccessibilityEnabled() {
        int acc = 0;
        try {
            acc = Settings.Secure.getInt(requireContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException ignored) {}
        if (acc == 1) {
            String services = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return services != null && services.contains(WhatsAppAccessibilityService.class.getSimpleName());
        }
        return false;
    }
}