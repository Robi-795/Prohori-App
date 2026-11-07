package com.prohori.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prohori.app.R;
import com.prohori.app.models.SecurityAlert;
import com.prohori.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.ViewHolder> {

    public interface OnAlertClick {
        void onClick(SecurityAlert alert);
    }

    private final List<SecurityAlert> items = new ArrayList<>();
    private final OnAlertClick listener;

    public AlertsAdapter(OnAlertClick listener) {
        this.listener = listener;
    }

    public void setItems(List<SecurityAlert> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /** Fills one item_alert row. Also used by the dashboard for its short "recent alerts" list. */
    public static void bind(View row, SecurityAlert alert) {
        TextView title = row.findViewById(R.id.tv_title);
        TextView sub = row.findViewById(R.id.tv_sub);
        TextView time = row.findViewById(R.id.tv_time);
        TextView level = row.findViewById(R.id.tv_level);
        View dot = row.findViewById(R.id.unread_dot);
        title.setText(alert.appName);
        sub.setText(alert.packageName);
        time.setText(UiUtils.formatDate(alert.timestamp));
        UiUtils.styleBadge(level, alert.riskLevel, alert.riskLevel.label);
        dot.setVisibility(alert.isRead ? View.INVISIBLE : View.VISIBLE);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final SecurityAlert alert = items.get(position);
        bind(holder.itemView, alert);
        holder.itemView.setOnClickListener(v -> listener.onClick(alert));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
