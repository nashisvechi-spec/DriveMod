package ru.drivemod;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private Prefs prefs;
    private TextView status;
    private final Handler handler = new Handler();
    private final Runnable statusTick = new Runnable() {
        public void run() { updateStatus(); handler.postDelayed(this, 700); }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); prefs = new Prefs(this);
        if (prefs.slot(3).isEmpty()) prefs.setSlot(3, new ComponentName(this, MainActivity.class).flattenToString());
    }
    @Override protected void onResume() { super.onResume(); render(); handler.post(statusTick); }
    @Override protected void onPause() { handler.removeCallbacks(statusTick); super.onPause(); }
    private void render() {
        LinearLayout root = Ui.screen(this);
        LinearLayout header = Ui.row(this); header.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 12));
        ImageView mark = new ImageView(this); mark.setImageResource(R.drawable.app_icon);
        header.addView(mark, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        TextView brand = Ui.title(this, "DriveMod", 24); brand.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(Ui.button(this, "Настройки", false, () -> startActivity(new Intent(this, SettingsActivity.class))));
        root.addView(header);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = Ui.column(this); body.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 18)); scroll.addView(body);
        body.addView(Ui.title(this, "Два приложения. Один экран.", 24)); Ui.gap(body, 7);
        body.addView(Ui.text(this, "Выберите навигацию и музыку со смартфона", 13, Ui.MUTED)); Ui.gap(body, 22);
        if (prefs.navFirst()) {
            body.addView(appCard(Prefs.NAV, "НАВИГАЦИЯ", "Первое окно", "Выбрать навигацию")); Ui.gap(body, 12);
            body.addView(appCard(Prefs.MUSIC, "МУЗЫКА", "Второе окно", "Выбрать музыку")); Ui.gap(body, 12);
        } else {
            body.addView(appCard(Prefs.MUSIC, "МУЗЫКА", "Первое окно", "Выбрать музыку")); Ui.gap(body, 12);
            body.addView(appCard(Prefs.NAV, "НАВИГАЦИЯ", "Второе окно", "Выбрать навигацию")); Ui.gap(body, 12);
        }
        LinearLayout tools = Ui.row(this);
        tools.addView(Ui.button(this, "↕  Поменять окна", false, () -> { prefs.swap(); render(); }), new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(tools); Ui.gap(body, 20);
        android.widget.Button start = Ui.button(this, "Открыть два приложения", true, this::startPair);
        start.setContentDescription("Запустить выбранную пару приложений"); body.addView(start, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));
        Ui.gap(body, 12); status = Ui.text(this, "", 12, Ui.MUTED); status.setLineSpacing(Ui.dp(this, 3), 1f); body.addView(status);
        Ui.gap(body, 20);
        TextView guide = Ui.text(this, "Размер окон меняется системным разделителем. В портретной ориентации окна располагаются друг над другом. Нижняя панель накладывается на приложения: нажмите её полоску, чтобы свернуть.", 12, Ui.MUTED);
        guide.setLineSpacing(Ui.dp(this, 3), 1f); body.addView(guide); Ui.gap(body, 16);
        body.addView(Ui.button(this, "Как включить службу DriveMod", false, () -> requestService(this)));
        DriveAccessibilityService service = DriveAccessibilityService.current();
        if (service != null && service.isDriving()) { Ui.gap(body, 10); body.addView(Ui.button(this, "Завершить автомобильный режим", false, () -> { service.stopDriving(); render(); })); }
        LinearLayout dock = homeDock();
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 82)); root.addView(dock, dockParams);
        root.post(() -> {
            int usable = root.getHeight() - root.getPaddingTop() - root.getPaddingBottom();
            dockParams.height = Math.max(1, usable / 9); dock.setLayoutParams(dockParams);
        });
        updateStatus();
    }
    private View appCard(String role, String caption, String position, String description) {
        LinearLayout card = Ui.card(this); card.setContentDescription(description);
        TextView cap = Ui.text(this, caption + "  ·  " + position, 10, Ui.MUTED); cap.setLetterSpacing(.1f); card.addView(cap); Ui.gap(card, 12);
        LinearLayout row = Ui.row(this); AppCatalog.Entry app = AppCatalog.resolve(this, prefs.get(role));
        ImageView image = new ImageView(this); image.setImageDrawable(app == null ? getDrawable(R.drawable.app_icon) : app.icon(this));
        row.addView(image, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46)));
        LinearLayout words = Ui.column(this); words.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
        words.addView(Ui.title(this, app == null ? "Выбрать приложение" : app.label, 17));
        TextView sub = Ui.text(this, app == null ? "Из установленных на смартфоне" : "Нажмите, чтобы заменить", 11, Ui.MUTED); words.addView(sub);
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(Ui.text(this, "›", 28, Ui.MUTED)); card.addView(row);
        card.setOnClickListener(v -> AppPicker.show(this, description, role, prefs.get(role), selected -> {
            String other = prefs.get(Prefs.NAV.equals(role) ? Prefs.MUSIC : Prefs.NAV);
            AppCatalog.Entry otherApp = AppCatalog.resolve(this, other);
            if (otherApp != null && otherApp.pkg().equals(selected.pkg())) { Ui.toast(this, "Для пары выберите два разных приложения"); return; }
            prefs.select(role, selected.id()); render();
        }));
        card.setOnLongClickListener(v -> { if (app != null) launchSingle(app); return true; });
        card.setFocusable(true); return card;
    }
    private LinearLayout homeDock() {
        LinearLayout dock = Ui.row(this); dock.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0); dock.setGravity(Gravity.CENTER);
        for (int n = 0; n < 4; n++) {
            final int slot = n; AppCatalog.Entry entry = AppCatalog.resolve(this, prefs.slot(n));
            LinearLayout item = Ui.column(this); item.setGravity(Gravity.CENTER); item.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
            ImageView image = new ImageView(this); image.setImageDrawable(entry == null ? getDrawable(R.drawable.app_icon) : entry.icon(this));
            item.addView(image, new LinearLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30)));
            TextView name = Ui.text(this, entry == null ? "+ Добавить" : entry.label, 9, Ui.MUTED); name.setSingleLine(true); name.setEllipsize(android.text.TextUtils.TruncateAt.END); name.setGravity(Gravity.CENTER); item.addView(name);
            item.setContentDescription("Ярлык " + (n + 1) + ": " + (entry == null ? "выбрать приложение" : entry.label));
            item.setOnClickListener(v -> { if (entry == null) pickSlot(slot); else if (entry.pkg().equals(getPackageName())) startActivity(new Intent(this, SettingsActivity.class)); else launchSingle(entry); });
            item.setOnLongClickListener(v -> { pickSlot(slot); return true; }); item.setFocusable(true);
            dock.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
        return dock;
    }
    private void pickSlot(int slot) { AppPicker.show(this, "Ярлык " + (slot + 1), "", prefs.slot(slot), selected -> { prefs.setSlot(slot, selected.id()); render(); }); }
    private void launchSingle(AppCatalog.Entry app) {
        try { startActivity(AppCatalog.launch(app, false)); }
        catch (RuntimeException e) { Ui.toast(this, "Не удалось открыть приложение. Выберите его заново."); }
    }
    private void startPair() {
        AppCatalog.Entry nav = AppCatalog.resolve(this, prefs.get(Prefs.NAV)), music = AppCatalog.resolve(this, prefs.get(Prefs.MUSIC));
        if (nav == null || music == null) { Ui.toast(this, "Сначала выберите навигацию и музыку"); return; }
        if (nav.pkg().equals(music.pkg())) { Ui.toast(this, "Для пары нужны два разных приложения"); return; }
        DriveAccessibilityService service = DriveAccessibilityService.current();
        if (service == null) { requestService(this); return; }
        service.beginPair();
    }
    private void updateStatus() {
        if (status == null || prefs == null) return;
        String text = prefs.get("status");
        if (DriveAccessibilityService.current() == null) text = "Для запуска пары включите службу DriveMod в специальных возможностях.";
        else if (text.isEmpty()) text = "Служба подключена. Можно открыть два приложения.";
        status.setText(text); status.setTextColor(prefs.data.getBoolean("statusError", false) ? Ui.ERROR : Ui.MUTED);
    }
    static void requestService(Activity activity) {
        new AlertDialog.Builder(activity).setTitle("Служба DriveMod")
            .setMessage("DriveMod использует специальные возможности для запуска пары приложений, проверки размеров их окон и показа панели ярлыков. Служба не анализирует тексты, сообщения и поля ввода, не передаёт данные в интернет.\n\nВ системных настройках выберите «DriveMod — два окна и панель» и включите службу.")
            .setPositiveButton("Открыть настройки", (d, which) -> {
                try { activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                catch (RuntimeException e) { Ui.toast(activity, "Откройте специальные возможности в настройках Android"); }
            }).setNegativeButton("Отмена", null).show();
    }
}
