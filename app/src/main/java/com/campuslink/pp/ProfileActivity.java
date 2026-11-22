package com.campuslink.pp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatar;
    private TextView tvFullName;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvUniversity;
    private TextView tvCampus;

    private TextView tvTotalListings;
    private TextView tvActiveListings;
    private TextView tvOtherListings;

    private LinearLayout myListingsContainer;

    private ProgressBar progressBar;

    private MaterialButton btnMyListings;
    private MaterialButton btnLogout;

    private final OkHttpClient client = new OkHttpClient();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private String currentUserId = "";
    private String currentFullName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        initializeViews();
        setupButtons();
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!currentUserId.isEmpty()) {
            loadListings(currentUserId);
        }
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        tvAvatar = findViewById(R.id.tvAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvUniversity = findViewById(R.id.tvUniversity);
        tvCampus = findViewById(R.id.tvCampus);

        tvTotalListings = findViewById(R.id.tvTotalListings);
        tvActiveListings = findViewById(R.id.tvActiveListings);
        tvOtherListings = findViewById(R.id.tvOtherListings);

        myListingsContainer =
                findViewById(R.id.myListingsContainer);

        progressBar =
                findViewById(R.id.progressBar);

        btnMyListings =
                findViewById(R.id.btnMyListings);

        btnLogout =
                findViewById(R.id.btnLogout);
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
        });

        // My Listings button
        btnMyListings.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            MyListingsActivity.class
                    );

            startActivity(intent);
        });

        // View All button
        findViewById(R.id.btnViewAll).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            MyListingsActivity.class
                    );

            startActivity(intent);
        });

        // Logout button
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    // =========================================================
    // LOAD CURRENT USER
    // =========================================================

    private void loadUserProfile() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "CampusLinkSession",
                        Context.MODE_PRIVATE
                );

        String accessToken =
                preferences.getString(
                        "access_token",
                        ""
                );

        if (accessToken.isEmpty()) {

            Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            goToLogin();

            return;
        }

        setLoading(true);

        String url =
                SupabaseConfig.SUPABASE_URL
                        + "/auth/v1/user";

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader(
                                "apikey",
                                SupabaseConfig.SUPABASE_KEY
                        )
                        .addHeader(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e
                    ) {

                        mainHandler.post(() -> {

                            setLoading(false);

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Unable to load profile: "
                                            + e.getMessage(),
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

                        if (response.body() != null) {
                            responseBody =
                                    response.body().string();
                        }

                        boolean success =
                                response.isSuccessful();

                        final String finalBody =
                                responseBody;

                        mainHandler.post(() -> {

                            setLoading(false);

                            if (!success) {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        "Unable to load profile.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            try {

                                JSONObject user =
                                        new JSONObject(finalBody);

                                currentUserId =
                                        user.optString(
                                                "id",
                                                ""
                                        );

                                String email =
                                        user.optString(
                                                "email",
                                                ""
                                        );

                                JSONObject metadata =
                                        user.optJSONObject(
                                                "user_metadata"
                                        );

                                String fullName = "";
                                String phone = "";
                                String university = "";
                                String campus = "";

                                if (metadata != null) {

                                    fullName =
                                            metadata.optString(
                                                    "full_name",
                                                    ""
                                            );

                                    phone =
                                            metadata.optString(
                                                    "phone",
                                                    ""
                                            );

                                    university =
                                            metadata.optString(
                                                    "university",
                                                    ""
                                            );

                                    campus =
                                            metadata.optString(
                                                    "campus",
                                                    ""
                                            );
                                }

                                currentFullName =
                                        fullName;

                                displayUserInformation(
                                        fullName,
                                        email,
                                        phone,
                                        university,
                                        campus
                                );

                                if (!currentUserId.isEmpty()) {

                                    loadListings(
                                            currentUserId
                                    );
                                }

                            } catch (Exception e) {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        "Error reading profile.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // DISPLAY USER INFORMATION
    // =========================================================

    private void displayUserInformation(
            String fullName,
            String email,
            String phone,
            String university,
            String campus
    ) {

        if (fullName == null || fullName.isEmpty()) {
            fullName = "CampusLink User";
        }

        tvFullName.setText(fullName);

        tvEmail.setText(
                email == null || email.isEmpty()
                        ? "Email not available"
                        : email
        );

        tvPhone.setText(
                phone == null || phone.isEmpty()
                        ? "Not provided"
                        : phone
        );

        tvUniversity.setText(
                university == null || university.isEmpty()
                        ? "Not provided"
                        : university
        );

        tvCampus.setText(
                campus == null || campus.isEmpty()
                        ? "Not provided"
                        : campus
        );

        String firstLetter =
                fullName.substring(0, 1).toUpperCase();

        tvAvatar.setText(firstLetter);
    }

    // =========================================================
    // LOAD USER LISTINGS
    // =========================================================

    private void loadListings(String userId) {

        if (userId == null || userId.isEmpty()) {
            return;
        }

        String url =
                SupabaseConfig.SUPABASE_URL
                        + "/rest/v1/listings"
                        + "?select=id,title,description,price,"
                        + "location,status,created_at"
                        + "&user_id=eq."
                        + userId
                        + "&order=created_at.desc";

        SharedPreferences preferences =
                getSharedPreferences(
                        "CampusLinkSession",
                        Context.MODE_PRIVATE
                );

        String accessToken =
                preferences.getString(
                        "access_token",
                        ""
                );

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader(
                                "apikey",
                                SupabaseConfig.SUPABASE_KEY
                        )
                        .addHeader(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e
                    ) {

                        mainHandler.post(() -> {

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Unable to load your listings.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(
                            Call call,
                            Response response
                    ) throws IOException {

                        String responseBody = "";

                        if (response.body() != null) {

                            responseBody =
                                    response.body().string();
                        }

                        final String finalBody =
                                responseBody;

                        mainHandler.post(() -> {

                            try {

                                JSONArray listings =
                                        new JSONArray(
                                                finalBody
                                        );

                                displayListings(
                                        listings
                                );

                            } catch (Exception e) {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        "Error loading listings.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // DISPLAY LISTINGS
    // =========================================================

    private void displayListings(JSONArray listings) {

        myListingsContainer.removeAllViews();

        int total =
                listings.length();

        int active = 0;
        int other = 0;

        for (int i = 0; i < total; i++) {

            try {

                JSONObject listing =
                        listings.getJSONObject(i);

                String status =
                        listing.optString(
                                "status",
                                ""
                        );

                if ("active".equalsIgnoreCase(status)) {

                    active++;

                } else {

                    other++;
                }

            } catch (Exception ignored) {
            }
        }

        tvTotalListings.setText(
                String.valueOf(total)
        );

        tvActiveListings.setText(
                String.valueOf(active)
        );

        tvOtherListings.setText(
                String.valueOf(other)
        );

        // No listings
        if (total == 0) {

            TextView emptyText =
                    new TextView(this);

            emptyText.setText(
                    "You haven't posted any listings yet."
            );

            emptyText.setTextSize(15);

            emptyText.setTextColor(
                    android.graphics.Color.DKGRAY
            );

            emptyText.setGravity(
                    Gravity.CENTER
            );

            emptyText.setPadding(
                    20,
                    30,
                    20,
                    30
            );

            myListingsContainer.addView(
                    emptyText
            );

            return;
        }

        // Show only first 3 listings on Profile
        int limit =
                Math.min(total, 3);

        for (int i = 0; i < limit; i++) {

            try {

                JSONObject listing =
                        listings.getJSONObject(i);

                addListingCard(listing);

            } catch (Exception ignored) {
            }
        }
    }

    // =========================================================
    // LISTING CARD
    // =========================================================

    private void addListingCard(JSONObject listing) {

        String title =
                listing.optString(
                        "title",
                        "Untitled Listing"
                );

        String description =
                listing.optString(
                        "description",
                        ""
                );

        String price =
                listing.optString(
                        "price",
                        "0"
                );

        String location =
                listing.optString(
                        "location",
                        "Location not specified"
                );

        String status =
                listing.optString(
                        "status",
                        "unknown"
                );

        MaterialCardView card =
                new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                16
        );

        card.setLayoutParams(cardParams);

        card.setRadius(18);
        card.setCardElevation(3);
        card.setStrokeWidth(1);

        card.setStrokeColor(
                android.graphics.Color.parseColor(
                        "#DCEFE2"
                )
        );

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                18,
                16,
                18,
                16
        );

        // Title
        TextView titleView =
                new TextView(this);

        titleView.setText(title);

        titleView.setTextSize(17);

        titleView.setTypeface(
                null,
                Typeface.BOLD
        );

        titleView.setTextColor(
                android.graphics.Color.parseColor(
                        "#07133F"
                )
        );

        content.addView(titleView);

        // Description
        if (!description.isEmpty()) {

            TextView descriptionView =
                    new TextView(this);

            String shortDescription =
                    description.length() > 80
                            ? description.substring(0, 80) + "..."
                            : description;

            descriptionView.setText(
                    shortDescription
            );

            descriptionView.setTextSize(13);

            descriptionView.setTextColor(
                    android.graphics.Color.DKGRAY
            );

            LinearLayout.LayoutParams descParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            descParams.setMargins(
                    0,
                    6,
                    0,
                    6
            );

            descriptionView.setLayoutParams(
                    descParams
            );

            content.addView(
                    descriptionView
            );
        }

        // Price
        TextView priceView =
                new TextView(this);

        priceView.setText(
                "KSh " + formatPrice(price)
        );

        priceView.setTextSize(16);

        priceView.setTypeface(
                null,
                Typeface.BOLD
        );

        priceView.setTextColor(
                android.graphics.Color.parseColor(
                        "#079A48"
                )
        );

        content.addView(priceView);

        // Location
        TextView locationView =
                new TextView(this);

        locationView.setText(
                "📍 " + location
        );

        locationView.setTextSize(13);

        locationView.setTextColor(
                android.graphics.Color.DKGRAY
        );

        LinearLayout.LayoutParams locationParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        locationParams.setMargins(
                0,
                5,
                0,
                5
        );

        locationView.setLayoutParams(
                locationParams
        );

        content.addView(locationView);

        // Status
        TextView statusView =
                new TextView(this);

        statusView.setText(
                "Status: " + status
        );

        statusView.setTextSize(12);

        statusView.setTextColor(
                android.graphics.Color.GRAY
        );

        content.addView(statusView);

        card.addView(content);

        // Open listing details
        card.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            ListingDetailsActivity.class
                    );

            intent.putExtra(
                    "title",
                    title
            );

            intent.putExtra(
                    "description",
                    description
            );

            intent.putExtra(
                    "price",
                    price
            );

            intent.putExtra(
                    "location",
                    location
            );

            intent.putExtra(
                    "seller_name",
                    currentFullName
            );

            intent.putExtra(
                    "status",
                    status
            );

            startActivity(intent);
        });

        myListingsContainer.addView(card);
    }

    // =========================================================
    // FORMAT PRICE
    // =========================================================

    private String formatPrice(String price) {

        try {

            double value =
                    Double.parseDouble(price);

            NumberFormat formatter =
                    NumberFormat.getNumberInstance(
                            Locale.US
                    );

            formatter.setMaximumFractionDigits(0);

            return formatter.format(value);

        } catch (Exception e) {

            return price;
        }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void logoutUser() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "CampusLinkSession",
                        Context.MODE_PRIVATE
                );

        preferences.edit()
                .clear()
                .apply();

        Toast.makeText(
                this,
                "Logged out successfully.",
                Toast.LENGTH_SHORT
        ).show();

        goToLogin();
    }

    // =========================================================
    // GO TO LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent =
                new Intent(
                        ProfileActivity.this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // LOADING
    // =========================================================

    private void setLoading(boolean loading) {

        progressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );
    }
}