package ru.drivemod;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(3, 5, 7), CARD = Color.rgb(12, 17, 25),
            SURFACE = Color.rgb(19, 28, 41), BLUE = Color.rgb(27, 75, 140),
            TEXT = Color.rgb(240, 244, 250), MUTED = Color.rgb(144, 160, 182),
            LINE = Color.rgb(30, 43, 60), ERROR = Color.rgb(236, 161, 150);
    static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    static LinearLayout column(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c) { LinearLayout l = new LinearLayout(c); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    static GradientDrawable shape(Context c, int color, int radius, boolean border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius));
        if (border) d.setStroke(dp(c, 1), LINE); return d;
    }
    static TextView text(Context c, String value, int sp, int color) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(sp); t.setTextColor(color);
        t.setFontFeatureSettings("tnum"); return t;
    }
    static TextView title(Context c, String value, int sp) {
        TextView t = text(c, value, sp, TEXT); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    static Button button(Context c, String label, boolean primary, Runnable action) {
        Button b = new Button(c); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTextColor(TEXT); b.setMinHeight(dp(c, 48)); b.setMinimumHeight(dp(c, 48));
        b.setPadding(dp(c, 16), dp(c, 7), dp(c, 16), dp(c, 7));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x334B88DD), shape(c, primary ? BLUE : SURFACE, 16, false), null));
        b.setOnClickListener(v -> action.run()); return b;
    }
    static void gap(LinearLayout parent, int dp) {
        View v = new View(parent.getContext()); parent.addView(v, new LinearLayout.LayoutParams(1, dp(parent.getContext(), dp)));
    }
    static LinearLayout card(Context c) {
        LinearLayout card = column(c); card.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        card.setBackground(shape(c, CARD, 22, true)); return card;
    }
    static LinearLayout screen(Activity activity) {
        activity.getWindow().setStatusBarColor(BG); activity.getWindow().setNavigationBarColor(BG);
        LinearLayout root = column(activity); root.setBackgroundColor(BG);
        if (Build.VERSION.SDK_INT >= 30) activity.getWindow().setDecorFitsSystemWindows(false);
        else activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            }
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        activity.setContentView(root); root.requestApplyInsets(); return root;
    }
    static void toast(Context c, String message) { android.widget.Toast.makeText(c, message, android.widget.Toast.LENGTH_LONG).show(); }
}
