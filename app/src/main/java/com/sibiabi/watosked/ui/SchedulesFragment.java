package com.sibiabi.watosked.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sibiabi.watosked.R;
import com.sibiabi.watosked.adapter.ScheduleAdapter;
import com.sibiabi.watosked.db.DatabaseHelper;
import com.sibiabi.watosked.model.ScheduledMessage;
import com.sibiabi.watosked.util.AlarmSchedulerHelper;

import java.util.List;

public class SchedulesFragment extends Fragment implements ScheduleAdapter.OnDeleteClick {

    private RecyclerView  rv;
    private TextView      tvEmpty;
    private ScheduleAdapter adapter;
    private DatabaseHelper db;
    private List<ScheduledMessage> list;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_schedules, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db      = new DatabaseHelper(requireContext());
        rv      = view.findViewById(R.id.rvSchedules);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        load();
    }

    @Override public void onResume() { super.onResume(); load(); }

    private void load() {
        list    = db.getPendingSchedules();
        adapter = new ScheduleAdapter(list, this);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDelete(ScheduledMessage msg) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Schedule")
                .setMessage("Cancel scheduled message to " + msg.getDisplayName() + "?")
                .setPositiveButton("Yes, Cancel", (d, w) -> {
                    AlarmSchedulerHelper.cancelAlarm(requireContext(), msg.getId());
                    db.updateStatus(msg.getId(), ScheduledMessage.STATUS_CANCELLED);
                    load();
                })
                .setNegativeButton("Keep", null)
                .show();
    }
}