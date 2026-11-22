package com.campuslink.pp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvRegister;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String PREFS_NAME = "CampusLinkSession";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_ID = "user_id";
    private static final String REFRESH_TOKEN = "refresh_token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            ));
            finish();
        });
    }

    private void attemptLogin() {

        String email = getText(etEmail);
        String password = getText(etPassword);

        if (email.isEmpty()) {
            etEmail.setError("Enter your email");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Enter your password");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        String url =
                SupabaseConfig.SUPABASE_URL
                        + "/auth/v1/token?grant_type=password";

        JSONObject jsonBody = new JSONObject();

        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            setLoading(false);
            Toast.makeText(
                    this,
                    "Unable to prepare login request",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader(
                        "apikey",
                        SupabaseConfig.SUPABASE_KEY
                )
                .addHeader(
                        "Authorization",
                        "Bearer " + SupabaseConfig.SUPABASE_KEY
                )
                .addHeader(
                        "Content-Type",
                        "application/json"
                )
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                mainHandler.post(() -> {

                    setLoading(false);

                    Toast.makeText(
                            LoginActivity.this,
                            "Network error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response
            ) throws IOException {

                String responseBody = "";

                try {
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }
                } catch (Exception ignored) {
                }

                final String finalResponseBody = responseBody;
                final int responseCode = response.code();
                final boolean successful = response.isSuccessful();

                mainHandler.post(() -> {

                    setLoading(false);

                    if (successful) {

                        try {

                            JSONObject responseJson =
                                    new JSONObject(finalResponseBody);

                            String accessToken =
                                    responseJson.getString("access_token");

                            String refreshToken =
                                    responseJson.optString(
                                            "refresh_token",
                                            ""
                                    );

                            JSONObject userObject =
                                    responseJson.getJSONObject("user");

                            String userId =
                                    userObject.getString("id");

                            // Save authenticated Supabase session
                            SharedPreferences preferences =
                                    getSharedPreferences(
                                            PREFS_NAME,
                                            MODE_PRIVATE
                                    );

                            preferences.edit()
                                    .putString(
                                            ACCESS_TOKEN,
                                            accessToken
                                    )
                                    .putString(
                                            USER_ID,
                                            userId
                                    )
                                    .putString(
                                            REFRESH_TOKEN,
                                            refreshToken
                                    )
                                    .apply();

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Intent intent =
                                    new Intent(
                                            LoginActivity.this,
                                            DashboardActivity.class
                                    );

                            startActivity(intent);
                            finish();

                        } catch (Exception e) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login succeeded, but session could not be saved.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {

                        String errorMessage =
                                "Login failed (Code "
                                        + responseCode
                                        + ")";

                        try {

                            JSONObject errObj =
                                    new JSONObject(finalResponseBody);

                            if (errObj.has("error_description")) {

                                errorMessage =
                                        errObj.getString(
                                                "error_description"
                                        );

                            } else if (errObj.has("msg")) {

                                errorMessage =
                                        errObj.getString("msg");

                            } else if (errObj.has("message")) {

                                errorMessage =
                                        errObj.getString("message");
                            }

                        } catch (Exception ignored) {
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
    }

    private void setLoading(boolean loading) {

        progressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        btnLogin.setEnabled(!loading);
    }

    private String getText(
            TextInputEditText editText
    ) {

        if (editText.getText() == null) {
            return "";
        }

        return editText.getText()
                .toString()
                .trim();
    }
}