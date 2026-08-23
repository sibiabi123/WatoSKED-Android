package com.sibiabi.watosked.adapter;

import android.content.Context;
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

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final Context context;
    private final List<ScheduledMessage> schedules;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());

    public ScheduleAdapter(Context context, List<ScheduledMessage> schedules) {
        this.context = context;
        this.schedules = schedules;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduledMessage item = schedules.get(position);
        holder.tvRecipient.setText(item.getRecipient());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText("⏰ Scheduled: " + dateFormat.format(new Date(item.getTimestamp())));
        holder.tvStatus.setText(item.getStatus());

        if ("SENT".equalsIgnoreCase(item.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_sent);
            holder.tvStatus.setTextColor(Color.parseColor("#000000"));
        } else if ("FAILED".equalsIgnoreCase(item.getStatus()) || "MISSED".equalsIgnoreCase(item.getStatus())) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#E53935"));
            holder.tvStatus.setTextColor(Color.WHITE);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#000000"));
        }
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecipient, tvMessage, tvTime, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecipient = itemView.findViewById(R.id.tvRecipient);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
