package com.prohori.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.prohori.app.R;
import com.prohori.app.activities.AboutActivity;
import com.prohori.app.activities.LoginActivity;
import com.prohori.app.activities.SettingsActivity;
import com.prohori.app.utils.LocalStore;
import com.prohori.app.utils.UiUtils;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvAvatar = view.findViewById(R.id.tv_avatar);
        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvEmail = view.findViewById(R.id.tv_email);
        TextView tvSince = view.findViewById(R.id.tv_since);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) name = "Prohori user";
            tvName.setText(name);
            tvAvatar.setText(name.substring(0, 1).toUpperCase());
            tvEmail.setText(user.getEmail() == null ? "" : user.getEmail());
            if (user.getMetadata() != null) {
                tvSince.setText("Member since " + UiUtils.formatDate(user.getMetadata().getCreationTimestamp()));
            }
        }

        view.findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));
        view.findViewById(R.id.btn_about).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AboutActivity.class)));
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> confirmLogout());
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage("You will need to sign in again to see your history and alerts.")
                .setPositiveButton("Log out", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        Context app = requireContext().getApplicationContext();
        try {
            // Stop push notifications for the account that is leaving this phone.
            FirebaseMessaging.getInstance().deleteToken();
        } catch (Exception ignored) {
        }
        FirebaseAuth.getInstance().signOut();
        new LocalStore(app).clearAll();

        Intent intent = new Intent(app, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
