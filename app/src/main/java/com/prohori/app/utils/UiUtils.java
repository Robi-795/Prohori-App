package com.prohori.app.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.prohori.app.R;
import com.prohori.app.models.RiskLevel;

import java.text.DateFormat;
import java.util.Date;

public final class UiUtils {

    private UiUtils() { }

    public static int riskColor(Context context, RiskLevel level) {
        int res;
        switch (level) {
            case CRITICAL: res = R.color.risk_critical; break;
            case HIGH: res = R.color.risk_high; break;
            case MEDIUM: res = R.color.risk_medium; break;
            case LOW: res = R.color.risk_low; break;
            default: res = R.color.risk_safe; break;
        }
        return ContextCompat.getColor(context, res);
    }

    /** Colours a TextView as a rounded risk badge. */
    public static void styleBadge(TextView view, RiskLevel level, String text) {
        view.setText(text);
        view.setBackgroundResource(R.drawable.bg_badge);
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(riskColor(view.getContext(), level)));
        view.setTextColor(ContextCompat.getColor(view.getContext(), R.color.bg));
    }

    public static String formatDate(long millis) {
        if (millis <= 0) return "unknown";
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(millis));
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /** At least 8 characters with a letter and a number. */
    public static boolean isStrongPassword(String password) {
        return password != null && password.length() >= 8
                && password.matches(".*[A-Za-z].*") && password.matches(".*[0-9].*");
    }
}
