package com.sibiabi.watosked.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sibiabi.watosked.R;
import com.sibiabi.watosked.db.DatabaseHelper;
import com.sibiabi.watosked.model.MessageTemplate;
import com.sibiabi.watosked.model.ScheduledMessage;
import com.sibiabi.watosked.util.AlarmSchedulerHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final int REQUEST_CONTACT   = 101;
    private static final int REQUEST_CONTACTS_PERM = 102;

    private EditText        etRecipient, etMessage;
    private TextView        tvCharCount, tvScheduledTime;
    private MaterialButton  btnPickContact, btnPickDateTime, btnSchedule;
    private MaterialButtonToggleGroup toggleWaType;
    private RadioGroup      rgRepeat;
    private RadioButton     rbNone, rbDaily, rbWeekly;
    private ChipGroup       chipGroupDays;
    private View            rowCustomDays;
    private DatabaseHelper  db;
    private SharedPreferences prefs;
    private Calendar        selectedCal = Calendar.getInstance();
    private boolean         timeSelected = false;
    private String          currentWaType = ScheduledMessage.WA_WHATSAPP;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db    = new DatabaseHelper(requireContext());
        prefs = requireContext().getSharedPreferences("watosked_prefs", Context.MODE_PRIVATE);

        etRecipient     = view.findViewById(R.id.etRecipient);
        etMessage       = view.findViewById(R.id.etMessage);
        tvCharCount     = view.findViewById(R.id.tvCharCount);
        tvScheduledTime = view.findViewById(R.id.tvScheduledTime);
        btnPickContact  = view.findViewById(R.id.btnPickContact);
        btnPickDateTime = view.findViewById(R.id.btnPickDateTime);
        btnSchedule     = view.findViewById(R.id.btnSchedule);
        toggleWaType    = view.findViewById(R.id.toggleWaType);
        rgRepeat        = view.findViewById(R.id.rgRepeat);
        rbNone          = view.findViewById(R.id.rbNone);
        rbDaily         = view.findViewById(R.id.rbDaily);
        rbWeekly        = view.findViewById(R.id.rbWeekly);
        chipGroupDays   = view.findViewById(R.id.chipGroupDays);
        rowCustomDays   = view.findViewById(R.id.rowCustomDays);

        // Character count
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                tvCharCount.setText(s.length() + " chars");
            }
        });

        // WhatsApp type toggle
        toggleWaType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                currentWaType = (checkedId == R.id.btnWaBusiness)
                        ? ScheduledMessage.WA_BUSINESS : ScheduledMessage.WA_WHATSAPP;
            }
        });

        // Repeat radio — show custom days row if custom
        rgRepeat.setOnCheckedChangeListener((group, checkedId) -> {
            rowCustomDays.setVisibility(checkedId == R.id.rbCustom ? View.VISIBLE : View.GONE);
        });

        // Contact picker
        btnPickContact.setOnClickListener(v -> pickContact());

        // Template picker
        view.findViewById(R.id.btnPickTemplate).setOnClickListener(v -> showTemplatePicker());

        // Date/time picker
        btnPickDateTime.setOnClickListener(v -> showDateTimePicker());

        // Schedule
        btnSchedule.setOnClickListener(v -> scheduleMessage());
    }

    private void pickContact() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, REQUEST_CONTACT);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not open contacts", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT && resultCode == android.app.Activity.RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri == null) return;
            try (Cursor cursor = requireContext().getContentResolver().query(
                    contactUri,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                                 ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String number = cursor.getString(0);
                    String name   = cursor.getString(1);
                    etRecipient.setText(number);
                    etRecipient.setTag(name);
                    Toast.makeText(requireContext(), "Selected: " + name, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showTemplatePicker() {
        List<MessageTemplate> templates = db.getAllTemplates();
        if (templates.isEmpty()) {
            // Ask to save current message as template
            String msg = etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Save as Template?")
                        .setMessage("Save this message as a reusable template?")
                        .setPositiveButton("Save", (d, w) -> {
                            MessageTemplate t = new MessageTemplate("Template " + (db.getAllTemplates().size() + 1), msg);
                            db.insertTemplate(t);
                            Toast.makeText(requireContext(), "Template saved!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                Toast.makeText(requireContext(), "No templates yet. Type a message and save it as template.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String[] titles = new String[templates.size() + 1];
        titles[0] = "➕ Save current as template";
        for (int i = 0; i < templates.size(); i++) titles[i + 1] = templates.get(i).getTitle();

        new AlertDialog.Builder(requireContext())
                .setTitle("Message Templates")
                .setItems(titles, (d, which) -> {
                    if (which == 0) {
                        String msg = etMessage.getText().toString().trim();
                        if (!TextUtils.isEmpty(msg)) {
                            MessageTemplate t = new MessageTemplate("Template " + (db.getAllTemplates().size() + 1), msg);
                            db.insertTemplate(t);
                            Toast.makeText(requireContext(), "Template saved!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Type a message first", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        etMessage.setText(templates.get(which - 1).getBody());
                    }
                })
                .show();
    }

    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            selectedCal.set(Calendar.YEAR, y);
            selectedCal.set(Calendar.MONTH, m);
            selectedCal.set(Calendar.DAY_OF_MONTH, d);
            new TimePickerDialog(requireContext(), (tv, h, min) -> {
                selectedCal.set(Calendar.HOUR_OF_DAY, h);
                selectedCal.set(Calendar.MINUTE, min);
                selectedCal.set(Calendar.SECOND, 0);
                timeSelected = true;
                tvScheduledTime.setText("Scheduled: " + fmt.format(selectedCal.getTime()));
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void scheduleMessage() {
        String recipient = etRecipient.getText().toString().trim();
        String message   = etMessage.getText().toString().trim();
        String name      = etRecipient.getTag() instanceof String ? (String) etRecipient.getTag() : "";

        if (TextUtils.isEmpty(recipient)) { etRecipient.setError("Enter phone number"); return; }
        if (TextUtils.isEmpty(message))   { etMessage.setError("Enter message");        return; }
        if (!timeSelected || selectedCal.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "Please pick a future date & time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine repeat
        String repeatType = ScheduledMessage.REPEAT_NONE;
        String repeatDays = "";
        int checkedId = rgRepeat.getCheckedRadioButtonId();
        if (checkedId == R.id.rbDaily)  repeatType = ScheduledMessage.REPEAT_DAILY;
        else if (checkedId == R.id.rbWeekly) repeatType = ScheduledMessage.REPEAT_WEEKLY;
        else if (checkedId == R.id.rbCustom) {
            repeatType = ScheduledMessage.REPEAT_CUSTOM;
            repeatDays = getSelectedDays();
            if (repeatDays.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one day for custom repeat", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ScheduledMessage msg = new ScheduledMessage(recipient, message, selectedCal.getTimeInMillis(), ScheduledMessage.STATUS_PENDING);
        msg.setContactName(name);
        msg.setRepeatType(repeatType);
        msg.setRepeatDays(repeatDays);
        msg.setWhatsappType(currentWaType);

        long id = db.insertSchedule(msg);
        if (id != -1) {
            AlarmSchedulerHelper.scheduleAlarm(requireContext(), msg);
            Toast.makeText(requireContext(), "✅ Message scheduled successfully!", Toast.LENGTH_LONG).show();
            clearForm();
        } else {
            Toast.makeText(requireContext(), "Failed to save schedule", Toast.LENGTH_SHORT).show();
        }
    }

    private String getSelectedDays() {
        StringBuilder sb = new StringBuilder();
        int[] ids = {R.id.chipMon, R.id.chipTue, R.id.chipWed, R.id.chipThu, R.id.chipFri, R.id.chipSat, R.id.chipSun};
        String[] names = {"MON","TUE","WED","THU","FRI","SAT","SUN"};
        for (int i = 0; i < ids.length; i++) {
            Chip chip = chipGroupDays.findViewById(ids[i]);
            if (chip != null && chip.isChecked()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(names[i]);
            }
        }
        return sb.toString();
    }

    private void clearForm() {
        etRecipient.setText("");
        etRecipient.setTag(null);
        etMessage.setText("");
        tvScheduledTime.setText("Scheduled: Not set");
        timeSelected = false;
        selectedCal = Calendar.getInstance();
        rbNone.setChecked(true);
        rowCustomDays.setVisibility(View.GONE);
    }
}