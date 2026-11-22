
package com.campuslink.pp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "CampusLinkMessaging";
    private static final int MAX_MESSAGE_LENGTH = 4000;

    private String conversationId;
    private String otherName;
    private String listingTitle;

    private LinearLayout messages;
    private ScrollView scroll;
    private EditText composer;
    private TextView send;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean active = false;
    private boolean loadingMessages = false;
    private boolean sendingMessage = false;
    private boolean markingRead = false;

    private String lastRenderedMessageId = "";
    private String lastRenderedSnapshot = "";

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if (active) {
                loadMessages(false);
                handler.postDelayed(this, 3000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        conversationId =
                getIntent().getStringExtra("conversation_id");
        otherName =
                getIntent().getStringExtra("other_name");
        listingTitle =
                getIntent().getStringExtra("listing_title");

        if (conversationId == null
                || conversationId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "No conversation selected. Open a conversation from Messages.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        if (otherName == null || otherName.trim().isEmpty()) {
            otherName = "CampusLink User";
        }

        if (listingTitle == null
                || listingTitle.trim().isEmpty()) {
            listingTitle = "CampusLink";
        }

        String userId = MessagingApi.userId(this);

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Your session has expired. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        buildInterface();
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248, 255, 251));
        root.setPadding(dp(12), dp(8), dp(12), dp(8));

        TextView back = new TextView(this);
        back.setText("←  Messages");
        back.setTextColor(Color.rgb(27, 94, 32));
        back.setTypeface(null, Typeface.BOLD);
        back.setTextSize(16);
        back.setPadding(0, dp(8), 0, dp(8));
        back.setOnClickListener(v -> finish());

        root.addView(back);

        TextView name = new TextView(this);
        name.setText(otherName);
        name.setTextColor(Color.rgb(13, 35, 56));
        name.setTextSize(22);
        name.setTypeface(null, Typeface.BOLD);

        root.addView(name);

        TextView item = new TextView(this);
        item.setText("About: " + listingTitle);
        item.setTextColor(Color.rgb(27, 94, 32));
        item.setTextSize(13);
        item.setPadding(0, dp(2), 0, dp(10));

        root.addView(item);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setSmoothScrollingEnabled(true);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(4), dp(8), dp(4), dp(8));

        scroll.addView(
                messages,
                new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout composerRow = new LinearLayout(this);
        composerRow.setOrientation(LinearLayout.HORIZONTAL);
        composerRow.setGravity(Gravity.CENTER_VERTICAL);
        composerRow.setPadding(0, dp(8), 0, dp(4));

        composer = new EditText(this);
        composer.setHint("Write a message...");
        composer.setTextSize(15);
        composer.setMinHeight(dp(48));
        composer.setMaxLines(4);
        composer.setPadding(dp(14), dp(8), dp(14), dp(8));
        composer.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable editBackground = new GradientDrawable();
        editBackground.setColor(Color.WHITE);
        editBackground.setCornerRadius(dp(24));
        editBackground.setStroke(dp(1), Color.LTGRAY);

        composer.setBackground(editBackground);

        composerRow.addView(
                composer,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        send = new TextView(this);
        send.setText("Send");
        send.setTextSize(15);
        send.setTypeface(null, Typeface.BOLD);
        send.setTextColor(Color.WHITE);
        send.setGravity(Gravity.CENTER);
        send.setPadding(dp(16), dp(13), dp(16), dp(13));

        GradientDrawable sendBackground = new GradientDrawable();
        sendBackground.setColor(Color.rgb(27, 94, 32));
        sendBackground.setCornerRadius(dp(24));

        send.setBackground(sendBackground);

        LinearLayout.LayoutParams sendParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        sendParams.leftMargin = dp(8);
        composerRow.addView(send, sendParams);

        root.addView(composerRow);
        setContentView(root);

        send.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();

        active = true;
        loadMessages(true);

        handler.removeCallbacks(refresh);
        handler.postDelayed(refresh, 3000);
    }

    @Override
    protected void onPause() {
        active = false;
        handler.removeCallbacks(refresh);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        active = false;
        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    private void loadMessages(boolean showErrors) {
        if (loadingMessages || !active) {
            return;
        }

        loadingMessages = true;

        String path =
                "/rest/v1/messages"
                        + "?select=*"
                        + "&conversation_id=eq."
                        + MessagingApi.enc(conversationId)
                        + "&order=created_at.asc"
                        + "&limit=500";

        MessagingApi.get(this, path, (ok, code, body, error) ->
                runOnUiThread(() -> {
                    loadingMessages = false;

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!ok) {
                        logRequestError(
                                "Load messages",
                                code,
                                body,
                                error
                        );

                        if (showErrors) {
                            showError(
                                    "Could not load messages. HTTP "
                                            + code + "\n"
                                            + errorDetails(body, error)
                            );
                        }

                        return;
                    }

                    try {
                        JSONArray rows = new JSONArray(body);

                        String newestId = "";

                        if (rows.length() > 0) {
                            newestId = rows
                                    .getJSONObject(rows.length() - 1)
                                    .optString("id", "");
                        }

                        String snapshot = rows.toString();

                        if (!snapshot.equals(lastRenderedSnapshot)
                                || messages.getChildCount() == 0) {

                            messages.removeAllViews();

                            String myUserId = MessagingApi.userId(this);

                            if (rows.length() == 0) {
                                addSystemNote(
                                        "This is the beginning of your "
                                                + "conversation. Keep "
                                                + "communication respectful "
                                                + "and never share your "
                                                + "password or verification codes."
                                );
                            } else {
                                for (int i = 0; i < rows.length(); i++) {
                                    renderMessage(
                                            rows.getJSONObject(i),
                                            myUserId
                                    );
                                }
                            }

                            lastRenderedMessageId = newestId;
                            lastRenderedSnapshot = snapshot;

                            scroll.post(() ->
                                    scroll.fullScroll(View.FOCUS_DOWN)
                            );
                        }

                        markIncomingMessagesRead(
                                rows,
                                MessagingApi.userId(this)
                        );

                    } catch (Exception e) {
                        Log.e(
                                TAG,
                                "Unable to parse messages response",
                                e
                        );

                        if (showErrors) {
                            showError(
                                    "Invalid messages response. Check Logcat."
                            );
                        }
                    }
                })
        );
    }

    private void renderMessage(JSONObject row, String myUserId) {
        String senderId = row.optString("sender_id", "");

        boolean mine = myUserId != null
                && myUserId.equals(senderId);

        String messageText = row.optString("body", "");

        if (messageText.trim().isEmpty()) {
            messageText = row.optString("message", "");
        }

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(mine ? Gravity.END : Gravity.START);
        line.setPadding(0, dp(3), 0, dp(3));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(9), dp(12), dp(7));

        GradientDrawable background = new GradientDrawable();
        background.setColor(
                mine ? Color.rgb(27, 94, 32) : Color.WHITE
        );
        background.setCornerRadius(dp(14));

        if (!mine) {
            background.setStroke(
                    dp(1),
                    Color.rgb(225, 235, 226)
            );
        }

        bubble.setBackground(background);

        TextView body = new TextView(this);
        body.setText(messageText);
        body.setTextSize(15);
        body.setTextColor(
                mine ? Color.WHITE : Color.rgb(30, 45, 40)
        );

        bubble.addView(body);

        TextView time = new TextView(this);

        String createdAt = row.optString("created_at", "");
        String formattedTime = formatMessageTime(createdAt);

        String status = "";

        if (mine) {
            String readAt = row.optString("read_at", "");

            boolean read = !readAt.isEmpty()
                    && !"null".equalsIgnoreCase(readAt);

            status = read ? "  ✓✓ Read" : "  ✓ Sent";
        }

        time.setText(formattedTime + status);
        time.setTextSize(10);
        time.setGravity(Gravity.END);
        time.setTextColor(
                mine ? Color.rgb(220, 245, 222) : Color.GRAY
        );
        time.setPadding(0, dp(3), 0, 0);

        bubble.addView(time);

        int screenWidth = getResources()
                .getDisplayMetrics().widthPixels;

        LinearLayout.LayoutParams bubbleParams =
                new LinearLayout.LayoutParams(
                        (int) (screenWidth * 0.76f),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        bubble.setLayoutParams(bubbleParams);
        line.addView(bubble);
        messages.addView(line);
    }

    private String formatMessageTime(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()
                || "null".equalsIgnoreCase(timestamp)) {
            return "";
        }

        String value = timestamp.trim();

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss"
        };

        Date parsed = null;

        for (String pattern : patterns) {
            try {
                SimpleDateFormat input =
                        new SimpleDateFormat(pattern, Locale.US);

                input.setLenient(false);

                if (pattern.endsWith("'Z'")) {
                    input.setTimeZone(TimeZone.getTimeZone("UTC"));
                } else if (!pattern.contains("XXX")) {
                    input.setTimeZone(TimeZone.getTimeZone("UTC"));
                }

                parsed = input.parse(value);

                if (parsed != null) {
                    break;
                }

            } catch (ParseException ignored) {
                // Try the next supported timestamp format.
            }
        }

        if (parsed == null) {
            Log.w(TAG, "Unrecognized message timestamp: " + timestamp);
            return timestamp;
        }

        SimpleDateFormat output =
                new SimpleDateFormat("h:mm a", Locale.getDefault());

        output.setTimeZone(TimeZone.getDefault());

        return output.format(parsed);
    }

    private void markIncomingMessagesRead(
            JSONArray rows,
            String myUserId
    ) {
        if (markingRead
                || rows == null
                || myUserId == null
                || myUserId.trim().isEmpty()
                || conversationId == null
                || conversationId.trim().isEmpty()) {
            return;
        }

        boolean hasUnreadIncoming = false;

        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);

            if (row == null) {
                continue;
            }

            String senderId = row.optString("sender_id", "");
            String readAt = row.optString("read_at", "");

            boolean incoming = !senderId.isEmpty()
                    && !myUserId.equals(senderId);

            boolean unread = readAt.isEmpty()
                    || "null".equalsIgnoreCase(readAt);

            if (incoming && unread) {
                hasUnreadIncoming = true;
                break;
            }
        }

        if (!hasUnreadIncoming) {
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("p_conversation_id", conversationId);

            markingRead = true;

            MessagingApi.post(
                    this,
                    "/rest/v1/rpc/campuslink_mark_messages_read",
                    payload,
                    (ok, code, body, error) ->
                            runOnUiThread(() -> {
                                markingRead = false;

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                if (!ok) {
                                    logRequestError(
                                            "Mark messages as read",
                                            code,
                                            body,
                                            error
                                    );
                                    return;
                                }

                                lastRenderedSnapshot = "";

                                if (active) {
                                    loadMessages(false);
                                }
                            })
            );

        } catch (Exception e) {
            markingRead = false;

            Log.e(
                    TAG,
                    "Unable to mark messages as read",
                    e
            );
        }
    }

    private void addSystemNote(String text) {
        TextView note = new TextView(this);

        note.setText(text);
        note.setTextColor(Color.GRAY);
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(20), dp(20), dp(20), dp(20));

        messages.addView(note);
    }

    private void sendMessage() {
        if (sendingMessage) {
            return;
        }

        String messageText = composer.getText() == null
                ? ""
                : composer.getText().toString().trim();

        if (messageText.isEmpty()) {
            composer.setError("Type a message first");
            return;
        }

        if (messageText.length() > MAX_MESSAGE_LENGTH) {
            composer.setError(
                    "Messages must be 4,000 characters or fewer"
            );
            return;
        }

        String myUserId = MessagingApi.userId(this);

        if (myUserId == null || myUserId.trim().isEmpty()) {
            showError("Your session has expired. Please log in again.");
            return;
        }

        sendingMessage = true;
        send.setEnabled(false);
        send.setText("Sending...");

        try {
            JSONObject payload = new JSONObject();

            payload.put("conversation_id", conversationId);
            payload.put("sender_id", myUserId);
            payload.put("body", messageText);

            MessagingApi.post(
                    this,
                    "/rest/v1/messages",
                    payload,
                    (ok, code, responseBody, error) ->
                            runOnUiThread(() -> {
                                sendingMessage = false;

                                send.setEnabled(true);
                                send.setText("Send");

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                if (!ok) {
                                    logRequestError(
                                            "Send message",
                                            code,
                                            responseBody,
                                            error
                                    );

                                    showError(
                                            "Message not sent. HTTP "
                                                    + code + "\n"
                                                    + errorDetails(
                                                    responseBody,
                                                    error
                                            )
                                    );

                                    return;
                                }

                                composer.setText("");

                                InputMethodManager imm =
                                        (InputMethodManager)
                                                getSystemService(
                                                        Context.INPUT_METHOD_SERVICE
                                                );

                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(
                                            composer.getWindowToken(),
                                            0
                                    );
                                }

                                lastRenderedMessageId = "";
                                lastRenderedSnapshot = "";

                                loadMessages(true);
                            })
            );

        } catch (Exception e) {
            sendingMessage = false;

            send.setEnabled(true);
            send.setText("Send");

            Log.e(TAG, "Unable to prepare message payload", e);

            showError("Could not prepare message: " + e.getMessage());
        }
    }

    private void logRequestError(
            String operation,
            int code,
            String responseBody,
            Exception error
    ) {
        Log.e(
                TAG,
                operation + " failed. HTTP " + code
                        + ". Response: " + responseBody,
                error
        );
    }

    private String errorDetails(String body, Exception error) {
        String details = body == null ? "" : body.trim();

        if (error != null) {
            details = error.getMessage() == null
                    ? error.toString()
                    : error.getMessage();
        }

        if (details.isEmpty()) {
            details = "No error details returned by the server.";
        }

        if (details.length() > 700) {
            details = details.substring(0, 700);
        }

        return details;
    }

    private void showError(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private int dp(float value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }
}
