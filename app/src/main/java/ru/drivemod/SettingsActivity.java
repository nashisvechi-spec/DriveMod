package ru.drivemod;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private Prefs prefs;
    @Override public void onCreate(Bundle state) { super.onCreate(state); prefs = new Prefs(this); }
    @Override protected void onResume() { super.onResume(); render(); }
    private void render() {
        LinearLayout root = Ui.screen(this);
        LinearLayout head = Ui.row(this); head.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 12));
        head.addView(Ui.title(this, "Настройки", 24), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(Ui.button(this, "Готово", false, this::finish)); root.addView(head);
        ScrollView scroll = new ScrollView(this); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = Ui.column(this); body.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 24)); scroll.addView(body);
        body.addView(Ui.title(this, "Четыре ярлыка", 18)); Ui.gap(body, 8);
        body.addView(Ui.text(this, "Панель занимает 1/9 доступной высоты. Нажатие на полоску сворачивает её; удерживание открывает меню.", 12, Ui.MUTED)); Ui.gap(body, 12);
        for (int n = 0; n < 4; n++) {
            final int slot = n; AppCatalog.Entry app = AppCatalog.resolve(this, prefs.slot(n));
            body.addView(Ui.button(this, (n + 1) + "   " + (app == null ? "Выбрать приложение" : app.label), false,
                () -> AppPicker.show(this, "Ярлык " + (slot + 1), "", prefs.slot(slot), selected -> { prefs.setSlot(slot, selected.id()); render(); })), new LinearLayout.LayoutParams(-1, -2));
            Ui.gap(body, 8);
        }
        Ui.gap(body, 18); body.addView(Ui.title(this, "Панель и экран", 18)); Ui.gap(body, 8);
        toggle(body, "Показывать нижнюю панель", "showDock", prefs.showDock());
        toggle(body, "Скрывать панель при вводе с клавиатуры", "hideOnKeyboard", prefs.hideOnKeyboard());
        toggle(body, "Не гасить экран в автомобильном режиме", "keepAwake", prefs.keepAwake());
        Ui.gap(body, 18); body.addView(Ui.title(this, "Разрешение", 18)); Ui.gap(body, 12);
        body.addView(Ui.button(this, "Специальные возможности DriveMod", false, () -> MainActivity.requestService(this)));
        Ui.gap(body, 10);
        TextView restricted = Ui.text(this, "Если Android запрещает включить службу: откройте сведения о DriveMod → меню ⋮ → «Разрешить ограниченные настройки», затем вернитесь в специальные возможности. Названия пунктов зависят от телефона.", 12, Ui.MUTED);
        restricted.setLineSpacing(Ui.dp(this, 3), 1); body.addView(restricted); Ui.gap(body, 10);
        body.addView(Ui.button(this, "Сведения о приложении в Android", false,
            () -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))));
        Ui.gap(body, 24); LinearLayout note = Ui.card(this); note.addView(Ui.title(this, "Два настоящих приложения", 16)); Ui.gap(note, 8);
        TextView details = Ui.text(this, "Окна создаёт Android. Их размеры и расположение меняются системным разделителем. Не все приложения и прошивки разрешают такой запуск.\n\nПанель DriveMod накладывается на нижний край и может закрывать часть элементов второго приложения. Сверните её, когда нужен весь экран.", 12, Ui.MUTED);
        details.setLineSpacing(Ui.dp(this, 3), 1); note.addView(details); body.addView(note);
        Ui.gap(body, 18); body.addView(Ui.text(this, "DriveMod " + BuildConfig.VERSION_NAME + " · Android " + android.os.Build.VERSION.RELEASE + "\nТёмно-синий и чёрный · Яндекс API пока не подключён", 11, Ui.MUTED));
    }
    private void toggle(LinearLayout body, String label, String key, boolean value) {
        Switch control = new Switch(this); control.setText(label); control.setTextSize(13); control.setTextColor(Ui.TEXT);
        control.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12)); control.setChecked(value);
        control.setOnCheckedChangeListener((button, checked) -> prefs.flag(key, checked));
        body.addView(control, new LinearLayout.LayoutParams(-1, -2));
    }
}
