package com.prohori.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prohori.app.R;
import com.prohori.app.models.ScanReport;
import com.prohori.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnScanClick {
        void onClick(ScanReport report);
    }

    public interface OnScanLongClick {
        void onLongClick(ScanReport report);
    }

    private final List<ScanReport> items = new ArrayList<>();
    private final OnScanClick clickListener;
    private final OnScanLongClick longClickListener;

    public HistoryAdapter(OnScanClick clickListener, OnScanLongClick longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setItems(List<ScanReport> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ScanReport r = items.get(position);
        holder.type.setText(r.scanType);
        holder.date.setText(UiUtils.formatDate(r.scanDate));
        holder.summary.setText(r.totalApps + " apps inspected, " + r.riskyApps() + " to review");
        holder.breakdown.setText("Critical " + r.critical + "   High " + r.high
                + "   Medium " + r.medium + "   Low " + r.low);
        holder.itemView.setOnClickListener(v -> clickListener.onClick(r));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onLongClick(r);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView type;
        final TextView date;
        final TextView summary;
        final TextView breakdown;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.tv_type);
            date = itemView.findViewById(R.id.tv_date);
            summary = itemView.findViewById(R.id.tv_summary);
            breakdown = itemView.findViewById(R.id.tv_breakdown);
        }
    }
}
