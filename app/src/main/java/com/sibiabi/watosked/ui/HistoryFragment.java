package com.sibiabi.watosked.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sibiabi.watosked.R;
import com.sibiabi.watosked.adapter.HistoryAdapter;
import com.sibiabi.watosked.db.DatabaseHelper;
import com.sibiabi.watosked.model.ScheduledMessage;

import java.util.List;

public class HistoryFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_history, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DatabaseHelper db   = new DatabaseHelper(requireContext());
        RecyclerView   rv   = view.findViewById(R.id.rvHistory);
        TextView   tvEmpty  = view.findViewById(R.id.tvEmpty);
        List<ScheduledMessage> list = db.getHistorySchedules();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new HistoryAdapter(list));
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }
}