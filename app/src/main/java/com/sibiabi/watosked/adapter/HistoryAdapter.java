package com.sibiabi.watosked.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sibiabi.watosked.R;
import com.sibiabi.watosked.model.ScheduledMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    private final List<ScheduledMessage> list;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public HistoryAdapter(List<ScheduledMessage> list) { this.list = list; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ScheduledMessage s = list.get(pos);
        h.tvName.setText(s.getDisplayName());
        h.tvPhone.setText(s.getRecipient());
        h.tvMessage.setText(s.getMessage().length() > 80
                ? s.getMessage().substring(0, 80) + "..." : s.getMessage());
        h.tvTime.setText(fmt.format(new Date(s.getTimestamp())));
        boolean sent = ScheduledMessage.STATUS_SENT.equals(s.getStatus());
        h.tvStatus.setText(sent ? "✅ SENT" : "❌ FAILED");
        h.tvStatus.setTextColor(sent ? Color.parseColor("#4CAF50") : Color.parseColor("#FF5252"));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvMessage, tvTime, tvStatus;
        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvName);
            tvPhone   = v.findViewById(R.id.tvPhone);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime    = v.findViewById(R.id.tvTime);
            tvStatus  = v.findViewById(R.id.tvStatus);
        }
    }
}