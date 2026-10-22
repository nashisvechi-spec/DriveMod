package ru.drivemod;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;

/** LAUNCH_ADJACENT needs an Activity source; Android ignores it from a Service. */
public class PairLaunchActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AppCatalog.Entry first, second;
    private boolean adjacentLaunched, replacementLaunched;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        first = AppCatalog.resolve(this, getIntent().getStringExtra("first"));
        second = AppCatalog.resolve(this, getIntent().getStringExtra("second"));
        if (saved != null) {
            adjacentLaunched = saved.getBoolean("adjacentLaunched");
            replacementLaunched = saved.getBoolean("replacementLaunched");
        }
        LinearLayout screen = Ui.screen(this);
        screen.setGravity(android.view.Gravity.CENTER);
        screen.addView(Ui.title(this, "Открываю приложения…", 22));
        if (first == null || second == null || first.pkg().equals(second.pkg())) {
            failed(); return;
        }
        handler.postDelayed(this::finish, 14000);
    }

    @Override protected void onResume() {
        super.onResume();
        if (first == null || second == null) return;
        if (!adjacentLaunched) handler.postDelayed(this::openAdjacent, 200);
        else if (isInMultiWindowMode()) handler.postDelayed(this::replaceLauncher, 500);
    }

    private boolean sessionActive() {
        DriveAccessibilityService service = DriveAccessibilityService.current();
        return service != null && service.isDriving();
    }

    private void openAdjacent() {
        if (adjacentLaunched || isFinishing()) return;
        if (!sessionActive()) { finish(); return; }
        adjacentLaunched = true;
        try {
            // The second app occupies the adjacent (bottom/right) stage. This Activity
            // stays in the first stage until it can replace itself with the first app.
            startActivity(AppCatalog.launch(second, true));
            if (isInMultiWindowMode()) handler.postDelayed(this::replaceLauncher, 500);
        } catch (RuntimeException e) { failed(); }
    }

    @Override public void onMultiWindowModeChanged(boolean inMultiWindow, Configuration config) {
        super.onMultiWindowModeChanged(inMultiWindow, config);
        if (inMultiWindow && adjacentLaunched) handler.postDelayed(this::replaceLauncher, 500);
    }

    private void replaceLauncher() {
        if (replacementLaunched || isFinishing() || !isInMultiWindowMode()) return;
        if (!sessionActive()) { finish(); return; }
        replacementLaunched = true;
        try {
            // A normal NEW_TASK launch inherits the caller's split-screen stage.
            startActivity(AppCatalog.launch(first, false));
            handler.postDelayed(this::finish, 1200);
        } catch (RuntimeException e) { failed(); }
    }

    private void failed() {
        DriveAccessibilityService service = DriveAccessibilityService.current();
        if (service != null) service.pairLaunchFailed();
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putBoolean("adjacentLaunched", adjacentLaunched);
        out.putBoolean("replacementLaunched", replacementLaunched);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null); super.onDestroy();
    }
}
