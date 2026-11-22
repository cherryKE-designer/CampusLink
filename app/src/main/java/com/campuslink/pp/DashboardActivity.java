
package com.campuslink.pp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity {

    // =========================
    // UI
    // =========================

    private MaterialButton btnLogout;
    private MaterialButton btnPost;
    private FrameLayout notificationButton;

    private EditText searchInput;
    private ViewFlipper bannerFlipper;

    private LinearLayout categoryBuySell;
    private LinearLayout categoryAdvertise;
    private LinearLayout categoryServices;
    private LinearLayout categoryConnect;

    private LinearLayout navHome;
    private LinearLayout navCategories;
    private FrameLayout navPost;
    private LinearLayout navMessages;
    private LinearLayout navProfile;

    private LinearLayout listingsContainer;

    // =========================
    // Networking
    // =========================

    private final OkHttpClient client = new OkHttpClient();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    // =========================
    // Session
    // =========================

    private static final String PREFS_NAME = "CampusLinkSession";
    private static final String ACCESS_TOKEN = "access_token";

    // =========================
    // Category preferences
    // =========================

    private static final String CATEGORY_PREFS =
            "CampusLinkCategoryPrefs";

    private static final String KEY_SELECTED_CATEGORIES =
            "selected_categories";

    private Set<String> getSelectedCategories() {
        Set<String> saved = getSharedPreferences(
                CATEGORY_PREFS,
                MODE_PRIVATE
        ).getStringSet(
                KEY_SELECTED_CATEGORIES,
                Collections.emptySet()
        );

        return new HashSet<>(saved);
    }

    private void openCategorySelection() {
        Intent intent = new Intent(
                DashboardActivity.this,
                CategorySelectionActivity.class
        );

        startActivity(intent);
    }

    // =========================
    // Listings
    // =========================

    private JSONArray allListings = new JSONArray();

    // =========================
    // Lifecycle
    // =========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupBanner();
        setupButtons();
        setupSearch();
        setupCategories();
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadListings();
    }

    // =========================
    // Initialize views
    // =========================

    private void initializeViews() {
        btnLogout = findViewById(R.id.btnLogout);
        btnPost = findViewById(R.id.btnPost);
        notificationButton = findViewById(R.id.notificationButton);
        searchInput = findViewById(R.id.searchInput);
        bannerFlipper = findViewById(R.id.bannerFlipper);

        categoryBuySell = findViewById(R.id.categoryBuySell);
        categoryAdvertise = findViewById(R.id.categoryAdvertise);
        categoryServices = findViewById(R.id.categoryServices);
        categoryConnect = findViewById(R.id.categoryConnect);

        navHome = findViewById(R.id.navHome);
        navCategories = findViewById(R.id.navCategories);
        navPost = findViewById(R.id.navPost);
        navMessages = findViewById(R.id.navMessages);
        navProfile = findViewById(R.id.navProfile);

        listingsContainer = findViewById(R.id.listingsContainer);
    }

    // =========================
    // Banner
    // =========================

    private void setupBanner() {
        if (bannerFlipper == null) {
            return;
        }

        bannerFlipper.setAutoStart(true);
        bannerFlipper.setFlipInterval(3500);
        bannerFlipper.startFlipping();
    }

    // =========================
    // Buttons
    // =========================

    private void setupButtons() {
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPreferences preferences = getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

                preferences.edit().clear().apply();

                Intent intent = new Intent(
                        DashboardActivity.this,
                        LoginActivity.class
                );

                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                startActivity(intent);
                finish();
            });
        }

        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> {
                Intent intent = new Intent(
                        DashboardActivity.this,
                        MessagesInboxActivity.class
                );
                startActivity(intent);
            });
        }

        if (btnPost != null) {
            btnPost.setOnClickListener(v -> openPostListing());
        }
    }

    // =========================
    // Search
    // =========================

    private void setupSearch() {
        if (searchInput == null) {
            return;
        }

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // =========================
    // Categories
    // =========================

    private void setupCategories() {
        View.OnClickListener openSelector =
                v -> openCategorySelection();

        if (categoryBuySell != null) {
            categoryBuySell.setOnClickListener(openSelector);
        }

        if (categoryAdvertise != null) {
            categoryAdvertise.setOnClickListener(openSelector);
        }

        if (categoryServices != null) {
            categoryServices.setOnClickListener(openSelector);
        }

        if (categoryConnect != null) {
            categoryConnect.setOnClickListener(openSelector);
        }
    }

    // =========================
    // Bottom navigation
    // =========================

    private void setupNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v ->
                    Toast.makeText(
                            this,
                            "Home",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }

        if (navCategories != null) {
            navCategories.setOnClickListener(v ->
                    openCategorySelection()
            );
        }

        if (navPost != null) {
            navPost.setOnClickListener(v -> openPostListing());
        }

        if (navMessages != null) {
            navMessages.setOnClickListener(v -> {
                Intent intent = new Intent(
                        DashboardActivity.this,
                        MessagesInboxActivity.class
                );

                startActivity(intent);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(
                        DashboardActivity.this,
                        ProfileActivity.class
                );

                startActivity(intent);
            });
        }
    }

    // =========================
    // Open post listing
    // =========================

    private void openPostListing() {
        Intent intent = new Intent(
                DashboardActivity.this,
                PostListingActivity.class
        );

        startActivity(intent);
    }

    // =========================
    // Load listings from Supabase
    // =========================

    private void loadListings() {
        SharedPreferences preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        String accessToken = preferences.getString(
                ACCESS_TOKEN,
                ""
        );

        if (accessToken == null || accessToken.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_SHORT
            ).show();

            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        /*
         * listings.user_id references the seller profile ID.
         * Selecting * includes user_id, which is required to
         * open a conversation with the listing's seller.
         */

        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/listings"
                + "?select=*,profiles(full_name),categories(name,icon)"
                + "&status=eq.active"
                + "&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() ->
                        showListingsMessage(
                                "Unable to load listings. Check your internet connection."
                        )
                );
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response
            ) throws IOException {

                try (Response res = response) {
                    if (!res.isSuccessful()) {
                        int statusCode = res.code();

                        String errorBody = res.body() != null
                                ? res.body().string()
                                : "";

                        android.util.Log.e(
                                "CampusLink",
                                "Listings error " + statusCode + ": " + errorBody
                        );

                        mainHandler.post(() -> {
                            showListingsMessage("Unable to load listings.");

                            Toast.makeText(
                                    DashboardActivity.this,
                                    "Server error: " + statusCode,
                                    Toast.LENGTH_LONG
                            ).show();
                        });

                        return;
                    }

                    String json = res.body() != null
                            ? res.body().string()
                            : "[]";

                    try {
                        JSONArray listings = new JSONArray(json);

                        mainHandler.post(() -> {
                            allListings = listings;
                            applyFilters();
                        });
                    } catch (Exception e) {
                        android.util.Log.e(
                                "CampusLink",
                                "Could not parse listings JSON",
                                e
                        );

                        mainHandler.post(() ->
                                showListingsMessage(
                                        "Unable to read listings."
                                )
                        );
                    }
                }
            }
        });
    }

    // =========================
    // Apply search and category filters
    // =========================

    private void applyFilters() {
        if (allListings == null) {
            return;
        }

        String searchText = "";

        if (searchInput != null) {
            searchText = searchInput.getText()
                    .toString()
                    .trim()
                    .toLowerCase(Locale.getDefault());
        }

        Set<String> selectedCategories = getSelectedCategories();
        JSONArray filteredListings = new JSONArray();

        for (int i = 0; i < allListings.length(); i++) {
            try {
                JSONObject listing = allListings.getJSONObject(i);

                String title = listing.optString("title", "");
                String description = listing.optString("description", "");
                String location = listing.optString("location", "");
                String phone = listing.optString("phone", "");

                JSONObject categoryObject =
                        listing.optJSONObject("categories");

                String category = "Other";
                String categoryIcon = "📦";

                if (categoryObject != null) {
                    category = categoryObject.optString("name", "Other");
                    categoryIcon = categoryObject.optString("icon", "📦");
                }

                String sellerName = "";
                JSONObject profile = listing.optJSONObject("profiles");

                if (profile != null) {
                    sellerName = profile.optString("full_name", "");
                }

                boolean categoryMatches = selectedCategories.isEmpty();

                if (!categoryMatches) {
                    for (String chosenCategory : selectedCategories) {
                        if (category.equalsIgnoreCase(chosenCategory)) {
                            categoryMatches = true;
                            break;
                        }
                    }
                }

                if (!categoryMatches) {
                    continue;
                }

                String searchableText = (
                        title + " "
                                + description + " "
                                + location + " "
                                + category + " "
                                + categoryIcon + " "
                                + sellerName + " "
                                + phone
                ).toLowerCase(Locale.getDefault());

                boolean searchMatches = searchText.isEmpty()
                        || searchableText.contains(searchText);

                if (searchMatches) {
                    filteredListings.put(listing);
                }

            } catch (Exception e) {
                android.util.Log.e(
                        "CampusLink",
                        "Error filtering listing",
                        e
                );
            }
        }

        displayListings(filteredListings);
    }

    // =========================
    // Display listings
    // =========================

    private void displayListings(JSONArray listings) {
        if (listingsContainer == null) {
            return;
        }

        listingsContainer.removeAllViews();

        if (listings.length() == 0) {
            Set<String> chosen = getSelectedCategories();

            String searchText = searchInput != null
                    ? searchInput.getText().toString().trim()
                    : "";

            String message;

            if (!searchText.isEmpty() && !chosen.isEmpty()) {
                message = "No listings found for \"" + searchText
                        + "\" in the selected categories.";
            } else if (!searchText.isEmpty()) {
                message = "No listings found for \"" + searchText + "\".";
            } else if (!chosen.isEmpty()) {
                message = "No active listings in: "
                        + android.text.TextUtils.join(", ", chosen) + ".";
            } else {
                message = "No active listings available yet.";
            }

            showListingsMessage(message);
            return;
        }

        TextView resultCount = createTextView(
                listings.length()
                        + " listing"
                        + (listings.length() == 1 ? "" : "s"),
                13,
                Color.DKGRAY
        );

        resultCount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultCount.setPadding(dp(4), dp(4), dp(4), dp(10));
        listingsContainer.addView(resultCount);

        Set<String> chosenCategories = getSelectedCategories();

        if (!chosenCategories.isEmpty()) {
            TextView filterLabel = createTextView(
                    "Selected: "
                            + android.text.TextUtils.join(", ", chosenCategories),
                    12,
                    Color.rgb(27, 94, 32)
            );

            filterLabel.setPadding(dp(4), 0, dp(4), dp(10));
            listingsContainer.addView(filterLabel);
        }

        for (int i = 0; i < listings.length(); i++) {
            try {
                addListingCard(listings.getJSONObject(i));
            } catch (Exception e) {
                android.util.Log.e(
                        "CampusLink",
                        "Error displaying listing",
                        e
                );
            }
        }
    }

    // =========================
    // Listing card
    // =========================

    private void addListingCard(JSONObject listing) {
        try {
            String title = listing.optString(
                    "title",
                    "Untitled Listing"
            );

            String description = listing.optString("description", "");
            String price = listing.optString("price", "");
            String location = listing.optString(
                    "location",
                    "Location not specified"
            );
            String phone = listing.optString("phone", "");
            String status = listing.optString("status", "active");

            JSONObject categoryObject =
                    listing.optJSONObject("categories");

            String category = "Other";
            String categoryIcon = "📦";

            if (categoryObject != null) {
                category = categoryObject.optString("name", "Other");
                categoryIcon = categoryObject.optString("icon", "📦");
            }

            String sellerName = "CampusLink Seller";
            JSONObject profile = listing.optJSONObject("profiles");

            if (profile != null) {
                String fullName = profile.optString("full_name", "").trim();

                if (!fullName.isEmpty()) {
                    sellerName = fullName;
                }
            }

            // Critical messaging fix: seller UUID from listings.user_id.
            final String finalSellerId =
                    listing.optString("user_id", "").trim();

            final long finalListingId = listing.optLong("id", 0);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));

            GradientDrawable cardBackground = new GradientDrawable();
            cardBackground.setColor(Color.WHITE);
            cardBackground.setCornerRadius(dp(16));
            cardBackground.setStroke(dp(1), Color.rgb(225, 235, 228));
            card.setBackground(cardBackground);
            card.setElevation(dp(3));

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            cardParams.setMargins(0, 0, 0, dp(14));
            card.setLayoutParams(cardParams);

            // Image
            ImageView imageView = new ImageView(this);

            imageView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(190)
                    )
            );

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            GradientDrawable imageBackground = new GradientDrawable();
            imageBackground.setColor(Color.rgb(238, 246, 240));
            imageBackground.setCornerRadius(dp(12));

            imageView.setBackground(imageBackground);
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            card.addView(imageView);

            JSONArray imageUrls = listing.optJSONArray("image_urls");
            String firstImageUrl = listing.optString("image_url", "");

            if (imageUrls != null && imageUrls.length() > 0) {
                firstImageUrl = imageUrls.optString(0, firstImageUrl);
            }

            if (!firstImageUrl.isEmpty()) {
                loadImage(firstImageUrl, imageView);
            }

            // Category badge
            TextView categoryView = createTextView(
                    categoryIcon + " " + category,
                    11,
                    Color.WHITE
            );

            categoryView.setGravity(Gravity.CENTER);
            categoryView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            categoryView.setPadding(dp(10), dp(5), dp(10), dp(5));

            GradientDrawable categoryBackground = new GradientDrawable();
            categoryBackground.setColor(Color.rgb(27, 94, 32));
            categoryBackground.setCornerRadius(dp(30));
            categoryView.setBackground(categoryBackground);

            LinearLayout.LayoutParams categoryParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            categoryParams.setMargins(0, dp(12), 0, dp(7));
            categoryView.setLayoutParams(categoryParams);
            card.addView(categoryView);

            // Title
            TextView titleView = createTextView(
                    title,
                    18,
                    Color.rgb(20, 40, 25)
            );

            titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            titleView.setPadding(0, dp(2), 0, dp(6));
            card.addView(titleView);

            // Price
            if (!price.isEmpty()) {
                TextView priceView = createTextView(
                        formatPrice(price),
                        17,
                        Color.rgb(27, 94, 32)
                );

                priceView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                priceView.setPadding(0, 0, 0, dp(7));
                card.addView(priceView);
            }

            // Location
            TextView locationView = createTextView(
                    "📍 " + location,
                    13,
                    Color.DKGRAY
            );

            locationView.setPadding(0, 0, 0, dp(5));
            card.addView(locationView);

            // Seller
            TextView sellerView = createTextView(
                    "👤 " + sellerName,
                    13,
                    Color.DKGRAY
            );

            sellerView.setPadding(0, 0, 0, dp(8));
            card.addView(sellerView);

            // Description
            if (!description.isEmpty()) {
                String shortDescription = description;

                if (shortDescription.length() > 130) {
                    shortDescription =
                            shortDescription.substring(0, 130) + "...";
                }

                TextView descriptionView = createTextView(
                        shortDescription,
                        13,
                        Color.GRAY
                );

                descriptionView.setPadding(0, 0, 0, dp(8));
                card.addView(descriptionView);
            }

            // Status
            TextView statusView = createTextView(
                    "● " + status.toUpperCase(Locale.getDefault()),
                    11,
                    Color.rgb(27, 94, 32)
            );

            statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(statusView);

            // Capture data for the click listener
            final String finalTitle = title;
            final String finalDescription = description;
            final String finalPrice = price;
            final String finalLocation = location;
            final String finalPhone = phone;
            final String finalSellerName = sellerName;
            final String finalStatus = status;
            final String finalCategory = category;
            final String finalImageUrl = firstImageUrl;

            card.setOnClickListener(v -> {
                Intent intent = new Intent(
                        DashboardActivity.this,
                        ListingDetailsActivity.class
                );

                intent.putExtra("listing_id", finalListingId);

                // Pass seller UUID to ListingDetailsActivity.
                intent.putExtra("seller_id", finalSellerId);

                intent.putExtra("title", finalTitle);
                intent.putExtra("description", finalDescription);
                intent.putExtra("price", finalPrice);
                intent.putExtra("location", finalLocation);
                intent.putExtra("phone", finalPhone);
                intent.putExtra("seller_name", finalSellerName);
                intent.putExtra("status", finalStatus);
                intent.putExtra("category", finalCategory);
                intent.putExtra("image_url", finalImageUrl);

                startActivity(intent);
            });

            listingsContainer.addView(card);

        } catch (Exception e) {
            android.util.Log.e(
                    "CampusLink",
                    "Error creating listing card",
                    e
            );
        }
    }

    // =========================
    // Load image
    // =========================

    private void loadImage(
            String imageUrl,
            ImageView imageView
    ) {
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.w(
                        "CampusLink",
                        "Unable to load listing image",
                        e
                );
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response
            ) throws IOException {

                try (Response res = response) {
                    if (!res.isSuccessful() || res.body() == null) {
                        return;
                    }

                    Bitmap bitmap;

                    try (InputStream inputStream = res.body().byteStream()) {
                        bitmap = BitmapFactory.decodeStream(inputStream);
                    }

                    if (bitmap != null) {
                        mainHandler.post(() ->
                                imageView.setImageBitmap(bitmap)
                        );
                    }
                }
            }
        });
    }

    // =========================
    // TextView helper
    // =========================

    private TextView createTextView(
            String text,
            int size,
            int color
    ) {
        TextView textView = new TextView(this);

        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        textView.setTextColor(color);
        textView.setFontFeatureSettings("kern");

        return textView;
    }

    // =========================
    // Empty/error message
    // =========================

    private void showListingsMessage(String message) {
        if (listingsContainer == null) {
            return;
        }

        listingsContainer.removeAllViews();

        TextView messageView = createTextView(
                message,
                14,
                Color.GRAY
        );

        messageView.setGravity(Gravity.CENTER);
        messageView.setPadding(
                dp(20),
                dp(35),
                dp(20),
                dp(35)
        );

        listingsContainer.addView(messageView);
    }

    // =========================
    // Price formatting
    // =========================

    private String formatPrice(String price) {
        if (price == null || price.trim().isEmpty()) {
            return "";
        }

        try {
            double amount = Double.parseDouble(price.trim());

            return String.format(
                    Locale.getDefault(),
                    "KSh %,.0f",
                    amount
            );

        } catch (Exception e) {
            return "KSh " + price;
        }
    }

    // =========================
    // DP helper
    // =========================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
