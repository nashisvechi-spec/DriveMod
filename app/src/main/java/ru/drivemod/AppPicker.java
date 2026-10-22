package ru.drivemod;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AppPicker {
    interface Selection { void selected(AppCatalog.Entry entry); }
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    static void show(Activity activity, String title, String role, String current, Selection callback) {
        LinearLayout content = Ui.column(activity);
        int pad = Ui.dp(activity, 16); content.setPadding(pad, pad, pad, 0);
        EditText search = new EditText(activity); search.setSingleLine(true); search.setTextSize(15);
        search.setTextColor(Ui.TEXT); search.setHintTextColor(Ui.MUTED); search.setHint("Название приложения");
        search.setContentDescription("Поиск установленных приложений"); content.addView(search);
        TextView hint = Ui.text(activity, "Загрузка приложений…", 12, Ui.MUTED);
        hint.setPadding(0, Ui.dp(activity, 10), 0, Ui.dp(activity, 10)); content.addView(hint);
        ListView list = new ListView(activity); list.setDivider(null); list.setClipToPadding(false);
        content.addView(list, new LinearLayout.LayoutParams(-1, Ui.dp(activity, 360)));
        List<AppCatalog.Entry> all = new ArrayList<>(), shown = new ArrayList<>();
        BaseAdapter adapter = new BaseAdapter() {
            public int getCount() { return shown.size(); }
            public Object getItem(int position) { return shown.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position, View recycled, ViewGroup parent) {
                AppCatalog.Entry entry = shown.get(position);
                LinearLayout row = Ui.row(activity); row.setMinimumHeight(Ui.dp(activity, 70));
                row.setPadding(Ui.dp(activity, 8), Ui.dp(activity, 9), Ui.dp(activity, 8), Ui.dp(activity, 9));
                if (entry.id().equals(current)) row.setBackground(Ui.shape(activity, Ui.SURFACE, 14, true));
                ImageView icon = new ImageView(activity); icon.setImageDrawable(entry.icon(activity));
                row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(activity, 40), Ui.dp(activity, 40)));
                LinearLayout label = Ui.column(activity); label.setPadding(Ui.dp(activity, 12), 0, 0, 0);
                TextView name = Ui.text(activity, entry.label, 14, Ui.TEXT); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                label.addView(name); label.addView(Ui.text(activity, entry.pkg(), 10, Ui.MUTED));
                row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
                if (entry.id().equals(current)) row.addView(Ui.text(activity, "✓", 20, 0xFF76A3DD));
                row.setContentDescription(entry.label + ", " + entry.pkg()); return row;
            }
        };
        list.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(title).setView(content).setNegativeButton("Отмена", null).create();
        Runnable filter = () -> {
            String q = search.getText().toString().trim().toLowerCase(Locale.ROOT); shown.clear();
            for (AppCatalog.Entry entry : all) if ((entry.label + " " + entry.pkg()).toLowerCase(Locale.ROOT).contains(q)) shown.add(entry);
            if (!role.isEmpty()) shown.sort((a, b) -> Boolean.compare(b.recommended(role), a.recommended(role)));
            hint.setText(shown.isEmpty() ? "Подходящих приложений нет" : "Установлено: " + shown.size() + (role.isEmpty() ? "" : " · подходящие выше"));
            adapter.notifyDataSetChanged();
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { filter.run(); }
            public void afterTextChanged(Editable e) { }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            AppCatalog.Entry entry = shown.get(position);
            if (AppCatalog.resolve(activity, entry.id()) == null) { Ui.toast(activity, "Приложение больше недоступно"); return; }
            dialog.dismiss(); callback.selected(entry);
        });
        dialog.setOnShowListener(ignored -> {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(0xFF8CB4EB);
        });
        dialog.show();
        WORKER.execute(() -> {
            List<AppCatalog.Entry> loaded = AppCatalog.installed(activity.getApplicationContext(), role.isEmpty());
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!dialog.isShowing() || activity.isFinishing() || activity.isDestroyed()) return;
                all.addAll(loaded); filter.run();
            });
        });
    }
}
