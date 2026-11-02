package com.instax.android;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int PICK_MEDIA = 4102;
    private final int bg = Color.rgb(16, 19, 27);
    private final int panel = Color.rgb(27, 32, 44);
    private final int accent = Color.rgb(91, 140, 255);
    private LinearLayout root, content, nav;
    private String active = "Home";
    private Uri selectedMedia;
    private EditText captionField;
    private TextView selectedMediaLabel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        showPage("Home");
    }

    private TextView text(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private void addSpace(LinearLayout box, int height) {
        View space = new View(this);
        box.addView(space, new LinearLayout.LayoutParams(1, height));
    }

    private void addCard(String title, String body) {
        LinearLayout card = column();
        card.setBackgroundColor(panel);
        card.setPadding(16, 14, 16, 14);
        card.addView(text(title, 16, true));
        addSpace(card, 8);
        card.addView(text(body, 14, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 14;
        content.addView(card, lp);
    }

    private Button button(String caption, Runnable action) {
        Button b = new Button(this);
        b.setText(caption);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextColor(Color.WHITE);
        e.setPadding(14, 8, 14, 8);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        return e;
    }

    private void openMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_MEDIA);
        } catch (Exception e) {
            Toast.makeText(this, "No compatible media picker is available.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_MEDIA || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        selectedMedia = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(selectedMedia,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) { }
        String mime = getContentResolver().getType(selectedMedia);
        String kind = mime != null && mime.startsWith("video/") ? "Video selected" : "Photo selected";
        if (selectedMediaLabel != null) {
            selectedMediaLabel.setText(kind + "\n" + selectedMedia);
        }
        Toast.makeText(this, kind, Toast.LENGTH_SHORT).show();
    }

    private void saveDraft() {
        if (selectedMedia == null) {
            Toast.makeText(this, "Select a photo or video first.", Toast.LENGTH_SHORT).show();
            return;
        }
        String caption = captionField == null ? "" : captionField.getText().toString().trim();
        SharedPreferences prefs = getSharedPreferences("instax_drafts", MODE_PRIVATE);
        prefs.edit()
            .putString("media_uri", selectedMedia.toString())
            .putString("caption", caption)
            .putLong("saved_at", System.currentTimeMillis())
            .apply();
        Toast.makeText(this, "Draft saved on this device.", Toast.LENGTH_LONG).show();
    }

    private void showPage(String page) {
        active = page;
        root = column();
        root.setBackgroundColor(bg);
        root.setPadding(18, 16, 18, 8);

        TextView brand = text("InstaX", 29, true);
        brand.setTextColor(accent);
        root.addView(brand);
        TextView tagline = text("Your world. Your people.", 13, false);
        tagline.setTextColor(Color.LTGRAY);
        root.addView(tagline);
        addSpace(root, 14);

        ScrollView scroll = new ScrollView(this);
        content = column();
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        switch (page) {
            case "Home":
                content.addView(text("STORIES", 12, true));
                addSpace(content, 8);
                addCard("＋ Add to your story", "Share a photo or video with your followers.");
                content.addView(text("YOUR FEED", 12, true));
                addSpace(content, 8);
                addCard("Welcome to InstaX", "Your feed is ready. Sign-in and backend services will connect real posts, creators, likes and comments.");
                addCard("Discover creators", "Use Search to find people and explore public profiles.");
                break;
            case "Search":
                content.addView(text("DISCOVER", 18, true));
                addSpace(content, 8);
                content.addView(field("Search people and posts"));
                addSpace(content, 12);
                addCard("Explore", "Creator discovery and hashtag search will appear here when the backend is connected.");
                break;
            case "Create":
                content.addView(text("CREATE A POST", 18, true));
                addSpace(content, 8);
                addCard("Share a moment", "Choose a photo or video, add a caption, and save a draft on this device.");
                content.addView(button("CHOOSE PHOTO OR VIDEO", this::openMediaPicker));
                addSpace(content, 10);
                selectedMediaLabel = text(selectedMedia == null ? "No media selected" : "Selected: " + selectedMedia, 12, false);
                selectedMediaLabel.setTextColor(Color.LTGRAY);
                content.addView(selectedMediaLabel);
                addSpace(content, 10);
                captionField = field("Write a caption...");
                captionField.setSingleLine(false);
                captionField.setMinLines(3);
                content.addView(captionField);
                addSpace(content, 10);
                content.addView(button("SAVE LOCAL DRAFT", this::saveDraft));
                addSpace(content, 8);
                addCard("Posting status", "Drafts are stored locally. Publishing to a shared feed requires account and cloud-storage setup.");
                break;
            case "Activity":
                content.addView(text("ACTIVITY", 18, true));
                addSpace(content, 8);
                addCard("You're all caught up", "Likes, comments, follows and mentions will appear here.");
                break;
            case "Profile":
                content.addView(text("YOUR PROFILE", 18, true));
                addSpace(content, 8);
                addCard("Set up your profile", "Add a profile photo, username and bio after creating your account.");
                content.addView(button("SIGN IN / CREATE ACCOUNT", () ->
                    Toast.makeText(this, "Authentication will be connected with Supabase.", Toast.LENGTH_SHORT).show()));
                addSpace(content, 10);
                addCard("Posts     0          Followers     0          Following     0", "Your profile grid will appear here.");
                break;
            default:
                addCard(page, "This section is being prepared.");
        }

        nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, 8, 0, 4);
        String[] tabs = {"Home", "Search", "Create", "Activity", "Profile"};
        for (String tab : tabs) {
            TextView item = text(tab, 11, tab.equals(active));
            item.setGravity(Gravity.CENTER);
            item.setTextColor(tab.equals(active) ? accent : Color.LTGRAY);
            item.setOnClickListener(v -> showPage(tab));
            nav.addView(item, new LinearLayout.LayoutParams(0, 48, 1));
        }
        root.addView(nav);
        setContentView(root);
    }
}
