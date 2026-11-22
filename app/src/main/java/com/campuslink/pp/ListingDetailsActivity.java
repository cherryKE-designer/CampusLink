
package com.campuslink.pp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListingDetailsActivity extends AppCompatActivity {

    private static final String TAG = "CampusLinkMessaging";

    private TextView btnBack, btnShare;
    private ImageView listingImage;
    private TextView imageCounter, btnPreviousImage, btnNextImage;

    private TextView listingCategory, listingTitle, listingPrice;
    private TextView listingLocation, listingSeller, listingPhone;
    private TextView listingStatus, listingDescription;

    private Button btnMessageSeller, btnWhatsApp, btnCall;

    private final List<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;

    private String listingId = "";
    private String sellerId = "";
    private String buyerId = "";

    private String phone = "";
    private String title = "";
    private String description = "";
    private String price = "";
    private String location = "";
    private String sellerName = "";
    private String category = "Other";
    private String status = "active";

    private boolean openingConversation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_details);

        initializeViews();
        readListingData();
        loadSession();
        displayListing();
        setupButtons();
        setupImageGallery();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);

        listingImage = findViewById(R.id.listingImage);
        imageCounter = findViewById(R.id.imageCounter);
        btnPreviousImage = findViewById(R.id.btnPreviousImage);
        btnNextImage = findViewById(R.id.btnNextImage);

        listingCategory = findViewById(R.id.listingCategory);
        listingTitle = findViewById(R.id.listingTitle);
        listingPrice = findViewById(R.id.listingPrice);
        listingLocation = findViewById(R.id.listingLocation);
        listingSeller = findViewById(R.id.listingSeller);
        listingPhone = findViewById(R.id.listingPhone);
        listingStatus = findViewById(R.id.listingStatus);
        listingDescription = findViewById(R.id.listingDescription);

        btnMessageSeller = findViewById(R.id.btnMessageSeller);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);
        btnCall = findViewById(R.id.btnCall);
    }

    /*
     * Supports both String and numeric Intent extras.
     * This prevents a numeric listing ID from being lost
     * when the caller uses putExtra(String, long).
     */
    private String getExtra(Intent intent, String key) {
        if (intent == null || !intent.hasExtra(key)) {
            return "";
        }

        Object value = intent.getExtras() != null
                ? intent.getExtras().get(key)
                : null;

        return value == null ? "" : String.valueOf(value).trim();
    }

    private void readListingData() {
        Intent intent = getIntent();

        listingId = getExtra(intent, "listing_id");
        if (listingId.isEmpty()) {
            listingId = getExtra(intent, "id");
        }

        sellerId = getExtra(intent, "seller_id");
        if (sellerId.isEmpty()) {
            sellerId = getExtra(intent, "user_id");
        }

        title = getExtra(intent, "title");
        if (title.isEmpty()) {
            title = getExtra(intent, "listing_title");
        }

        description = getExtra(intent, "description");
        price = getExtra(intent, "price");
        location = getExtra(intent, "location");
        phone = getExtra(intent, "phone");
        sellerName = getExtra(intent, "seller_name");
        category = getExtra(intent, "category");
        status = getExtra(intent, "status");

        if (title.isEmpty()) title = "Untitled Listing";
        if (description.isEmpty()) description = "No description provided.";
        if (price.isEmpty()) price = "0";
        if (location.isEmpty()) location = "Location not provided";
        if (sellerName.isEmpty()) sellerName = "CampusLink Seller";
        if (category.isEmpty()) category = "Other";
        if (status.isEmpty()) status = "active";

        addImage(getExtra(intent, "image_url"));

        String[] multipleImages = intent.getStringArrayExtra("image_urls");
        if (multipleImages != null) {
            for (String image : multipleImages) {
                addImage(image);
            }
        }

        String imagesJson = getExtra(intent, "image_urls_json");
        if (!imagesJson.isEmpty()) {
            try {
                JSONArray array = new JSONArray(imagesJson);

                for (int i = 0; i < array.length(); i++) {
                    addImage(array.optString(i, ""));
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not parse listing image URLs", e);
            }
        }

        Log.d(TAG, "Listing ID: " + listingId);
        Log.d(TAG, "Seller ID received: " + sellerId);
    }

    private void addImage(String image) {
        if (image != null && !image.trim().isEmpty()
                && !imageUrls.contains(image.trim())) {
            imageUrls.add(image.trim());
        }
    }

    private void loadSession() {
        SharedPreferences preferences = getSharedPreferences(
                MessagingApi.PREFS,
                MODE_PRIVATE
        );

        buyerId = preferences.getString(MessagingApi.USER_ID, "");

        if (buyerId == null) {
            buyerId = "";
        }

        buyerId = buyerId.trim();

        Log.d(TAG, "Current user ID loaded: "
                + (buyerId.isEmpty() ? "EMPTY" : buyerId));
    }

    private void displayListing() {
        listingTitle.setText(title);
        listingDescription.setText(description);
        listingPrice.setText(formatPrice(price));
        listingLocation.setText("📍 " + location);
        listingSeller.setText("👤 Posted by: " + sellerName);
        listingCategory.setText(getCategoryIcon(category) + " " + category);
        listingStatus.setText("● " + capitalize(status));

        listingPhone.setText(phone.isEmpty()
                ? "📱 Phone: Not provided"
                : "📱 Phone: " + phone);

        btnWhatsApp.setEnabled(!phone.isEmpty());
        btnCall.setEnabled(!phone.isEmpty());

        btnWhatsApp.setAlpha(phone.isEmpty() ? 0.5f : 1f);
        btnCall.setAlpha(phone.isEmpty() ? 0.5f : 1f);

        /*
         * IMPORTANT:
         * Do not disable Message Seller because an ID is missing.
         * Keep it clickable so the user receives an explanation.
         */
        btnMessageSeller.setEnabled(true);
        btnMessageSeller.setClickable(true);
        btnMessageSeller.setAlpha(1f);

        if (buyerId.isEmpty()) {
            btnMessageSeller.setText("Sign in to Message Seller");
        } else if (!sellerId.isEmpty()
                && buyerId.equalsIgnoreCase(sellerId)) {
            btnMessageSeller.setText("This Is Your Listing");
        } else {
            btnMessageSeller.setText("✉ Message Seller");
        }
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());

        btnShare.setOnClickListener(v -> shareListing());

        btnCall.setOnClickListener(v -> callSeller());

        btnWhatsApp.setOnClickListener(v -> contactWhatsApp());

        btnMessageSeller.setOnClickListener(v -> {
            Log.d(TAG, "Message Seller button clicked");

            Toast.makeText(
                    ListingDetailsActivity.this,
                    "Opening seller messages...",
                    Toast.LENGTH_SHORT
            ).show();

            openSellerMessages();
        });
    }

    private void openSellerMessages() {
        if (openingConversation) {
            return;
        }

        if (buyerId.isEmpty()) {
            showMessage(
                    "You are not signed in. Please sign in, then reopen this listing."
            );
            return;
        }

        if (sellerId.isEmpty()) {
            showMessage(
                    "Seller ID is missing. Check seller_id in the listing data."
            );
            Log.e(TAG, "Cannot message seller: sellerId is empty");
            return;
        }

        if (buyerId.equalsIgnoreCase(sellerId)) {
            showMessage("You cannot message yourself.");
            return;
        }

        if (listingId.isEmpty() || !listingId.matches("\\d+")) {
            showMessage(
                    "Listing ID is missing or invalid. Check listing_id in the listing data."
            );
            Log.e(TAG, "Invalid listing ID: " + listingId);
            return;
        }

        openingConversation = true;
        setMessageButtonBusy(true);

        String path = "/rest/v1/conversations"
                + "?select=*"
                + "&listing_id=eq." + MessagingApi.enc(listingId)
                + "&buyer_id=eq." + MessagingApi.enc(buyerId)
                + "&seller_id=eq." + MessagingApi.enc(sellerId)
                + "&limit=1";

        Log.d(TAG, "Looking for conversation: " + path);

        MessagingApi.get(this, path, (ok, code, body, error) ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!ok) {
                        finishConversationRequest();

                        logMessagingError(
                                "Find conversation",
                                code,
                                body,
                                error
                        );

                        showMessage(
                                "Could not find conversation. HTTP "
                                        + code + ". Check Logcat."
                        );
                        return;
                    }

                    try {
                        JSONArray rows = new JSONArray(body);

                        if (rows.length() > 0) {
                            openChatFromConversation(
                                    rows.getJSONObject(0)
                            );
                        } else {
                            createSellerConversation();
                        }

                    } catch (Exception e) {
                        finishConversationRequest();

                        Log.e(
                                TAG,
                                "Invalid conversation response: " + body,
                                e
                        );

                        showMessage(
                                "Could not process the conversation response."
                        );
                    }
                })
        );
    }

    private void createSellerConversation() {
        try {
            JSONObject payload = new JSONObject();

            payload.put("listing_id", Long.parseLong(listingId));
            payload.put("buyer_id", buyerId);
            payload.put("seller_id", sellerId);
            payload.put("buyer_name", "CampusLink User");
            payload.put("seller_name", sellerName);
            payload.put("listing_title", title);
            payload.put("last_message", "");

            Log.d(TAG, "Creating conversation for listing " + listingId);

            MessagingApi.post(
                    this,
                    "/rest/v1/conversations",
                    payload,
                    (ok, code, body, error) ->
                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                if (!ok) {
                                    finishConversationRequest();

                                    logMessagingError(
                                            "Create conversation",
                                            code,
                                            body,
                                            error
                                    );

                                    showMessage(
                                            "Could not create conversation. HTTP "
                                                    + code + ". Check Logcat."
                                    );
                                    return;
                                }

                                try {
                                    JSONArray rows = new JSONArray(body);

                                    if (rows.length() == 0) {
                                        finishConversationRequest();

                                        showMessage(
                                                "Supabase returned no conversation. "
                                                        + "Check your INSERT policy."
                                        );
                                        return;
                                    }

                                    openChatFromConversation(
                                            rows.getJSONObject(0)
                                    );

                                } catch (Exception e) {
                                    finishConversationRequest();

                                    Log.e(
                                            TAG,
                                            "Invalid conversation creation response: "
                                                    + body,
                                            e
                                    );

                                    showMessage(
                                            "Could not read the new conversation."
                                    );
                                }
                            })
            );

        } catch (Exception e) {
            finishConversationRequest();

            Log.e(TAG, "Could not prepare conversation payload", e);
            showMessage("Could not prepare the conversation.");
        }
    }

    private void openChatFromConversation(JSONObject conversation) {
        String conversationId = conversation.optString("id", "");

        if (conversationId.isEmpty()) {
            finishConversationRequest();
            showMessage("Conversation ID is missing.");
            return;
        }

        String conversationSellerId =
                conversation.optString("seller_id", sellerId);

        String otherName;

        if (buyerId.equalsIgnoreCase(conversationSellerId)) {
            otherName = conversation.optString(
                    "buyer_name",
                    "CampusLink User"
            );
        } else {
            otherName = conversation.optString(
                    "seller_name",
                    sellerName
            );
        }

        String conversationTitle = conversation.optString(
                "listing_title",
                title
        );

        Intent intent = new Intent(
                ListingDetailsActivity.this,
                ChatActivity.class
        );

        intent.putExtra("conversation_id", conversationId);
        intent.putExtra("other_name", otherName);
        intent.putExtra("listing_title", conversationTitle);

        finishConversationRequest();

        Log.d(TAG, "Opening ChatActivity for " + conversationId);
        startActivity(intent);
    }

    private void finishConversationRequest() {
        openingConversation = false;
        setMessageButtonBusy(false);
    }

    private void setMessageButtonBusy(boolean busy) {
        btnMessageSeller.setEnabled(true);
        btnMessageSeller.setClickable(true);

        if (busy) {
            btnMessageSeller.setText("Opening...");
            btnMessageSeller.setAlpha(0.7f);
        } else {
            btnMessageSeller.setAlpha(1f);

            if (buyerId.isEmpty()) {
                btnMessageSeller.setText("Sign in to Message Seller");
            } else if (!sellerId.isEmpty()
                    && buyerId.equalsIgnoreCase(sellerId)) {
                btnMessageSeller.setText("This Is Your Listing");
            } else {
                btnMessageSeller.setText("✉ Message Seller");
            }
        }
    }

    private void logMessagingError(
            String operation,
            int code,
            String body,
            Exception error
    ) {
        Log.e(
                TAG,
                operation + " failed. HTTP " + code
                        + ". Response: " + body,
                error
        );
    }

    private void setupImageGallery() {
        if (imageUrls.isEmpty()) {
            listingImage.setImageResource(
                    android.R.drawable.ic_menu_gallery
            );

            imageCounter.setText("No images");
            btnPreviousImage.setVisibility(View.GONE);
            btnNextImage.setVisibility(View.GONE);
            imageCounter.setVisibility(View.GONE);
            return;
        }

        updateImage();

        btnPreviousImage.setOnClickListener(v -> {
            if (imageUrls.size() <= 1) return;

            currentImageIndex =
                    (currentImageIndex - 1 + imageUrls.size())
                            % imageUrls.size();

            updateImage();
        });

        btnNextImage.setOnClickListener(v -> {
            if (imageUrls.size() <= 1) return;

            currentImageIndex =
                    (currentImageIndex + 1) % imageUrls.size();

            updateImage();
        });

        boolean multiple = imageUrls.size() > 1;

        btnPreviousImage.setVisibility(
                multiple ? View.VISIBLE : View.GONE
        );
        btnNextImage.setVisibility(
                multiple ? View.VISIBLE : View.GONE
        );
        imageCounter.setVisibility(
                multiple ? View.VISIBLE : View.GONE
        );
    }

    private void updateImage() {
        if (imageUrls.isEmpty()) return;

        imageCounter.setText(
                (currentImageIndex + 1) + " / " + imageUrls.size()
        );

        loadImage(imageUrls.get(currentImageIndex));
    }

    private void loadImage(String imageUrl) {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);

                Bitmap bitmap;

                try (InputStream stream = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(stream);
                }

                Bitmap finalBitmap = bitmap;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (finalBitmap != null) {
                        listingImage.setImageBitmap(finalBitmap);
                    } else {
                        listingImage.setImageResource(
                                android.R.drawable.ic_menu_gallery
                        );
                    }
                });

            } catch (Exception e) {
                Log.w(TAG, "Could not load listing image", e);

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        listingImage.setImageResource(
                                android.R.drawable.ic_menu_gallery
                        );
                    }
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void callSeller() {
        if (phone.isEmpty()) {
            showMessage("Seller phone number is not available.");
            return;
        }

        try {
            Intent intent = new Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:" + phone.trim())
            );

            startActivity(intent);

        } catch (Exception e) {
            showMessage("Unable to open the phone dialer.");
        }
    }

    private void contactWhatsApp() {
        if (phone.isEmpty()) {
            showMessage("Seller phone number is not available.");
            return;
        }

        String number = normalizeKenyanPhone(phone);

        if (number.isEmpty()) {
            showMessage("The seller's phone number is invalid.");
            return;
        }

        String message = "Hello " + sellerName
                + ", I found your listing on CampusLink.\n\n"
                + "Item: " + title + "\n"
                + "Price: " + formatPrice(price) + "\n"
                + "Location: " + location + "\n\n"
                + "Is it still available?";

        String url = "https://wa.me/" + number
                + "?text=" + Uri.encode(message);

        try {
            startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(url))
            );
        } catch (Exception e) {
            showMessage("Unable to open WhatsApp.");
        }
    }

    private String normalizeKenyanPhone(String number) {
        String clean = number.replaceAll("[^0-9+]", "");

        if (clean.startsWith("+")) {
            clean = clean.substring(1);
        }

        if (clean.startsWith("0")) {
            clean = "254" + clean.substring(1);
        } else if (clean.startsWith("7") && clean.length() == 9) {
            clean = "254" + clean;
        }

        return clean;
    }

    private void shareListing() {
        String shareText = "CampusLink Listing\n\n"
                + title + "\n"
                + formatPrice(price) + "\n"
                + "📍 " + location + "\n"
                + "📦 " + category + "\n"
                + "👤 Seller: " + sellerName + "\n\n"
                + description;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(
                Intent.createChooser(intent, "Share CampusLink Listing")
        );
    }

    private String formatPrice(String value) {
        try {
            double amount = Double.parseDouble(
                    value.replace(",", "").trim()
            );

            NumberFormat formatter =
                    NumberFormat.getNumberInstance(Locale.US);

            formatter.setMaximumFractionDigits(2);
            formatter.setMinimumFractionDigits(0);

            return "KSh " + formatter.format(amount);

        } catch (Exception e) {
            return "KSh " + (value.isEmpty() ? "0" : value);
        }
    }

    private String capitalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Active";
        }

        String clean = value.trim().toLowerCase(Locale.ROOT);

        return clean.substring(0, 1).toUpperCase(Locale.ROOT)
                + clean.substring(1);
    }

    private String getCategoryIcon(String value) {
        if (value == null) return "📦";

        switch (value) {
            case "Electronics": return "💻";
            case "Phones": return "📱";
            case "Books": return "📚";
            case "Clothing": return "👕";
            case "Furniture": return "🪑";
            case "Food": return "🍔";
            case "Services": return "🔧";
            case "Jobs & Gigs": return "💼";
            case "Accommodation": return "🏠";
            case "Vehicles": return "🚗";
            default: return "📦";
        }
    }

    private void showMessage(String message) {
        Toast.makeText(
                ListingDetailsActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
