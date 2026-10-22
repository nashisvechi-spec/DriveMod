package ru.drivemod;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class DockOverlay {
    private final DriveAccessibilityService service;
    private final Prefs prefs;
    private final WindowManager wm;
    private LinearLayout root;
    private WindowManager.LayoutParams params;
    private boolean collapsed;
    private int fullHeight, currentHeight;
    private Rect lastBounds;
    DockOverlay(DriveAccessibilityService service, Prefs prefs) {
        this.service = service; this.prefs = prefs; wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    }
    static Rect displayBounds(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= 30) return wm.getMaximumWindowMetrics().getBounds();
        DisplayMetrics metrics = new DisplayMetrics(); wm.getDefaultDisplay().getRealMetrics(metrics);
        return new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
    }
    private int[] insets() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics metrics = wm.getMaximumWindowMetrics();
            Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return new int[]{insets.left, insets.top, insets.right, insets.bottom};
        }
        int status = service.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int nav = service.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return new int[]{0, status > 0 ? service.getResources().getDimensionPixelSize(status) : 0, 0, nav > 0 ? service.getResources().getDimensionPixelSize(nav) : 0};
    }
    void show() {
        Rect bounds = displayBounds(service);
        if (root != null && bounds.equals(lastBounds)) return;
        remove(); lastBounds = bounds; int[] bars = insets();
        fullHeight = Math.max(1, (bounds.height() - bars[1] - bars[3]) / 9);
        currentHeight = collapsed ? Ui.dp(service, 20) : fullHeight;
        params = new WindowManager.LayoutParams(bounds.width() - bars[0] - bars[2], currentHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        if (prefs.keepAwake()) params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.gravity = Gravity.BOTTOM | Gravity.LEFT; params.x = bars[0]; params.y = bars[3];
        if (Build.VERSION.SDK_INT >= 30) params.setFitInsetsTypes(0);
        params.setTitle("DriveMod dock");
        root = Ui.column(service); root.setBackground(Ui.shape(service, Ui.BG, 16, true));
        root.setContentDescription("Панель DriveMod");
        LinearLayout grip = Ui.row(service); grip.setGravity(Gravity.CENTER);
        View bar = new View(service); bar.setBackground(Ui.shape(service, 0xFF3464A5, 4, false));
        grip.addView(bar, new LinearLayout.LayoutParams(Ui.dp(service, 40), Ui.dp(service, 3)));
        grip.setContentDescription(collapsed ? "Развернуть панель DriveMod" : "Свернуть панель DriveMod");
        grip.setFocusable(true); grip.setOnClickListener(v -> { collapsed = !collapsed; refresh(); });
        grip.setOnLongClickListener(v -> { menu(); return true; });
        root.addView(grip, new LinearLayout.LayoutParams(-1, collapsed ? -1 : Math.min(Ui.dp(service, 16), fullHeight / 4)));
        if (!collapsed) {
            LinearLayout buttons = Ui.row(service); buttons.setGravity(Gravity.CENTER);
            for (int i = 0; i < 4; i++) {
                final int slot = i; AppCatalog.Entry entry = AppCatalog.resolve(service, prefs.slot(i));
                LinearLayout item = Ui.column(service); item.setGravity(Gravity.CENTER);
                item.setPadding(Ui.dp(service, 4), 0, Ui.dp(service, 4), Ui.dp(service, 3));
                int available = fullHeight - Math.min(Ui.dp(service, 16), fullHeight / 4);
                boolean labels = available >= Ui.dp(service, 50);
                int size = Math.max(Ui.dp(service, 20), Math.min(Ui.dp(service, 36), available - Ui.dp(service, labels ? 17 : 5)));
                ImageView image = new ImageView(service); image.setImageDrawable(entry == null ? service.getDrawable(R.drawable.app_icon) : entry.icon(service));
                item.addView(image, new LinearLayout.LayoutParams(size, size));
                if (labels) {
                    TextView label = Ui.text(service, entry == null ? "+ Добавить" : entry.label, 9, Ui.MUTED);
                    label.setSingleLine(true); label.setEllipsize(TextUtils.TruncateAt.END); label.setGravity(Gravity.CENTER);
                    item.addView(label, new LinearLayout.LayoutParams(-1, -2));
                }
                item.setContentDescription("Ярлык " + (i + 1) + ": " + (entry == null ? "не выбран" : entry.label));
                item.setFocusable(true); item.setOnClickListener(v -> service.openShortcut(slot));
                item.setOnLongClickListener(v -> { menu(); return true; });
                buttons.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
            }
            root.addView(buttons, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        try { wm.addView(root, params); }
        catch (RuntimeException e) { root = null; currentHeight = 0; }
    }
    private void menu() {
        AlertDialog dialog = new AlertDialog.Builder(service)
                .setTitle("DriveMod")
                .setItems(new String[]{"Открыть пару заново", "Настройки", collapsed ? "Развернуть панель" : "Свернуть панель", "Завершить автомобильный режим"}, (d, which) -> {
                    if (which == 0) service.beginPair();
                    else if (which == 1) service.openSettings();
                    else if (which == 2) { collapsed = !collapsed; refresh(); }
                    else service.stopDriving();
                }).setNegativeButton("Закрыть", null).create();
        if (dialog.getWindow() != null) dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        dialog.show();
    }
    void refresh() { boolean visible = root != null; remove(); if (visible && prefs.showDock() && service.isDriving()) show(); }
    void remove() {
        if (root != null) { try { wm.removeViewImmediate(root); } catch (RuntimeException ignored) { } }
        root = null; currentHeight = 0; lastBounds = null;
    }
    int height() { return currentHeight; }
}
