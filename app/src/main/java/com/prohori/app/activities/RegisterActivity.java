package com.prohori.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.prohori.app.R;
import com.prohori.app.firebase.AuthErrors;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.utils.UiUtils;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirm;
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirm;
    private MaterialButton btnRegister;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilName = findViewById(R.id.til_name);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirm = findViewById(R.id.til_confirm);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirm = findViewById(R.id.et_confirm);
        btnRegister = findViewById(R.id.btn_register);
        progress = findViewById(R.id.progress);

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tv_login).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);

        final String name = UiUtils.text(etName).trim();
        final String email = UiUtils.text(etEmail).trim();
        String password = UiUtils.text(etPassword);
        String confirm = UiUtils.text(etConfirm);

        boolean valid = true;
        if (name.isEmpty()) {
            tilName.setError("Enter your name");
            valid = false;
        }
        if (!UiUtils.isValidEmail(email)) {
            tilEmail.setError("Enter a valid email address");
            valid = false;
        }
        if (!UiUtils.isStrongPassword(password)) {
            tilPassword.setError("Use at least 8 characters with a letter and a number");
            valid = false;
        }
        if (!password.equals(confirm)) {
            tilConfirm.setError("Passwords do not match");
            valid = false;
        }
        if (!valid) return;

        setLoading(true);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        UiUtils.toast(this, AuthErrors.message(task.getException()));
                        return;
                    }
                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        UiUtils.toast(this, "Account created. Please sign in.");
                        finish();
                        return;
                    }
                    UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build();
                    user.updateProfile(request).addOnCompleteListener(this, profileTask -> {
                        FirebaseManager.get().saveUserDoc(user.getUid(), name, email);
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
