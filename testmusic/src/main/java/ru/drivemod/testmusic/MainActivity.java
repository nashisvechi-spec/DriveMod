package ru.drivemod.testmusic;
public class MainActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle b) {
        super.onCreate(b); android.widget.TextView text = new android.widget.TextView(this);
        text.setText("MUSIC\nExternal test application"); text.setTextSize(26); text.setTextColor(0xFFFFFFFF);
        text.setGravity(android.view.Gravity.CENTER); text.setBackgroundColor(0xFF282330); setContentView(text);
    }
}
