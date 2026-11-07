package com.prohori.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.prohori.app.R;
import com.prohori.app.firebase.AuthErrors;
import com.prohori.app.utils.UiUtils;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSend;
    private ProgressBar progress;
    private TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        btnSend = findViewById(R.id.btn_send);
        progress = findViewById(R.id.progress);
        tvMessage = findViewById(R.id.tv_message);

        btnSend.setOnClickListener(v -> sendReset());
    }

    private void sendReset() {
        tilEmail.setError(null);
        String email = UiUtils.text(etEmail).trim();
        if (!UiUtils.isValidEmail(email)) {
            tilEmail.setError("Enter a valid email address");
            return;
        }
        progress.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    progress.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    Exception error = task.getException();
                    // The same message is shown whether or not the email has an account,
                    // so this screen cannot be used to discover registered emails.
                    if (task.isSuccessful() || error instanceof FirebaseAuthInvalidUserException) {
                        tvMessage.setText("If an account exists for this email, a reset link has been sent. "
                                + "Check your inbox and spam folder.");
                        tvMessage.setVisibility(View.VISIBLE);
                    } else {
                        UiUtils.toast(this, AuthErrors.message(error));
                    }
                });
    }
}
