package com.sibiabi.watosked.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.sibiabi.watosked.R;
import com.sibiabi.watosked.model.ScheduledMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {

    public interface OnDeleteClick {
        void onDelete(ScheduledMessage msg);
    }

    private final List<ScheduledMessage> list;
    private final OnDeleteClick          listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public ScheduleAdapter(List<ScheduledMessage> list, OnDeleteClick listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ScheduledMessage s = list.get(pos);

        h.tvName.setText(s.getDisplayName());
        h.tvPhone.setText(s.getRecipient());

        String preview = s.getMessage();
        h.tvMessage.setText(preview.length() > 70 ? preview.substring(0, 70) + "..." : preview);
        h.tvTime.setText(fmt.format(new Date(s.getTimestamp())));

        // Repeat chip
        if (!ScheduledMessage.REPEAT_NONE.equals(s.getRepeatType())) {
            h.chipRepeat.setVisibility(View.VISIBLE);
            h.chipRepeat.setText(s.getRepeatType());
        } else {
            h.chipRepeat.setVisibility(View.GONE);
        }

        // WA type chip
        h.chipWaType.setText(s.isWhatsAppBusiness() ? "Business" : "WhatsApp");

        h.btnDelete.setOnClickListener(v -> listener.onDelete(s));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView    tvName, tvPhone, tvMessage, tvTime;
        Chip        chipRepeat, chipWaType;
        ImageButton btnDelete;

        VH(View v) {
            super(v);
            tvName     = v.findViewById(R.id.tvName);
            tvPhone    = v.findViewById(R.id.tvPhone);
            tvMessage  = v.findViewById(R.id.tvMessage);
            tvTime     = v.findViewById(R.id.tvTime);
            chipRepeat = v.findViewById(R.id.chipRepeat);
            chipWaType = v.findViewById(R.id.chipWaType);
            btnDelete  = v.findViewById(R.id.btnDelete);
        }
    }
}