package com.campuslink.pp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etUniversity;
    private TextInputEditText etCampus;
    private TextInputEditText etPassword;

    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvLogin;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final String[] universities = new String[] {
            "Kaimosi Friends University (KAFU)",
            "Maseno University",
            "Masinde Muliro University of Science and Technology (MMUST)",
            "Kisii University",
            "Alupe University",
            "Kibabii University"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etUniversity = findViewById(R.id.etUniversity);
        etCampus = findViewById(R.id.etCampus);
        etPassword = findViewById(R.id.etPassword);

        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvLogin = findViewById(R.id.tvLogin);

        etUniversity.setFocusable(false);
        etUniversity.setClickable(true);
        etUniversity.setOnClickListener(v -> showUniversityPickerDialog());

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void showUniversityPickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Your University")
                .setItems(universities, (dialog, which) -> {
                    etUniversity.setText(universities[which]);
                })
                .show();
    }

    private void registerUser() {
        String fullName = getText(etFullName);
        String email = getText(etEmail);
        String phone = getText(etPhone);
        String university = getText(etUniversity);
        String campus = getText(etCampus);
        String password = getText(etPassword);

        if (fullName.isEmpty()) {
            etFullName.setError("Enter your full name");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Enter your email");
            etEmail.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Enter your phone number");
            etPhone.requestFocus();
            return;
        }

        if (university.isEmpty()) {
            etUniversity.setError("Select your university");
            etUniversity.requestFocus();
            return;
        }

        if (campus.isEmpty()) {
            etCampus.setError("Enter your campus");
            etCampus.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        String url = SupabaseConfig.SUPABASE_URL + "/auth/v1/signup";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            JSONObject dataObj = new JSONObject();
            dataObj.put("full_name", fullName);
            dataObj.put("phone", phone);
            dataObj.put("university", university);
            dataObj.put("campus", campus);
            jsonBody.put("data", dataObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = "";
                try {
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }
                } catch (Exception ignored) {}

                final String finalResponseBody = responseBody;
                mainHandler.post(() -> {
                    setLoading(false);
                    if (response.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = "Registration failed (Code " + response.code() + ")";
                        try {
                            JSONObject errObj = new JSONObject(finalResponseBody);
                            if (errObj.has("error_description")) {
                                errorMessage = errObj.getString("error_description");
                            } else if (errObj.has("msg")) {
                                errorMessage = errObj.getString("msg");
                            } else if (errObj.has("message")) {
                                errorMessage = errObj.getString("message");
                            }
                        } catch (Exception ignored) {}
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    private String getText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}
