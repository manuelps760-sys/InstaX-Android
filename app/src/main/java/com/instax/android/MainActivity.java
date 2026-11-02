package com.instax.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(16, 19, 27);
    private final int panel = Color.rgb(27, 32, 44);
    private final int accent = Color.rgb(91, 140, 255);
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        showHome();
    }

    private TextView label(String text, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextColor(Color.WHITE); v.setTextSize(size);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private void showHome() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg);
        root.setPadding(20, 18, 20, 12);
        TextView brand = label("InstaX", 30, true); brand.setTextColor(accent); root.addView(brand);
        TextView subtitle = label("Your world. Your people.", 14, false); subtitle.setPadding(0, 4, 0, 22); root.addView(subtitle);
        TextView stories = label("STORIES", 12, true); stories.setPadding(0, 8, 0, 12); root.addView(stories);
        TextView storyRow = label("  + Add story       ◯ Friends       ◯ Explore", 15, false);
        storyRow.setBackgroundColor(panel); storyRow.setPadding(14, 18, 14, 18); root.addView(storyRow);
        TextView feedTitle = label("YOUR FEED", 12, true); feedTitle.setPadding(0, 24, 0, 10); root.addView(feedTitle);
        TextView empty = label("Welcome to InstaX\n\nYour photo and video feed will appear here once accounts and posts are connected.", 16, false);
        empty.setGravity(Gravity.CENTER); empty.setPadding(20, 30, 20, 30); empty.setBackgroundColor(panel);
        root.addView(empty, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(0, 16, 0, 8);
        String[] tabs = {"Home", "Search", "Create", "Activity", "Profile"};
        for (String tab : tabs) {
            TextView item = label(tab, 12, tab.equals("Home")); item.setGravity(Gravity.CENTER);
            item.setTextColor(tab.equals("Home") ? accent : Color.LTGRAY);
            nav.addView(item, new LinearLayout.LayoutParams(0, 48, 1));
        }
        root.addView(nav); setContentView(root);
    }
}
