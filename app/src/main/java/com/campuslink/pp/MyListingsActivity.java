package com.campuslink.pp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyListingsActivity extends AppCompatActivity {

    private LinearLayout myListingsContainer;

    private TextView tvTotalListings;
    private TextView tvActiveListings;
    private TextView tvSoldListings;

    private final OkHttpClient client =
            new OkHttpClient();

    private String accessToken;
    private String userId;

    private static final MediaType JSON =
            MediaType.parse(
                    "application/json; charset=utf-8"
            );

    private static final String PREFS_NAME =
            "CampusLinkSession";

    private static final String ACCESS_TOKEN =
            "access_token";

    private static final String USER_ID =
            "user_id";

    // CampusLink colors
    private static final int NAVY =
            Color.rgb(7, 19, 63);

    private static final int GREEN =
            Color.rgb(7, 154, 72);

    private static final int LIGHT_GREEN =
            Color.rgb(232, 245, 233);

    private static final int TEXT_GRAY =
            Color.rgb(83, 107, 157);

    private static final int BORDER =
            Color.rgb(221, 239, 229);

    private static final int RED =
            Color.rgb(211, 47, 47);

    // Categories matching Supabase categories table
    private final String[] categoryNames = {
            "Electronics",
            "Phones",
            "Books",
            "Clothing",
            "Furniture",
            "Food",
            "Services",
            "Jobs & Gigs",
            "Accommodation",
            "Vehicles",
            "Other"
    };

    private final long[] categoryIds = {
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11
    };

    private final String[] conditions = {
            "New",
            "Like New",
            "Used - Good",
            "Used - Fair",
            "For Parts",
            "Not Applicable"
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_my_listings
        );

        myListingsContainer =
                findViewById(
                        R.id.myListingsContainer
                );

        tvTotalListings =
                findViewById(
                        R.id.tvTotalListings
                );

        tvActiveListings =
                findViewById(
                        R.id.tvActiveListings
                );

        tvSoldListings =
                findViewById(
                        R.id.tvSoldListings
                );

        // Back button
        findViewById(
                R.id.btnBack
        ).setOnClickListener(v ->
                finish()
        );

        // Refresh button
        findViewById(
                R.id.btnRefresh
        ).setOnClickListener(v -> {

            Toast.makeText(
                    MyListingsActivity.this,
                    "Refreshing listings...",
                    Toast.LENGTH_SHORT
            ).show();

            loadMyListings();
        });

        loadSession();

        if (accessToken == null
                || accessToken.isEmpty()
                || userId == null
                || userId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        loadMyListings();
    }


    private void loadSession() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        accessToken =
                prefs.getString(
                        ACCESS_TOKEN,
                        null
                );

        userId =
                prefs.getString(
                        USER_ID,
                        null
                );
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (myListingsContainer != null
                && accessToken != null
                && !accessToken.isEmpty()
                && userId != null
                && !userId.isEmpty()) {

            loadMyListings();
        }
    }


    private void loadMyListings() {

        showLoading();

        String url =
                SupabaseConfig.SUPABASE_URL
                        + "/rest/v1/listings"
                        + "?select=*,categories(name,icon)"
                        + "&user_id=eq."
                        + userId
                        + "&order=created_at.desc";

        Request request =
                new Request.Builder()
                        .url(url)
                        .addHeader(
                                "apikey",
                                SupabaseConfig.SUPABASE_KEY
                        )
                        .addHeader(
                                "Authorization",
                                "Bearer "
                                        + accessToken
                        )
                        .addHeader(
                                "Accept",
                                "application/json"
                        )
                        .get()
                        .build();

        client.newCall(request)
                .enqueue(
                        new okhttp3.Callback() {

                            @Override
                            public void onFailure(
                                    okhttp3.Call call,
                                    IOException e
                            ) {

                                runOnUiThread(() ->
                                        showError(
                                                "Unable to load your listings.\n\n"
                                                        + "Please check your internet connection."
                                        )
                                );
                            }


                            @Override
                            public void onResponse(
                                    okhttp3.Call call,
                                    Response response
                            ) throws IOException {

                                String responseBody =
                                        response.body() != null
                                                ? response.body().string()
                                                : "";

                                if (!response.isSuccessful()) {

                                    runOnUiThread(() ->
                                            showError(
                                                    "Could not load your listings.\n\n"
                                                            + "Server error "
                                                            + response.code()
                                            )
                                    );

                                    return;
                                }

                                try {

                                    JSONArray listings =
                                            new JSONArray(
                                                    responseBody
                                            );

                                    runOnUiThread(() ->
                                            displayListings(
                                                    listings
                                            )
                                    );

                                } catch (Exception e) {

                                    runOnUiThread(() ->
                                            showError(
                                                    "The listing data could not be read."
                                            )
                                    );
                                }
                            }
                        }
                );
    }


    private void showLoading() {

        myListingsContainer.removeAllViews();

        LinearLayout loadingLayout =
                new LinearLayout(this);

        loadingLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        loadingLayout.setGravity(
                Gravity.CENTER
        );

        loadingLayout.setPadding(
                30,
                60,
                30,
                60
        );

        TextView icon =
                new TextView(this);

        icon.setText("⟳");
        icon.setTextSize(38);
        icon.setTextColor(GREEN);
        icon.setGravity(Gravity.CENTER);

        loadingLayout.addView(icon);

        TextView loading =
                new TextView(this);

        loading.setText(
                "Loading your listings..."
        );

        loading.setTextSize(16);
        loading.setTypeface(
                null,
                Typeface.BOLD
        );

        loading.setTextColor(NAVY);
        loading.setGravity(Gravity.CENTER);

        loadingLayout.addView(
                loading
        );

        TextView message =
                new TextView(this);

        message.setText(
                "Please wait a moment."
        );

        message.setTextSize(13);
        message.setTextColor(TEXT_GRAY);
        message.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        messageParams.setMargins(
                0,
                dp(5),
                0,
                0
        );

        message.setLayoutParams(
                messageParams
        );

        loadingLayout.addView(
                message
        );

        myListingsContainer.addView(
                loadingLayout
        );
    }


    private void showError(String message) {

        myListingsContainer.removeAllViews();

        LinearLayout errorLayout =
                new LinearLayout(this);

        errorLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        errorLayout.setGravity(
                Gravity.CENTER
        );

        errorLayout.setPadding(
                dp(25),
                dp(60),
                dp(25),
                dp(60)
        );

        TextView icon =
                new TextView(this);

        icon.setText("⚠");
        icon.setTextSize(40);
        icon.setTextColor(RED);
        icon.setGravity(Gravity.CENTER);

        errorLayout.addView(icon);

        TextView title =
                createTextView(
                        "Something went wrong",
                        19,
                        NAVY,
                        true
                );

        title.setGravity(
                Gravity.CENTER
        );

        errorLayout.addView(title);

        TextView messageView =
                createTextView(
                        message,
                        14,
                        TEXT_GRAY,
                        false
                );

        messageView.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        messageParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        messageView.setLayoutParams(
                messageParams
        );

        errorLayout.addView(
                messageView
        );

        Button retry =
                new Button(this);

        retry.setText(
                "Retry"
        );

        retry.setTextSize(14);
        retry.setTextColor(Color.WHITE);
        retry.setAllCaps(false);

        GradientDrawable retryBackground =
                new GradientDrawable();

        retryBackground.setColor(
                GREEN
        );

        retryBackground.setCornerRadius(
                dp(12)
        );

        retry.setBackground(
                retryBackground
        );

        LinearLayout.LayoutParams retryParams =
                new LinearLayout.LayoutParams(
                        dp(130),
                        dp(50)
                );

        retryParams.gravity =
                Gravity.CENTER;

        retryParams.setMargins(
                0,
                dp(20),
                0,
                0
        );

        retry.setLayoutParams(
                retryParams
        );

        retry.setOnClickListener(
                v -> loadMyListings()
        );

        errorLayout.addView(
                retry
        );

        myListingsContainer.addView(
                errorLayout
        );
    }


    private void displayListings(
            JSONArray listings
    ) {

        myListingsContainer.removeAllViews();

        int total =
                listings.length();

        int active = 0;
        int sold = 0;

        for (int i = 0;
             i < listings.length();
             i++) {

            JSONObject listing =
                    listings.optJSONObject(i);

            if (listing == null) {
                continue;
            }

            String status =
                    listing.optString(
                            "status",
                            "active"
                    );

            if (status.equalsIgnoreCase(
                    "sold"
            )) {

                sold++;

            } else {

                active++;
            }
        }

        tvTotalListings.setText(
                String.valueOf(total)
        );

        tvActiveListings.setText(
                String.valueOf(active)
        );

        tvSoldListings.setText(
                String.valueOf(sold)
        );

        if (total == 0) {

            showEmptyState();

            return;
        }

        for (int i = 0;
             i < listings.length();
             i++) {

            try {

                JSONObject listing =
                        listings.getJSONObject(i);

                addListingCard(
                        listing
                );

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }


    private void showEmptyState() {

        LinearLayout emptyLayout =
                new LinearLayout(this);

        emptyLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        emptyLayout.setGravity(
                Gravity.CENTER
        );

        emptyLayout.setPadding(
                dp(25),
                dp(45),
                dp(25),
                dp(60)
        );

        TextView icon =
                new TextView(this);

        icon.setText("🛍");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);

        emptyLayout.addView(icon);

        TextView title =
                createTextView(
                        "No listings yet",
                        21,
                        NAVY,
                        true
                );

        title.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        titleParams.setMargins(
                0,
                dp(12),
                0,
                0
        );

        title.setLayoutParams(
                titleParams
        );

        emptyLayout.addView(title);

        TextView message =
                createTextView(
                        "You haven't posted anything yet.\n"
                                + "Start selling products or offering services "
                                + "to students on CampusLink.",
                        14,
                        TEXT_GRAY,
                        false
                );

        message.setGravity(
                Gravity.CENTER
        );

        message.setLineSpacing(
                2,
                1.1f
        );

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        messageParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        message.setLayoutParams(
                messageParams
        );

        emptyLayout.addView(
                message
        );

        Button postButton =
                new Button(this);

        postButton.setText(
                "＋  Post Your First Listing"
        );

        postButton.setTextSize(14);
        postButton.setTextColor(
                Color.WHITE
        );

        postButton.setAllCaps(false);

        GradientDrawable buttonBackground =
                new GradientDrawable();

        buttonBackground.setColor(
                GREEN
        );

        buttonBackground.setCornerRadius(
                dp(14)
        );

        postButton.setBackground(
                buttonBackground
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(52)
                );

        buttonParams.gravity =
                Gravity.CENTER;

        buttonParams.setMargins(
                0,
                dp(22),
                0,
                0
        );

        postButton.setLayoutParams(
                buttonParams
        );

        postButton.setOnClickListener(
                v -> {

                    Intent intent =
                            new Intent(
                                    MyListingsActivity.this,
                                    PostListingActivity.class
                            );

                    startActivity(intent);
                }
        );

        emptyLayout.addView(
                postButton
        );

        myListingsContainer.addView(
                emptyLayout
        );
    }


    private void addListingCard(
            JSONObject listing
    ) {

        String id =
                listing.optString(
                        "id",
                        ""
                );

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
                        "active"
                );

        String phone =
                listing.optString(
                        "phone",
                        ""
                );

        String condition =
                listing.optString(
                        "condition",
                        "Not specified"
                );

        String category =
                "Other";

        String categoryIcon =
                "📦";

        JSONObject categoryObject =
                listing.optJSONObject(
                        "categories"
                );

        if (categoryObject != null) {

            category =
                    categoryObject.optString(
                            "name",
                            "Other"
                    );

            categoryIcon =
                    categoryObject.optString(
                            "icon",
                            "📦"
                    );
        }

        String imageUrl = "";

        JSONArray imageUrls =
                listing.optJSONArray(
                        "image_urls"
                );

        if (imageUrls != null
                && imageUrls.length() > 0) {

            imageUrl =
                    imageUrls.optString(
                            0,
                            ""
                    );
        }

        if (imageUrl.isEmpty()) {

            imageUrl =
                    listing.optString(
                            "image_url",
                            ""
                    );
        }

        boolean isSold =
                status.equalsIgnoreCase(
                        "sold"
                );


        // =========================
        // CARD
        // =========================

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(
                Color.WHITE
        );

        cardBackground.setCornerRadius(
                dp(18)
        );

        cardBackground.setStroke(
                dp(1),
                BORDER
        );

        card.setBackground(
                cardBackground
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                dp(18)
        );

        card.setLayoutParams(
                cardParams
        );


        // =========================
        // IMAGE
        // =========================

        ImageView image =
                new ImageView(this);

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setBackgroundColor(
                LIGHT_GREEN
        );

        image.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(200)
                );

        image.setLayoutParams(
                imageParams
        );

        card.addView(
                image
        );

        if (!imageUrl.isEmpty()) {

            loadImage(
                    imageUrl,
                    image
            );
        }


        // =========================
        // TITLE
        // =========================

        TextView titleView =
                createTextView(
                        title,
                        19,
                        NAVY,
                        true
                );

        titleView.setMaxLines(2);

        titleView.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        titleParams.setMargins(
                0,
                dp(12),
                0,
                dp(3)
        );

        titleView.setLayoutParams(
                titleParams
        );

        card.addView(
                titleView
        );


        // =========================
        // PRICE
        // =========================

        TextView priceView =
                createTextView(
                        "KSh "
                                + formatPrice(price),
                        19,
                        GREEN,
                        true
                );

        card.addView(
                priceView
        );


        // =========================
        // CATEGORY
        // =========================

        TextView categoryView =
                createTextView(
                        categoryIcon
                                + " "
                                + category
                                + "   •   "
                                + condition,
                        14,
                        TEXT_GRAY,
                        false
                );

        LinearLayout.LayoutParams categoryParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        categoryParams.setMargins(
                0,
                dp(6),
                0,
                0
        );

        categoryView.setLayoutParams(
                categoryParams
        );

        card.addView(
                categoryView
        );


        // =========================
        // LOCATION
        // =========================

        TextView locationView =
                createTextView(
                        "📍 " + location,
                        14,
                        TEXT_GRAY,
                        false
                );

        locationView.setMaxLines(1);

        locationView.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        LinearLayout.LayoutParams locationParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        locationParams.setMargins(
                0,
                dp(5),
                0,
                dp(6)
        );

        locationView.setLayoutParams(
                locationParams
        );

        card.addView(
                locationView
        );


        // =========================
        // PHONE
        // =========================

        if (!phone.isEmpty()) {

            TextView phoneView =
                    createTextView(
                            "📞 " + phone,
                            13,
                            TEXT_GRAY,
                            false
                    );

            LinearLayout.LayoutParams phoneParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            phoneParams.setMargins(
                    0,
                    0,
                    0,
                    dp(6)
            );

            phoneView.setLayoutParams(
                    phoneParams
            );

            card.addView(
                    phoneView
            );
        }


        // =========================
        // STATUS BADGE
        // =========================

        TextView statusBadge =
                new TextView(this);

        statusBadge.setText(
                isSold
                        ? "  ● SOLD  "
                        : "  ● ACTIVE  "
        );

        statusBadge.setTextSize(
                12
        );

        statusBadge.setTypeface(
                null,
                Typeface.BOLD
        );

        statusBadge.setGravity(
                Gravity.CENTER
        );

        statusBadge.setTextColor(
                isSold
                        ? RED
                        : GREEN
        );

        GradientDrawable statusBackground =
                new GradientDrawable();

        statusBackground.setColor(
                isSold
                        ? Color.rgb(
                        255,
                        235,
                        238
                )
                        : LIGHT_GREEN
        );

        statusBackground.setCornerRadius(
                dp(30)
        );

        statusBadge.setBackground(
                statusBackground
        );

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(32)
                );

        statusParams.setMargins(
                0,
                dp(2),
                0,
                dp(12)
        );

        statusBadge.setLayoutParams(
                statusParams
        );

        card.addView(
                statusBadge
        );


        // =========================
        // BUTTON ROW
        // =========================

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        buttons.setGravity(
                Gravity.CENTER
        );


        // EDIT

        Button edit =
                createActionButton(
                        "Edit",
                        NAVY
                );

        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                );

        editParams.setMargins(
                0,
                0,
                dp(4),
                0
        );

        edit.setLayoutParams(
                editParams
        );

        edit.setOnClickListener(
                v -> showEditDialog(
                        listing
                )
        );

        buttons.addView(
                edit
        );


        // STATUS

        Button statusButton =
                createActionButton(
                        isSold
                                ? "Active"
                                : "Sold",
                        GREEN
                );

        LinearLayout.LayoutParams statusButtonParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                );

        statusButtonParams.setMargins(
                dp(4),
                0,
                dp(4),
                0
        );

        statusButton.setLayoutParams(
                statusButtonParams
        );

        statusButton.setOnClickListener(
                v -> {

                    String newStatus =
                            isSold
                                    ? "active"
                                    : "sold";

                    updateStatus(
                            id,
                            newStatus
                    );
                }
        );

        buttons.addView(
                statusButton
        );


        // DELETE

        Button delete =
                createActionButton(
                        "Delete",
                        RED
                );

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                );

        deleteParams.setMargins(
                dp(4),
                0,
                0,
                0
        );

        delete.setLayoutParams(
                deleteParams
        );

        delete.setOnClickListener(
                v -> confirmDelete(
                        id,
                        title
                )
        );

        buttons.addView(
                delete
        );

        card.addView(
                buttons
        );


        // =========================
        // CARD CLICK
        // =========================

        final String finalId = id;
        final String finalTitle = title;
        final String finalDescription = description;
        final String finalPrice = price;
        final String finalLocation = location;
        final String finalPhone = phone;
        final String finalStatus = status;
        final String finalCategory = category;
        final String finalImageUrl = imageUrl;

        card.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MyListingsActivity.this,
                            ListingDetailsActivity.class
                    );

            intent.putExtra(
                    "listing_id",
                    finalId
            );

            intent.putExtra(
                    "title",
                    finalTitle
            );

            intent.putExtra(
                    "description",
                    finalDescription
            );

            intent.putExtra(
                    "price",
                    finalPrice
            );

            intent.putExtra(
                    "location",
                    finalLocation
            );

            intent.putExtra(
                    "image_url",
                    finalImageUrl
            );

            intent.putExtra(
                    "phone",
                    finalPhone
            );

            intent.putExtra(
                    "status",
                    finalStatus
            );

            intent.putExtra(
                    "category",
                    finalCategory
            );

            startActivity(
                    intent
            );
        });


        myListingsContainer.addView(
                card
        );
    }


    private Button createActionButton(
            String text,
            int color
    ) {

        Button button =
                new Button(this);

        button.setText(
                text
        );

        button.setTextSize(
                12
        );

        button.setTextColor(
                color
        );

        button.setTypeface(
                null,
                Typeface.BOLD
        );

        button.setAllCaps(
                false
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.WHITE
        );

        background.setCornerRadius(
                dp(12)
        );

        background.setStroke(
                dp(1),
                color
        );

        button.setBackground(
                background
        );

        return button;
    }


    // =========================================================
    // EDIT LISTING
    // =========================================================

    private void showEditDialog(
            JSONObject listing
    ) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(22),
                dp(5),
                dp(22),
                dp(5)
        );


        // TITLE

        EditText title =
                new EditText(this);

        title.setHint(
                "Listing title"
        );

        title.setSingleLine(
                true
        );

        title.setText(
                listing.optString(
                        "title",
                        ""
                )
        );

        layout.addView(
                title
        );


        // DESCRIPTION

        EditText description =
                new EditText(this);

        description.setHint(
                "Description"
        );

        description.setText(
                listing.optString(
                        "description",
                        ""
                )
        );

        description.setMinLines(
                3
        );

        description.setGravity(
                Gravity.TOP
        );

        LinearLayout.LayoutParams descriptionParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(100)
                );

        descriptionParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        description.setLayoutParams(
                descriptionParams
        );

        layout.addView(
                description
        );


        // PRICE

        EditText price =
                new EditText(this);

        price.setHint(
                "Price in KSh"
        );

        price.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        price.setText(
                listing.optString(
                        "price",
                        ""
                )
        );

        layout.addView(
                price
        );


        // CATEGORY LABEL

        TextView categoryLabel =
                createTextView(
                        "Category",
                        13,
                        TEXT_GRAY,
                        true
                );

        LinearLayout.LayoutParams categoryLabelParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        categoryLabelParams.setMargins(
                0,
                dp(10),
                0,
                dp(3)
        );

        categoryLabel.setLayoutParams(
                categoryLabelParams
        );

        layout.addView(
                categoryLabel
        );


        // CATEGORY SPINNER

        Spinner categorySpinner =
                new Spinner(this);

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categoryNames
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        categorySpinner.setAdapter(
                categoryAdapter
        );

        long currentCategoryId =
                listing.optLong(
                        "category_id",
                        11
                );

        int categoryPosition =
                getCategoryPosition(
                        currentCategoryId
                );

        categorySpinner.setSelection(
                categoryPosition
        );

        layout.addView(
                categorySpinner
        );


        // CONDITION LABEL

        TextView conditionLabel =
                createTextView(
                        "Condition",
                        13,
                        TEXT_GRAY,
                        true
                );

        LinearLayout.LayoutParams conditionLabelParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        conditionLabelParams.setMargins(
                0,
                dp(10),
                0,
                dp(3)
        );

        conditionLabel.setLayoutParams(
                conditionLabelParams
        );

        layout.addView(
                conditionLabel
        );


        // CONDITION SPINNER

        Spinner conditionSpinner =
                new Spinner(this);

        ArrayAdapter<String> conditionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        conditions
                );

        conditionAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        conditionSpinner.setAdapter(
                conditionAdapter
        );

        String currentCondition =
                listing.optString(
                        "condition",
                        "Not Applicable"
                );

        int conditionPosition =
                getConditionPosition(
                        currentCondition
                );

        conditionSpinner.setSelection(
                conditionPosition
        );

        layout.addView(
                conditionSpinner
        );


        // LOCATION

        EditText location =
                new EditText(this);

        location.setHint(
                "Location"
        );

        location.setSingleLine(
                true
        );

        location.setText(
                listing.optString(
                        "location",
                        ""
                )
        );

        LinearLayout.LayoutParams locationParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        locationParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        location.setLayoutParams(
                locationParams
        );

        layout.addView(
                location
        );


        // PHONE

        EditText phone =
                new EditText(this);

        phone.setHint(
                "Phone number"
        );

        phone.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        phone.setSingleLine(
                true
        );

        phone.setText(
                listing.optString(
                        "phone",
                        ""
                )
        );

        LinearLayout.LayoutParams phoneParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        phoneParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        phone.setLayoutParams(
                phoneParams
        );

        layout.addView(
                phone
        );


        // DIALOG

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Edit Listing"
                        )
                        .setView(
                                layout
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Save Changes",
                                null
                        )
                        .create();


        dialog.setOnShowListener(
                d -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                String newTitle =
                                        title.getText()
                                                .toString()
                                                .trim();

                                String newDescription =
                                        description.getText()
                                                .toString()
                                                .trim();

                                String newPrice =
                                        price.getText()
                                                .toString()
                                                .trim();

                                String newLocation =
                                        location.getText()
                                                .toString()
                                                .trim();

                                String newPhone =
                                        phone.getText()
                                                .toString()
                                                .trim();

                                String newCategory =
                                        categorySpinner
                                                .getSelectedItem()
                                                .toString();

                                int selectedCategoryPosition =
                                        categorySpinner
                                                .getSelectedItemPosition();

                                long newCategoryId =
                                        categoryIds[
                                                selectedCategoryPosition
                                                ];

                                String newCondition =
                                        conditionSpinner
                                                .getSelectedItem()
                                                .toString();


                                // Validation

                                if (newTitle.isEmpty()) {

                                    title.setError(
                                            "Enter a title"
                                    );

                                    title.requestFocus();

                                    return;
                                }


                                if (newDescription.isEmpty()) {

                                    description.setError(
                                            "Enter a description"
                                    );

                                    description.requestFocus();

                                    return;
                                }


                                if (newPrice.isEmpty()) {

                                    price.setError(
                                            "Enter a price"
                                    );

                                    price.requestFocus();

                                    return;
                                }


                                try {

                                    Double.parseDouble(
                                            newPrice.replace(",", "").trim()
                                    );

                                } catch (Exception e) {

                                    price.setError(
                                            "Enter a valid price"
                                    );

                                    price.requestFocus();

                                    return;
                                }


                                if (newLocation.isEmpty()) {

                                    location.setError(
                                            "Enter a location"
                                    );

                                    location.requestFocus();

                                    return;
                                }


                                if (newPhone.isEmpty()) {

                                    phone.setError(
                                            "Enter a phone number"
                                    );

                                    phone.requestFocus();

                                    return;
                                }


                                updateListing(
                                        listing.optString(
                                                "id",
                                                ""
                                        ),
                                        newTitle,
                                        newDescription,
                                        newPrice,
                                        newCategoryId,
                                        newCondition,
                                        newLocation,
                                        newPhone
                                );

                                dialog.dismiss();
                            }
                    );
                }
        );

        dialog.show();
    }


    private int getCategoryPosition(
            long categoryId
    ) {

        for (int i = 0;
             i < categoryIds.length;
             i++) {

            if (categoryIds[i] ==
                    categoryId) {

                return i;
            }
        }

        return categoryIds.length - 1;
    }


    private int getConditionPosition(
            String condition
    ) {

        for (int i = 0;
             i < conditions.length;
             i++) {

            if (conditions[i]
                    .equalsIgnoreCase(
                            condition
                    )) {

                return i;
            }
        }

        return conditions.length - 1;
    }


    // =========================================================
    // UPDATE LISTING
    // =========================================================

    private void updateListing(
            String listingId,
            String title,
            String description,
            String price,
            long categoryId,
            String condition,
            String location,
            String phone
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "title",
                    title
            );

            json.put(
                    "description",
                    description
            );

            json.put(
                    "price",
                    Double.parseDouble(
                            price
                    )
            );

            json.put(
                    "category_id",
                    categoryId
            );

            json.put(
                    "condition",
                    condition
            );

            json.put(
                    "location",
                    location
            );

            json.put(
                    "phone",
                    phone
            );


            String url =
                    SupabaseConfig.SUPABASE_URL
                            + "/rest/v1/listings"
                            + "?id=eq."
                            + listingId
                            + "&user_id=eq."
                            + userId;


            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );


            Request request =
                    new Request.Builder()
                            .url(url)
                            .addHeader(
                                    "apikey",
                                    SupabaseConfig.SUPABASE_KEY
                            )
                            .addHeader(
                                    "Authorization",
                                    "Bearer "
                                            + accessToken
                            )
                            .addHeader(
                                    "Content-Type",
                                    "application/json"
                            )
                            .addHeader(
                                    "Prefer",
                                    "return=minimal"
                            )
                            .patch(body)
                            .build();


            client.newCall(request)
                    .enqueue(
                            new okhttp3.Callback() {

                                @Override
                                public void onFailure(
                                        okhttp3.Call call,
                                        IOException e
                                ) {

                                    runOnUiThread(() ->
                                            Toast.makeText(
                                                    MyListingsActivity.this,
                                                    "Update failed. Check your internet connection.",
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                                }


                                @Override
                                public void onResponse(
                                        okhttp3.Call call,
                                        Response response
                                ) throws IOException {

                                    String responseBody =
                                            response.body() != null
                                                    ? response.body().string()
                                                    : "";

                                    if (response.isSuccessful()) {

                                        runOnUiThread(() -> {

                                            Toast.makeText(
                                                    MyListingsActivity.this,
                                                    "Listing updated successfully.",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            loadMyListings();
                                        });

                                    } else {

                                        runOnUiThread(() ->
                                                Toast.makeText(
                                                        MyListingsActivity.this,
                                                        "Could not update listing.\nError "
                                                                + response.code()
                                                                + "\n"
                                                                + responseBody,
                                                        Toast.LENGTH_LONG
                                                ).show()
                                        );
                                    }
                                }
                            }
                    );

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Unable to update listing.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =========================================================
    // UPDATE STATUS
    // =========================================================

    private void updateStatus(
            String listingId,
            String newStatus
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "status",
                    newStatus
            );


            String url =
                    SupabaseConfig.SUPABASE_URL
                            + "/rest/v1/listings"
                            + "?id=eq."
                            + listingId
                            + "&user_id=eq."
                            + userId;


            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            JSON
                    );


            Request request =
                    new Request.Builder()
                            .url(url)
                            .addHeader(
                                    "apikey",
                                    SupabaseConfig.SUPABASE_KEY
                            )
                            .addHeader(
                                    "Authorization",
                                    "Bearer "
                                            + accessToken
                            )
                            .addHeader(
                                    "Content-Type",
                                    "application/json"
                            )
                            .addHeader(
                                    "Prefer",
                                    "return=minimal"
                            )
                            .patch(body)
                            .build();


            client.newCall(request)
                    .enqueue(
                            new okhttp3.Callback() {

                                @Override
                                public void onFailure(
                                        okhttp3.Call call,
                                        IOException e
                                ) {

                                    runOnUiThread(() ->
                                            Toast.makeText(
                                                    MyListingsActivity.this,
                                                    "Status update failed.",
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                                }


                                @Override
                                public void onResponse(
                                        okhttp3.Call call,
                                        Response response
                                ) throws IOException {

                                    String responseBody =
                                            response.body() != null
                                                    ? response.body().string()
                                                    : "";

                                    if (response.isSuccessful()) {

                                        runOnUiThread(() -> {

                                            Toast.makeText(
                                                    MyListingsActivity.this,
                                                    newStatus.equals(
                                                            "sold"
                                                    )
                                                            ? "Listing marked as sold."
                                                            : "Listing marked as active.",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            loadMyListings();
                                        });

                                    } else {

                                        runOnUiThread(() ->
                                                Toast.makeText(
                                                        MyListingsActivity.this,
                                                        "Could not change listing status.\n"
                                                                + "Error "
                                                                + response.code()
                                                                + "\n"
                                                                + responseBody,
                                                        Toast.LENGTH_LONG
                                                ).show()
                                        );
                                    }
                                }
                            }
                    );

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Unable to change listing status.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    private void confirmDelete(
            String listingId,
            String title
    ) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Delete Listing?"
                        )
                        .setMessage(
                                "Are you sure you want to permanently delete:\n\n"
                                        + title
                                        + "\n\n"
                                        + "This action cannot be undone."
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Delete",
                                null
                        )
                        .create();


        dialog.setOnShowListener(
                d -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setTextColor(
                            RED
                    );

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                deleteListing(
                                        listingId
                                );

                                dialog.dismiss();
                            }
                    );
                }
        );

        dialog.show();
    }


    private void deleteListing(
            String listingId
    ) {

        String url =
                SupabaseConfig.SUPABASE_URL
                        + "/rest/v1/listings"
                        + "?id=eq."
                        + listingId
                        + "&user_id=eq."
                        + userId;


        Request request =
                new Request.Builder()
                        .url(url)
                        .addHeader(
                                "apikey",
                                SupabaseConfig.SUPABASE_KEY
                        )
                        .addHeader(
                                "Authorization",
                                "Bearer "
                                        + accessToken
                        )
                        .addHeader(
                                "Accept",
                                "application/json"
                        )
                        .delete()
                        .build();


        client.newCall(request)
                .enqueue(
                        new okhttp3.Callback() {

                            @Override
                            public void onFailure(
                                    okhttp3.Call call,
                                    IOException e
                            ) {

                                runOnUiThread(() ->
                                        Toast.makeText(
                                                MyListingsActivity.this,
                                                "Delete failed. Check your internet connection.",
                                                Toast.LENGTH_LONG
                                        ).show()
                                );
                            }


                            @Override
                            public void onResponse(
                                    okhttp3.Call call,
                                    Response response
                            ) throws IOException {

                                String responseBody =
                                        response.body() != null
                                                ? response.body().string()
                                                : "";

                                if (response.isSuccessful()) {

                                    runOnUiThread(() -> {

                                        Toast.makeText(
                                                MyListingsActivity.this,
                                                "Listing deleted successfully.",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        loadMyListings();
                                    });

                                } else {

                                    runOnUiThread(() ->
                                            Toast.makeText(
                                                    MyListingsActivity.this,
                                                    "Could not delete listing.\n"
                                                            + "Error "
                                                            + response.code()
                                                            + "\n"
                                                            + responseBody,
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                                }
                            }
                        }
                );
    }


    // =========================================================
    // IMAGE LOADING
    // =========================================================

    private void loadImage(
            String imageUrl,
            ImageView imageView
    ) {

        new Thread(() -> {

            HttpURLConnection connection =
                    null;

            InputStream input =
                    null;

            try {

                URL url =
                        new URL(
                                imageUrl
                        );

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setConnectTimeout(
                        10000
                );

                connection.setReadTimeout(
                        10000
                );

                connection.setDoInput(
                        true
                );

                connection.connect();

                input =
                        connection.getInputStream();

                Bitmap bitmap =
                        BitmapFactory.decodeStream(
                                input
                        );

                runOnUiThread(() -> {

                    if (bitmap != null) {

                        imageView.setImageBitmap(
                                bitmap
                        );
                    }
                });

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                try {

                    if (input != null) {
                        input.close();
                    }

                } catch (Exception ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private TextView createTextView(
            String text,
            int size,
            int color,
            boolean bold
    ) {

        TextView view =
                new TextView(this);

        view.setText(
                text
        );

        view.setTextSize(
                size
        );

        view.setTextColor(
                color
        );

        if (bold) {

            view.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }

        return view;
    }


    private String formatPrice(
            String price
    ) {

        try {

            double value =
                    Double.parseDouble(
                            price
                    );

            if (value ==
                    Math.floor(value)) {

                return String.format(
                        Locale.US,
                        "%,.0f",
                        value
                );
            }

            return String.format(
                    Locale.US,
                    "%,.2f",
                    value
            );

        } catch (Exception e) {

            return price;
        }
    }


    private int dp(
            int value
    ) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}