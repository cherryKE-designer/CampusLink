package com.campuslink.pp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostListingActivity extends AppCompatActivity {

    private LinearLayout photoContainer;
    private TextView photoCount;

    private TextInputEditText inputTitle;
    private TextInputEditText inputDescription;
    private TextInputEditText inputPrice;
    private TextInputEditText inputLocation;

    private MaterialAutoCompleteTextView inputCategory;

    private MaterialButton btnSubmitListing;

    private final ArrayList<Uri> selectedImages = new ArrayList<>();

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private final OkHttpClient client = new OkHttpClient();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private static final String PREFS_NAME = "CampusLinkSession";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_ID = "user_id";

    private static final String BUCKET = "listing-images";

    private static final int MAX_IMAGES = 5;

    /*
     * Categories must match the Supabase categories table.
     *
     * ID 1  = Electronics
     * ID 2  = Phones
     * ID 3  = Books
     * ID 4  = Clothing
     * ID 5  = Furniture
     * ID 6  = Food
     * ID 7  = Services
     * ID 8  = Jobs & Gigs
     * ID 9  = Accommodation
     * ID 10 = Vehicles
     * ID 11 = Other
     */
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

    // Stores the actual selected Supabase category ID
    private long selectedCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_post_listing);

        initializeViews();
        setupCategoryDropdown();
        setupImagePicker();
        setupButtons();

        displaySelectedImages();
    }

    private void initializeViews() {

        photoContainer = findViewById(R.id.photoContainer);
        photoCount = findViewById(R.id.photoCount);

        inputTitle = findViewById(R.id.inputTitle);
        inputDescription = findViewById(R.id.inputDescription);
        inputPrice = findViewById(R.id.inputPrice);
        inputLocation = findViewById(R.id.inputLocation);

        inputCategory = findViewById(R.id.inputCategory);

        btnSubmitListing = findViewById(R.id.btnSubmitListing);
    }

    private void setupCategoryDropdown() {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        categoryNames
                );

        inputCategory.setAdapter(adapter);

        inputCategory.setOnClickListener(v ->
                inputCategory.showDropDown()
        );

        inputCategory.setOnItemClickListener(
                (parent, view, position, id) -> {

                    selectedCategoryId =
                            categoryIds[position];

                    inputCategory.setText(
                            categoryNames[position],
                            false
                    );
                }
        );
    }

    private void setupImagePicker() {

        imagePickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode() != RESULT_OK
                                    || result.getData() == null) {
                                return;
                            }

                            Intent data = result.getData();

                            selectedImages.clear();

                            if (data.getClipData() != null) {

                                int count =
                                        Math.min(
                                                data.getClipData().getItemCount(),
                                                MAX_IMAGES
                                        );

                                for (int i = 0; i < count; i++) {

                                    Uri imageUri =
                                            data.getClipData()
                                                    .getItemAt(i)
                                                    .getUri();

                                    selectedImages.add(imageUri);
                                }

                            } else if (data.getData() != null) {

                                selectedImages.add(
                                        data.getData()
                                );
                            }

                            displaySelectedImages();
                        }
                );
    }

    private void openImagePicker() {

        if (selectedImages.size() >= MAX_IMAGES) {

            Toast.makeText(
                    this,
                    "You can select up to 5 photos.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("image/*");

        intent.putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                true
        );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        imagePickerLauncher.launch(intent);
    }

    private void displaySelectedImages() {

        photoContainer.removeAllViews();

        for (Uri uri : selectedImages) {

            ImageView imageView =
                    new ImageView(this);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            dpToPx(105),
                            dpToPx(105)
                    );

            params.setMargins(
                    0,
                    0,
                    dpToPx(10),
                    0
            );

            imageView.setLayoutParams(params);

            imageView.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            imageView.setImageURI(uri);

            imageView.setBackgroundResource(
                    R.drawable.bg_search
            );

            photoContainer.addView(imageView);
        }

        // Add photos button
        LinearLayout addButton =
                new LinearLayout(this);

        addButton.setOrientation(
                LinearLayout.VERTICAL
        );

        addButton.setGravity(
                android.view.Gravity.CENTER
        );

        addButton.setClickable(true);
        addButton.setFocusable(true);

        addButton.setBackgroundResource(
                R.drawable.bg_search
        );

        LinearLayout.LayoutParams addParams =
                new LinearLayout.LayoutParams(
                        dpToPx(105),
                        dpToPx(105)
                );

        addButton.setLayoutParams(addParams);

        TextView plus =
                new TextView(this);

        plus.setText("+");
        plus.setTextColor(
                android.graphics.Color.rgb(
                        13,
                        184,
                        71
                )
        );
        plus.setTextSize(35);
        plus.setGravity(
                android.view.Gravity.CENTER
        );

        TextView text =
                new TextView(this);

        text.setText("Add Photos");
        text.setTextColor(
                android.graphics.Color.rgb(
                        7,
                        19,
                        63
                )
        );
        text.setTextSize(12);
        text.setGravity(
                android.view.Gravity.CENTER
        );

        addButton.addView(plus);
        addButton.addView(text);

        addButton.setOnClickListener(
                v -> openImagePicker()
        );

        photoContainer.addView(addButton);

        if (selectedImages.isEmpty()) {

            photoCount.setText(
                    "No photos selected"
            );

        } else {

            photoCount.setText(
                    selectedImages.size()
                            + " / "
                            + MAX_IMAGES
                            + " photos selected"
            );
        }
    }

    private void setupButtons() {

        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());

        findViewById(R.id.btnAddPhotos)
                .setOnClickListener(
                        v -> openImagePicker()
                );

        btnSubmitListing.setOnClickListener(
                v -> validateListing()
        );
    }

    private void validateListing() {

        String category =
                getText(inputCategory);

        String title =
                getText(inputTitle);

        String description =
                getText(inputDescription);

        String price =
                getText(inputPrice);

        String location =
                getText(inputLocation);

        // CATEGORY
        if (category.isEmpty()
                || selectedCategoryId == -1) {

            inputCategory.setError(
                    "Please select a category"
            );

            inputCategory.requestFocus();

            Toast.makeText(
                    this,
                    "Please select a listing category.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // TITLE
        if (title.isEmpty()) {

            inputTitle.setError(
                    "Enter a product or service name"
            );

            inputTitle.requestFocus();

            return;
        }

        if (title.length() < 3) {

            inputTitle.setError(
                    "Name must be at least 3 characters"
            );

            inputTitle.requestFocus();

            return;
        }

        // DESCRIPTION
        if (description.isEmpty()) {

            inputDescription.setError(
                    "Enter a description"
            );

            inputDescription.requestFocus();

            return;
        }

        if (description.length() < 10) {

            inputDescription.setError(
                    "Please provide more details"
            );

            inputDescription.requestFocus();

            return;
        }

        // PRICE
        if (price.isEmpty()) {

            inputPrice.setError(
                    "Enter the price"
            );

            inputPrice.requestFocus();

            return;
        }

        double priceValue;

        try {

            priceValue =
                    Double.parseDouble(price);

            if (priceValue < 0) {

                inputPrice.setError(
                        "Price cannot be negative"
                );

                inputPrice.requestFocus();

                return;
            }

        } catch (NumberFormatException e) {

            inputPrice.setError(
                    "Enter a valid price"
            );

            inputPrice.requestFocus();

            return;
        }

        // LOCATION
        if (location.isEmpty()) {

            inputLocation.setError(
                    "Enter the campus or location"
            );

            inputLocation.requestFocus();

            return;
        }

        // PHOTOS
        if (selectedImages.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please add at least one photo.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        inputTitle.clearFocus();
        inputDescription.clearFocus();
        inputPrice.clearFocus();
        inputLocation.clearFocus();
        inputCategory.clearFocus();

        uploadListing(
                title,
                description,
                price,
                location,
                selectedCategoryId
        );
    }

    private void uploadListing(
            String title,
            String description,
            String price,
            String location,
            long categoryId
    ) {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        String accessToken =
                prefs.getString(
                        ACCESS_TOKEN,
                        null
                );

        String userId =
                prefs.getString(
                        USER_ID,
                        null
                );

        if (accessToken == null
                || userId == null) {

            Toast.makeText(
                    this,
                    "Your session has expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        setUploading(true);

        fetchSellerPhone(
                userId,
                accessToken,
                phone -> {

                    uploadImagesSequentially(
                            0,
                            new ArrayList<>(),
                            title,
                            description,
                            price,
                            location,
                            categoryId,
                            phone,
                            userId,
                            accessToken
                    );
                }
        );
    }

    private void fetchSellerPhone(
            String userId,
            String accessToken,
            PhoneCallback callback
    ) {

        try {

            String encodedUserId =
                    URLEncoder.encode(
                            userId,
                            "UTF-8"
                    );

            String url =
                    SupabaseConfig.SUPABASE_URL
                            + "/rest/v1/profiles"
                            + "?select=phone"
                            + "&id=eq."
                            + encodedUserId;

            Request request =
                    new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader(
                                    "apikey",
                                    SupabaseConfig.SUPABASE_ANON_KEY
                            )
                            .addHeader(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )
                            .build();

            client.newCall(request)
                    .enqueue(new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e
                        ) {

                            mainHandler.post(() ->
                                    callback.onPhone("")
                            );
                        }

                        @Override
                        public void onResponse(
                                Call call,
                                Response response
                        ) throws IOException {

                            String body =
                                    response.body() != null
                                            ? response.body().string()
                                            : "";

                            String phone = "";

                            try {

                                if (!body.isEmpty()) {

                                    JSONArray array =
                                            new JSONArray(body);

                                    if (array.length() > 0) {

                                        JSONObject profile =
                                                array.getJSONObject(0);

                                        phone =
                                                profile.optString(
                                                        "phone",
                                                        ""
                                                );
                                    }
                                }

                            } catch (Exception ignored) {
                            }

                            String finalPhone = phone;

                            mainHandler.post(() ->
                                    callback.onPhone(finalPhone)
                            );
                        }
                    });

        } catch (Exception e) {

            callback.onPhone("");
        }
    }

    private void uploadImagesSequentially(
            int index,
            ArrayList<String> imageUrls,
            String title,
            String description,
            String price,
            String location,
            long categoryId,
            String phone,
            String userId,
            String accessToken
    ) {

        if (index >= selectedImages.size()) {

            insertListing(
                    title,
                    description,
                    price,
                    location,
                    categoryId,
                    phone,
                    userId,
                    accessToken,
                    imageUrls
            );

            return;
        }

        Uri imageUri =
                selectedImages.get(index);

        uploadSingleImage(
                imageUri,
                accessToken,
                new ImageUploadCallback() {

                    @Override
                    public void onSuccess(
                            String imageUrl
                    ) {

                        imageUrls.add(imageUrl);

                        uploadImagesSequentially(
                                index + 1,
                                imageUrls,
                                title,
                                description,
                                price,
                                location,
                                categoryId,
                                phone,
                                userId,
                                accessToken
                        );
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {

                        mainHandler.post(() -> {

                            setUploading(false);

                            Toast.makeText(
                                    PostListingActivity.this,
                                    "Image upload failed: "
                                            + error,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }

    private void uploadSingleImage(
            Uri imageUri,
            String accessToken,
            ImageUploadCallback callback
    ) {

        try {

            byte[] imageBytes =
                    readBytes(imageUri);

            String mimeType =
                    getContentResolver()
                            .getType(imageUri);

            if (mimeType == null) {
                mimeType = "image/jpeg";
            }

            String extension =
                    getExtension(mimeType);

            String fileName =
                    UUID.randomUUID()
                            + extension;

            String storagePath =
                    "listings/"
                            + fileName;

            String url =
                    SupabaseConfig.SUPABASE_URL
                            + "/storage/v1/object/"
                            + BUCKET
                            + "/"
                            + storagePath;

            RequestBody requestBody =
                    RequestBody.create(
                            imageBytes,
                            MediaType.parse(mimeType)
                    );

            Request request =
                    new Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .addHeader(
                                    "apikey",
                                    SupabaseConfig.SUPABASE_ANON_KEY
                            )
                            .addHeader(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )
                            .addHeader(
                                    "Content-Type",
                                    mimeType
                            )
                            .build();

            client.newCall(request)
                    .enqueue(new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e
                        ) {

                            mainHandler.post(() ->
                                    callback.onFailure(
                                            e.getMessage() != null
                                                    ? e.getMessage()
                                                    : "Network error"
                                    )
                            );
                        }

                        @Override
                        public void onResponse(
                                Call call,
                                Response response
                        ) throws IOException {

                            if (response.isSuccessful()) {

                                String publicUrl =
                                        SupabaseConfig.SUPABASE_URL
                                                + "/storage/v1/object/public/"
                                                + BUCKET
                                                + "/"
                                                + storagePath;

                                mainHandler.post(() ->
                                        callback.onSuccess(
                                                publicUrl
                                        )
                                );

                            } else {

                                String error =
                                        response.body() != null
                                                ? response.body().string()
                                                : "Upload failed";

                                mainHandler.post(() ->
                                        callback.onFailure(
                                                error
                                        )
                                );
                            }
                        }
                    });

        } catch (Exception e) {

            callback.onFailure(
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Unable to read image"
            );
        }
    }

    private void insertListing(
            String title,
            String description,
            String price,
            String location,
            long categoryId,
            String phone,
            String userId,
            String accessToken,
            ArrayList<String> imageUrls
    ) {

        try {

            JSONObject listing =
                    new JSONObject();

            /*
             * These column names EXACTLY match
             * the current listings table.
             */

            listing.put(
                    "user_id",
                    userId
            );

            listing.put(
                    "title",
                    title
            );

            listing.put(
                    "description",
                    description
            );

            listing.put(
                    "price",
                    Double.parseDouble(price)
            );

            listing.put(
                    "category_id",
                    categoryId
            );

            listing.put(
                    "location",
                    location
            );

            listing.put(
                    "phone",
                    phone
            );

            listing.put(
                    "status",
                    "active"
            );

            // Store all uploaded image URLs
            JSONArray images =
                    new JSONArray();

            for (String imageUrl : imageUrls) {

                images.put(imageUrl);
            }

            listing.put(
                    "image_urls",
                    images
            );

            // First image for thumbnail / compatibility
            if (!imageUrls.isEmpty()) {

                listing.put(
                        "image_url",
                        imageUrls.get(0)
                );
            }

            String url =
                    SupabaseConfig.SUPABASE_URL
                            + "/rest/v1/listings";

            RequestBody body =
                    RequestBody.create(
                            listing.toString(),
                            MediaType.parse(
                                    "application/json"
                            )
                    );

            Request request =
                    new Request.Builder()
                            .url(url)
                            .post(body)
                            .addHeader(
                                    "apikey",
                                    SupabaseConfig.SUPABASE_ANON_KEY
                            )
                            .addHeader(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )
                            .addHeader(
                                    "Content-Type",
                                    "application/json"
                            )
                            .addHeader(
                                    "Prefer",
                                    "return=representation"
                            )
                            .build();

            client.newCall(request)
                    .enqueue(new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e
                        ) {

                            mainHandler.post(() -> {

                                setUploading(false);

                                Toast.makeText(
                                        PostListingActivity.this,
                                        "Failed to post listing:\n"
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

                            String responseBody =
                                    response.body() != null
                                            ? response.body().string()
                                            : "";

                            if (response.isSuccessful()) {

                                mainHandler.post(() -> {

                                    setUploading(false);

                                    Toast.makeText(
                                            PostListingActivity.this,
                                            "🎉 Listing posted successfully!",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    selectedImages.clear();

                                    Intent intent =
                                            new Intent(
                                                    PostListingActivity.this,
                                                    DashboardActivity.class
                                            );

                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    );

                                    startActivity(intent);

                                    finish();
                                });

                            } else {

                                mainHandler.post(() -> {

                                    setUploading(false);

                                    Toast.makeText(
                                            PostListingActivity.this,
                                            "Could not save listing:\n"
                                                    + response.code()
                                                    + "\n"
                                                    + responseBody,
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                            }
                        }
                    });

        } catch (Exception e) {

            setUploading(false);

            Toast.makeText(
                    this,
                    "Error preparing listing:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private byte[] readBytes(Uri uri)
            throws IOException {

        InputStream inputStream =
                getContentResolver()
                        .openInputStream(uri);

        if (inputStream == null) {

            throw new IOException(
                    "Could not open image"
            );
        }

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[4096];

        int length;

        while ((length =
                inputStream.read(buffer)) != -1) {

            output.write(
                    buffer,
                    0,
                    length
            );
        }

        inputStream.close();

        return output.toByteArray();
    }

    private String getExtension(
            String mimeType
    ) {

        if (mimeType == null) {
            return ".jpg";
        }

        switch (mimeType) {

            case "image/png":
                return ".png";

            case "image/webp":
                return ".webp";

            case "image/gif":
                return ".gif";

            case "image/jpeg":
            default:
                return ".jpg";
        }
    }

    private String getText(
            TextInputEditText field
    ) {

        if (field.getText() == null) {
            return "";
        }

        return field.getText()
                .toString()
                .trim();
    }

    private String getText(
            MaterialAutoCompleteTextView field
    ) {

        if (field.getText() == null) {
            return "";
        }

        return field.getText()
                .toString()
                .trim();
    }

    private void setUploading(
            boolean uploading
    ) {

        mainHandler.post(() -> {

            btnSubmitListing.setEnabled(
                    !uploading
            );

            btnSubmitListing.setText(
                    uploading
                            ? "UPLOADING..."
                            : "POST LISTING"
            );
        });
    }

    private int dpToPx(int dp) {

        return (int) (
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private interface ImageUploadCallback {

        void onSuccess(
                String imageUrl
        );

        void onFailure(
                String error
        );
    }

    private interface PhoneCallback {

        void onPhone(
                String phone
        );
    }
}
