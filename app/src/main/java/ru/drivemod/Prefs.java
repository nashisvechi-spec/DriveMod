package ru.drivemod;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String NAV = "navigation", MUSIC = "music";
    final SharedPreferences data;
    Prefs(Context context) { data = context.getSharedPreferences("drivemod", Context.MODE_PRIVATE); }
    String get(String key) { return data.getString(key, ""); }
    void set(String key, String value) { data.edit().putString(key, value).apply(); }
    String slot(int n) { return get("slot" + n); }
    void setSlot(int n, String component) { set("slot" + n, component); }
    boolean showDock() { return data.getBoolean("showDock", true); }
    boolean hideOnKeyboard() { return data.getBoolean("hideOnKeyboard", true); }
    boolean keepAwake() { return data.getBoolean("keepAwake", true); }
    boolean navFirst() { return data.getBoolean("navFirst", true); }
    void flag(String key, boolean value) { data.edit().putBoolean(key, value).apply(); }
    void select(String role, String component) {
        String old = get(role);
        int slot = NAV.equals(role) ? 0 : 1;
        SharedPreferences.Editor e = data.edit().putString(role, component);
        if (slot(slot).isEmpty() || slot(slot).equals(old)) e.putString("slot" + slot, component);
        e.apply();
    }
    void swap() {
        flag("navFirst", !navFirst());
    }
    void status(String message, boolean error) {
        data.edit().putString("status", message).putBoolean("statusError", error).apply();
    }
}
