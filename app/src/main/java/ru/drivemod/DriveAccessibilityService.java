package ru.drivemod;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class DriveAccessibilityService extends AccessibilityService {
    private static volatile DriveAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final PairLaunchMachine machine = new PairLaunchMachine();
    private Prefs prefs;
    private DockOverlay dock;
    private AppCatalog.Entry first, second;
    private volatile boolean driving;
    private String eventPackage = "";
    private volatile boolean pairVisible;
    static DriveAccessibilityService current() { return instance; }
    boolean isDriving() { return driving; }
    private final Runnable poll = this::tick;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (data, key) -> {
        if (key == null || prefs == null) return;
        if (Prefs.NAV.equals(key) || Prefs.MUSIC.equals(key) || "navFirst".equals(key)) { if (driving) stopDriving(); }
        else if (key.startsWith("slot") || key.equals("showDock") || key.equals("keepAwake") || key.equals("hideOnKeyboard")) {
            if (dock != null) dock.refresh();
        }
    };
    @Override protected void onServiceConnected() {
        super.onServiceConnected(); instance = this; prefs = new Prefs(this); dock = new DockOverlay(this, prefs);
        prefs.data.registerOnSharedPreferenceChangeListener(listener);
        prefs.status("Служба подключена. Выберите приложения и откройте пару.", false);
    }
    void beginPair() {
        if (prefs == null) return;
        AppCatalog.Entry nav = AppCatalog.resolve(this, prefs.get(Prefs.NAV));
        AppCatalog.Entry music = AppCatalog.resolve(this, prefs.get(Prefs.MUSIC));
        if (nav == null || music == null || nav.pkg().equals(music.pkg())
                || nav.pkg().equals(getPackageName()) || music.pkg().equals(getPackageName())) {
            prefs.status("Выберите два разных установленных приложения: навигацию и музыку.", true);
            Ui.toast(this, prefs.get("status")); return;
        }
        stopDriving(); first = prefs.navFirst() ? nav : music; second = prefs.navFirst() ? music : nav;
        driving = true; pairVisible = false; eventPackage = "";
        machine.begin(SystemClock.elapsedRealtime()); prefs.status("Открываю первое приложение…", false);
        try { startActivity(AppCatalog.launch(first, false)); handler.postDelayed(poll, 180); }
        catch (RuntimeException e) { fail("Не удалось открыть навигацию. Выберите приложение заново."); }
    }
    void stopDriving() {
        driving = false; pairVisible = false; machine.stop(); handler.removeCallbacks(poll);
        if (dock != null) dock.remove();
        if (prefs != null) prefs.status("Автомобильный режим завершён.", false);
    }
    private void tick() {
        handler.removeCallbacks(poll);
        if (!driving || first == null || second == null) return;
        Snapshot snapshot = snapshot();
        pairVisible = WindowGeometry.separated(snapshot.first, snapshot.second, snapshot.width, snapshot.height);
        boolean partial = snapshot.first != null && WindowGeometry.area(snapshot.first) < (long) snapshot.width * snapshot.height * .82;
        PairLaunchMachine.Action action = machine.observe(snapshot.first != null || eventPackage.equals(first.pkg()), partial, pairVisible, SystemClock.elapsedRealtime());
        switch (action) {
            case REQUEST_SPLIT:
                prefs.status("Запрашиваю разделённый экран…", false);
                performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                break;
            case OPEN_SECOND:
                prefs.status("Открываю второе приложение…", false);
                try { startActivity(AppCatalog.launch(second, true)); }
                catch (RuntimeException e) { fail("Музыкальное приложение недоступно. Выберите его заново."); return; }
                break;
            case READY:
                prefs.status("Два приложения открыты. Размер меняется системным разделителем.", false);
                break;
            case FAIL:
                fail("Android не подтвердил два окна. Откройте выбранную пару вручную через «Недавние» → разделённый экран. Панель DriveMod остаётся доступной.");
                return;
            default: break;
        }
        if (machine.phase() == PairLaunchMachine.Phase.ACTIVE || machine.phase() == PairLaunchMachine.Phase.FAILED) updateDock(snapshot.keyboard);
        handler.postDelayed(poll, machine.phase() == PairLaunchMachine.Phase.ACTIVE ? 800 : 220);
    }
    private void fail(String message) {
        handler.removeCallbacks(poll); machine.fail(); prefs.status(message, true); Ui.toast(this, message);
        // The visible controls remain usable even if an OEM rejects automatic split-screen.
        if (driving && dock != null) { updateDock(false); handler.postDelayed(poll, 1000); }
    }
    private void updateDock(boolean keyboard) {
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        if (!prefs.showDock() || locked || (keyboard && prefs.hideOnKeyboard())) dock.remove();
        else dock.show();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!driving) return;
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName() != null) eventPackage = event.getPackageName().toString();
        handler.removeCallbacks(poll); handler.postDelayed(poll, 90);
    }
    @Override public void onInterrupt() { stopDriving(); }
    @Override public boolean onUnbind(Intent intent) { stopDriving(); if (instance == this) instance = null; return super.onUnbind(intent); }
    @Override public void onDestroy() {
        stopDriving(); if (prefs != null) prefs.data.unregisterOnSharedPreferenceChangeListener(listener);
        if (instance == this) instance = null; super.onDestroy();
    }
    private static final class Snapshot { int[] first, second; int width, height; boolean keyboard; }
    private Snapshot snapshot() {
        Snapshot s = new Snapshot(); Rect display = DockOverlay.displayBounds(this); s.width = display.width(); s.height = display.height();
        List<AccessibilityWindowInfo> windows = getWindows();
        for (AccessibilityWindowInfo window : windows) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) s.keyboard = true;
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) continue;
            try {
                // Only the root package and window rectangle are inspected; no node text or child traversal.
                CharSequence pkg = root.getPackageName(); if (pkg == null) continue;
                Rect rect = new Rect(); window.getBoundsInScreen(rect); int[] bounds = {rect.left, rect.top, rect.right, rect.bottom};
                if (first != null && first.pkg().contentEquals(pkg) && (s.first == null || WindowGeometry.area(bounds) > WindowGeometry.area(s.first))) s.first = bounds;
                if (second != null && second.pkg().contentEquals(pkg) && (s.second == null || WindowGeometry.area(bounds) > WindowGeometry.area(s.second))) s.second = bounds;
            } finally { root.recycle(); }
        }
        return s;
    }
    void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    void openShortcut(int slot) {
        AppCatalog.Entry entry = AppCatalog.resolve(this, prefs.slot(slot));
        if (entry == null) { Ui.toast(this, "Ярлык не настроен. Удерживайте панель и откройте настройки."); return; }
        if (entry.pkg().equals(getPackageName())) { openSettings(); return; }
        try { startActivity(AppCatalog.launch(entry, false)); }
        catch (RuntimeException e) { Ui.toast(this, "Не удалось открыть приложение. Замените ярлык в настройках."); }
    }
    @Override protected void dump(FileDescriptor fd, PrintWriter out, String[] args) {
        out.println("DriveMod"); out.println("driving=" + driving); out.println("phase=" + machine.phase());
        out.println("pairVisible=" + pairVisible); out.println("dockHeight=" + (dock == null ? 0 : dock.height()));
        out.println("dockSlots=4"); out.println("version=" + BuildConfig.VERSION_NAME);
    }
}
