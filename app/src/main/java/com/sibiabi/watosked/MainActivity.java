package com.sibiabi.watosked;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sibiabi.watosked.adapter.ScheduleAdapter;
import com.sibiabi.watosked.db.DatabaseHelper;
import com.sibiabi.watosked.model.ScheduledMessage;
import com.sibiabi.watosked.service.WhatsAppAccessibilityService;
import com.sibiabi.watosked.util.AlarmSchedulerHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText etRecipient, etMessage, etScreenPin;
    private TextView tvSelectedDateTime;
    private MaterialCardView cardPermissionWarning;
    private Button btnEnableAccessibility, btnPickDateTime, btnSchedule, btnSavePin;
    private RecyclerView rvSchedules;

    private DatabaseHelper dbHelper;
    private ScheduleAdapter adapter;
    private List<ScheduledMessage> scheduleList;
    private SharedPreferences prefs;

    private Calendar selectedCalendar;
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("watosked_prefs", Context.MODE_PRIVATE);
        selectedCalendar = Calendar.getInstance();

        initViews();
        loadSavedPin();
        setupRecyclerView();
        checkPermissions();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        refreshScheduleList();
    }

    private void initViews() {
        cardPermissionWarning = findViewById(R.id.cardPermissionWarning);
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility);
        etScreenPin = findViewById(R.id.etScreenPin);
        btnSavePin = findViewById(R.id.btnSavePin);
        etRecipient = findViewById(R.id.etRecipient);
        etMessage = findViewById(R.id.etMessage);
        btnPickDateTime = findViewById(R.id.btnPickDateTime);
        tvSelectedDateTime = findViewById(R.id.tvSelectedDateTime);
        btnSchedule = findViewById(R.id.btnSchedule);
        rvSchedules = findViewById(R.id.rvSchedules);
    }

    private void loadSavedPin() {
        String savedPin = prefs.getString("screen_pin", "");
        if (!TextUtils.isEmpty(savedPin)) {
            etScreenPin.setText(savedPin);
        }
    }

    private void setupRecyclerView() {
        scheduleList = dbHelper.getAllSchedules();
        adapter = new ScheduleAdapter(this, scheduleList);
        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        rvSchedules.setAdapter(adapter);
    }

    private void refreshScheduleList() {
        scheduleList.clear();
        scheduleList.addAll(dbHelper.getAllSchedules());
        adapter.notifyDataSetChanged();
    }

    private void checkPermissions() {
        boolean isAccessibilityEnabled = isAccessibilityServiceEnabled();
        cardPermissionWarning.setVisibility(isAccessibilityEnabled ? View.GONE : View.VISIBLE);

        // Check Battery Optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + WhatsAppAccessibilityService.class.getName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException ignored) {
        }

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (settingValue != null) {
                return settingValue.contains(service) || settingValue.contains(WhatsAppAccessibilityService.class.getSimpleName());
            }
        }
        return false;
    }

    private void setupListeners() {
        btnEnableAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnSavePin.setOnClickListener(v -> {
            String pin = etScreenPin.getText().toString().trim();
            prefs.edit().putString("screen_pin", pin).apply();
            Toast.makeText(this, TextUtils.isEmpty(pin) ? "PIN cleared" : "🔒 Screen Lock PIN saved successfully!", Toast.LENGTH_SHORT).show();
        });

        btnPickDateTime.setOnClickListener(v -> showDateTimePicker());

        btnSchedule.setOnClickListener(v -> handleScheduleSubmit());
    }

    private void showDateTimePicker() {
        final Calendar now = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            MainActivity.this,
                            (timeView, hourOfDay, minute) -> {
                                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedCalendar.set(Calendar.MINUTE, minute);
                                selectedCalendar.set(Calendar.SECOND, 0);

                                tvSelectedDateTime.setText("Scheduled for: " + displayFormat.format(selectedCalendar.getTime()));
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            false
                    );
                    timePickerDialog.show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void handleScheduleSubmit() {
        String recipient = etRecipient.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(recipient)) {
            etRecipient.setError("Please enter recipient phone number with country code");
            return;
        }

        if (TextUtils.isEmpty(message)) {
            etMessage.setError("Please enter message content");
            return;
        }

        long scheduleTimestamp = selectedCalendar.getTimeInMillis();
        if (scheduleTimestamp <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future date and time!", Toast.LENGTH_SHORT).show();
            return;
        }

        ScheduledMessage scheduledMessage = new ScheduledMessage(
                recipient,
                message,
                scheduleTimestamp,
                "PENDING"
        );

        long id = dbHelper.insertSchedule(scheduledMessage);
        if (id != -1) {
            AlarmSchedulerHelper.scheduleAlarm(this, scheduledMessage);
            Toast.makeText(this, "✅ Message scheduled successfully!", Toast.LENGTH_LONG).show();

            // Clear inputs
            etRecipient.setText("");
            etMessage.setText("");
            tvSelectedDateTime.setText("Scheduled for: Not set");

            refreshScheduleList();
        } else {
            Toast.makeText(this, "Failed to save schedule to database", Toast.LENGTH_SHORT).show();
        }
    }
}
