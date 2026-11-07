package com.prohori.app.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prohori.app.R;
import com.prohori.app.models.RiskResult;
import com.prohori.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Shows apps with their risk badge. Used by the scan result and installed apps screens. */
public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder> {

    public interface OnAppClick {
        void onClick(RiskResult result);
    }

    private final List<RiskResult> items = new ArrayList<>();
    private final PackageManager packageManager;
    private final OnAppClick listener;

    public AppsAdapter(Context context, OnAppClick listener) {
        this.packageManager = context.getPackageManager();
        this.listener = listener;
    }

    public void setItems(List<RiskResult> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final RiskResult item = items.get(position);
        holder.name.setText(item.appName);
        holder.packageName.setText(item.packageName);
        if (item.installSource != null && !item.installSource.isEmpty()) {
            holder.source.setText(item.installSource);
            holder.source.setVisibility(View.VISIBLE);
        } else {
            holder.source.setVisibility(View.GONE);
        }
        UiUtils.styleBadge(holder.badge, item.riskLevel, item.riskLevel.label + " " + item.riskScore);
        try {
            holder.icon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
        } catch (PackageManager.NameNotFoundException e) {
            holder.icon.setImageResource(R.drawable.ic_apps);
        }
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final TextView source;
        final TextView badge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_icon);
            name = itemView.findViewById(R.id.tv_name);
            packageName = itemView.findViewById(R.id.tv_package);
            source = itemView.findViewById(R.id.tv_source);
            badge = itemView.findViewById(R.id.tv_badge);
        }
    }
}
