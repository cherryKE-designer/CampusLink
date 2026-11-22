
package com.campuslink.pp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MessagesInboxActivity extends AppCompatActivity {

    private static final String TAG = "CampusLinkInbox";

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private LinearLayout list;
    private TextView empty;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Map<String, Integer> unreadCounts =
            new HashMap<>();

    private boolean active = false;
    private boolean loadingConversations = false;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if (active) {
                loadConversations();
                handler.postDelayed(this, 5000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248, 255, 251));
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView back = new TextView(this);
        back.setText("←  Back to CampusLink");
        back.setTextColor(Color.rgb(27, 94, 32));
        back.setTextSize(16);
        back.setTypeface(null, Typeface.BOLD);
        back.setPadding(0, dp(8), 0, dp(18));
        back.setOnClickListener(v -> finish());

        root.addView(back);

        TextView title = new TextView(this);
        title.setText("Messages");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(13, 35, 56));

        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Conversations with buyers and sellers");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.GRAY);
        subtitle.setPadding(0, dp(4), 0, dp(16));

        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        scroll.addView(
                list,
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

        empty = new TextView(this);
        empty.setText(
                "No conversations yet.\n\n"
                        + "Open a listing and tap Message Seller "
                        + "to start chatting."
        );
        empty.setTextColor(Color.GRAY);
        empty.setTextSize(15);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(20), dp(36), dp(20), dp(36));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();

        active = true;

        handler.removeCallbacks(refresh);

        loadConversations();

        handler.postDelayed(refresh, 5000);
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

    private void loadConversations() {
        if (!active || loadingConversations) {
            return;
        }

        String uid = MessagingApi.userId(this);

        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Please log in again to view messages.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        loadingConversations = true;

        String path =
                "/rest/v1/conversations"
                        + "?select=*"
                        + "&or=(buyer_id.eq."
                        + MessagingApi.enc(uid)
                        + ",seller_id.eq."
                        + MessagingApi.enc(uid)
                        + ")"
                        + "&order=updated_at.desc";

        MessagingApi.get(
                this,
                path,
                (ok, code, body, error) ->
                        runOnUiThread(() -> {
                            loadingConversations = false;

                            if (!active || isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            if (!ok) {
                                Log.e(
                                        TAG,
                                        "Could not load conversations. HTTP "
                                                + code + ": " + body,
                                        error
                                );

                                showError(
                                        "Could not load conversations ("
                                                + code
                                                + "). Check your connection "
                                                + "and Supabase permissions."
                                );

                                return;
                            }

                            try {
                                JSONArray rows = new JSONArray(body);

                                if (rows.length() == 0) {
                                    unreadCounts.clear();
                                    renderConversations(rows, uid);
                                    return;
                                }

                                loadUnreadCounts(rows, uid);

                            } catch (Exception e) {
                                Log.e(
                                        TAG,
                                        "Invalid conversations response",
                                        e
                                );

                                showError(
                                        "Unexpected response from Supabase."
                                );
                            }
                        })
        );
    }

    private void loadUnreadCounts(
            JSONArray conversations,
            String uid
    ) {
        Set<String> conversationIds = new HashSet<>();

        for (int i = 0; i < conversations.length(); i++) {
            JSONObject row = conversations.optJSONObject(i);

            if (row == null) {
                continue;
            }

            String id = row.optString("id", "");

            if (UUID_PATTERN.matcher(id).matches()) {
                conversationIds.add(id);
            }
        }

        if (conversationIds.isEmpty()) {
            unreadCounts.clear();
            renderConversations(conversations, uid);
            return;
        }

        StringBuilder ids = new StringBuilder();

        for (String id : conversationIds) {
            if (ids.length() > 0) {
                ids.append(",");
            }

            ids.append(id);
        }

        /*
         * Unread means:
         * 1. The message belongs to one of the user's conversations.
         * 2. The sender is not the current user.
         * 3. read_at is NULL.
         */
        String path =
                "/rest/v1/messages"
                        + "?select=conversation_id"
                        + "&conversation_id=in.("
                        + ids
                        + ")"
                        + "&sender_id=neq."
                        + MessagingApi.enc(uid)
                        + "&read_at=is.null"
                        + "&limit=5000";

        MessagingApi.get(
                this,
                path,
                (ok, code, body, error) ->
                        runOnUiThread(() -> {
                            if (!active || isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            unreadCounts.clear();

                            if (ok) {
                                try {
                                    JSONArray unreadRows =
                                            new JSONArray(body);

                                    for (int i = 0;
                                         i < unreadRows.length();
                                         i++) {

                                        JSONObject message =
                                                unreadRows.optJSONObject(i);

                                        if (message == null) {
                                            continue;
                                        }

                                        String conversationId =
                                                message.optString(
                                                        "conversation_id",
                                                        ""
                                                );

                                        if (conversationIds.contains(
                                                conversationId)) {

                                            int count =
                                                    unreadCounts.containsKey(
                                                            conversationId
                                                    )
                                                            ? unreadCounts.get(
                                                            conversationId
                                                    )
                                                            : 0;

                                            unreadCounts.put(
                                                    conversationId,
                                                    count + 1
                                            );
                                        }
                                    }

                                } catch (Exception e) {
                                    Log.e(
                                            TAG,
                                            "Could not parse unread messages",
                                            e
                                    );
                                }
                            } else {
                                /*
                                 * Keep the inbox usable if the unread-count
                                 * request fails. Conversation access remains
                                 * controlled by Supabase RLS.
                                 */
                                Log.e(
                                        TAG,
                                        "Unread count request failed. HTTP "
                                                + code + ": " + body,
                                        error
                                );
                            }

                            renderConversations(conversations, uid);
                        })
        );
    }

    private void renderConversations(
            JSONArray rows,
            String uid
    ) {
        if (list == null) {
            return;
        }

        list.removeAllViews();

        if (rows.length() == 0) {
            list.addView(empty);
            return;
        }

        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);

            if (row != null) {
                addConversation(row, uid);
            }
        }
    }

    private void addConversation(
            JSONObject row,
            String uid
    ) {
        String conversationId = row.optString("id", "");
        String buyer = row.optString("buyer_id", "");
        String seller = row.optString("seller_id", "");

        boolean isBuyer = uid.equals(buyer);

        String other = isBuyer
                ? row.optString("seller_name", "CampusLink Seller")
                : row.optString("buyer_name", "CampusLink Buyer");

        if (other.trim().isEmpty()
                || "null".equalsIgnoreCase(other)) {
            other = isBuyer
                    ? "CampusLink Seller"
                    : "CampusLink Buyer";
        }

        String listing = row.optString(
                "listing_title",
                "CampusLink conversation"
        );

        if (listing.trim().isEmpty()
                || "null".equalsIgnoreCase(listing)) {
            listing = "CampusLink conversation";
        }

        String last = row.optString(
                "last_message",
                "Conversation started — send a message"
        );

        if (last.trim().isEmpty()
                || "null".equalsIgnoreCase(last)) {
            last = "Conversation started — send a message";
        }

        int unread = unreadCounts.containsKey(conversationId)
                ? unreadCounts.get(conversationId)
                : 0;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(
                unread > 0
                        ? Color.rgb(239, 250, 241)
                        : Color.WHITE
        );
        bg.setCornerRadius(dp(14));
        bg.setStroke(
                dp(1),
                unread > 0
                        ? Color.rgb(27, 94, 32)
                        : Color.rgb(221, 238, 225)
        );

        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        // Conversation name and unread badge.
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(other);
        name.setTextSize(17);
        name.setTypeface(
                null,
                unread > 0 ? Typeface.BOLD : Typeface.NORMAL
        );
        name.setTextColor(Color.rgb(13, 35, 56));

        heading.addView(
                name,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        if (unread > 0) {
            TextView badge = new TextView(this);

            badge.setText(
                    unread > 99 ? "99+" : String.valueOf(unread)
            );
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(12);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setMinWidth(dp(25));
            badge.setPadding(dp(7), dp(4), dp(7), dp(4));

            GradientDrawable badgeBackground =
                    new GradientDrawable();

            badgeBackground.setColor(Color.rgb(27, 94, 32));
            badgeBackground.setCornerRadius(dp(20));

            badge.setBackground(badgeBackground);

            heading.addView(badge);
        }

        card.addView(heading);

        TextView item = new TextView(this);
        item.setText("Listing: " + listing);
        item.setTextSize(13);
        item.setTextColor(Color.rgb(27, 94, 32));
        item.setPadding(0, dp(4), 0, dp(5));

        card.addView(item);

        TextView preview = new TextView(this);
        preview.setText(last);
        preview.setMaxLines(2);
        preview.setTextSize(14);
        preview.setTextColor(Color.DKGRAY);

        if (unread > 0) {
            preview.setTypeface(null, Typeface.BOLD);
        }

        card.addView(preview);

        TextView status = new TextView(this);
        status.setText(
                unread > 0
                        ? unread + " unread message"
                        + (unread == 1 ? "" : "s")
                        : "Open conversation"
        );
        status.setTextSize(11);
        status.setTextColor(
                unread > 0
                        ? Color.rgb(27, 94, 32)
                        : Color.GRAY
        );
        status.setPadding(0, dp(7), 0, 0);

        card.addView(status);

        final String finalConversationId = conversationId;
        final String finalOther = other;
        final String finalListing = listing;

        card.setOnClickListener(v -> {
            if (finalConversationId.trim().isEmpty()) {
                showError("This conversation has no valid ID.");
                return;
            }

            Intent intent = new Intent(
                    MessagesInboxActivity.this,
                    ChatActivity.class
            );

            intent.putExtra("conversation_id", finalConversationId);
            intent.putExtra("other_name", finalOther);
            intent.putExtra("listing_title", finalListing);

            startActivity(intent);
        });

        list.addView(card);
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
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }
}
