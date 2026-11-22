package com.campuslink.pp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CategorySelectionActivity extends AppCompatActivity {

    private static final String PREFS = "CampusLinkCategoryPrefs";
    private static final String KEY_SELECTED = "selected_categories";

    // These names must match categories.name in Supabase exactly.
    private final String[] categories = {
            "Electronics",
            "Phones",
            "Books",
            "Clothing",
            "Furniture",
            "Food",
            "Services",
            "Jobs & Gigs",
            "Accommodation",
            "Vehicles"
    };

    private final Set<String> selected = new HashSet<>();
    private final Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();
    private TextView selectedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Set<String> saved = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getStringSet(KEY_SELECTED, new HashSet<>());
        if (saved != null) {
            selected.addAll(saved);
        }

        buildScreen();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(16));
        root.setBackgroundColor(Color.rgb(247, 250, 247));

        TextView title = new TextView(this);
        title.setText("Choose Your Categories");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(27, 94, 32));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Select one or more categories to find relevant campus listings.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0, dp(8), 0, dp(12));
        root.addView(subtitle);

        selectedCount = new TextView(this);
        selectedCount.setTextSize(14);
        selectedCount.setTextColor(Color.rgb(27, 94, 32));
        selectedCount.setPadding(0, 0, 0, dp(8));
        root.addView(selectedCount);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button selectAll = new Button(this);
        selectAll.setText("Select All");
        actions.addView(selectAll, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button clear = new Button(this);
        clear.setText("Clear");
        actions.addView(clear, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(actions);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(12), 0, dp(12));

        for (String category : categories) {
            addCategoryRow(list, category);
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button apply = new Button(this);
        apply.setText("Apply Categories");
        apply.setTextColor(Color.WHITE);
        apply.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(27, 94, 32)));
        root.addView(apply, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        selectAll.setOnClickListener(v -> {
            selected.clear();
            selected.addAll(Arrays.asList(categories));
            refreshChecks();
        });

        clear.setOnClickListener(v -> {
            selected.clear();
            refreshChecks();
        });

        apply.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putStringSet(KEY_SELECTED, new HashSet<>(selected))
                    .apply();
            finish();
        });

        setContentView(root);
        refreshChecks();
    }

    private void addCategoryRow(LinearLayout list, String category) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(6), dp(12), dp(6));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(220, 230, 220));
        row.setBackground(background);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(category);
        checkBox.setTextSize(16);
        checkBox.setTextColor(Color.rgb(30, 45, 32));
        checkBox.setButtonTintList(ColorStateList.valueOf(Color.rgb(27, 94, 32)));
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                selected.add(category);
            } else {
                selected.remove(category);
            }
            updateCount();
        });

        checkBoxes.put(category, checkBox);
        row.addView(checkBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));
        list.addView(row);
    }

    private void refreshChecks() {
        for (String category : categories) {
            CheckBox checkBox = checkBoxes.get(category);
            if (checkBox != null) {
                checkBox.setChecked(selected.contains(category));
            }
        }
        updateCount();
    }

    private void updateCount() {
        if (selectedCount != null) {
            selectedCount.setText(selected.size() + " categories selected");
        }
    }
}
