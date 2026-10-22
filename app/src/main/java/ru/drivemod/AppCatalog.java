package ru.drivemod;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class AppCatalog {
    static final class Entry {
        final ComponentName component;
        final String label;
        final int category;
        Entry(ComponentName component, String label, int category) {
            this.component = component; this.label = label; this.category = category;
        }
        String id() { return component.flattenToString(); }
        String pkg() { return component.getPackageName(); }
        Drawable icon(Context context) {
            try { return context.getPackageManager().getActivityIcon(component); }
            catch (PackageManager.NameNotFoundException | SecurityException e) {
                return context.getDrawable(R.drawable.app_icon);
            }
        }
        boolean recommended(String role) {
            String s = (label + " " + pkg()).toLowerCase(Locale.ROOT);
            if (Prefs.NAV.equals(role)) return category == ApplicationInfo.CATEGORY_MAPS
                    || s.contains("navigator") || s.contains("navigation") || s.contains("maps")
                    || s.contains("навиг") || s.contains("карты") || s.contains("2gis") || s.contains("osmand");
            return category == ApplicationInfo.CATEGORY_AUDIO || s.contains("music")
                    || s.contains("музык") || s.contains("spotify") || s.contains("player")
                    || s.contains("плеер") || s.contains("radio") || s.contains("радио");
        }
    }
    static List<Entry> installed(Context context, boolean includeSelf) {
        PackageManager pm = context.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> matches = pm.queryIntentActivities(query, 0);
        LinkedHashMap<String, Entry> apps = new LinkedHashMap<>();
        for (ResolveInfo match : matches) {
            ActivityInfo a = match.activityInfo;
            if (!a.exported || !a.applicationInfo.enabled || !a.enabled) continue;
            if (!includeSelf && a.packageName.equals(context.getPackageName())) continue;
            ComponentName component = new ComponentName(a.packageName, a.name);
            Entry entry = new Entry(component, String.valueOf(match.loadLabel(pm)), a.applicationInfo.category);
            if (!apps.containsKey(a.packageName)) apps.put(a.packageName, entry);
            Intent frontDoor = pm.getLaunchIntentForPackage(a.packageName);
            if (frontDoor != null && component.equals(frontDoor.getComponent())) apps.put(a.packageName, entry);
        }
        List<Entry> result = new ArrayList<>(apps.values());
        Collator collator = Collator.getInstance(new Locale("ru"));
        result.sort((a, b) -> collator.compare(a.label, b.label));
        return result;
    }
    static Entry resolve(Context context, String id) {
        if (id == null || id.isEmpty()) return null;
        ComponentName component = ComponentName.unflattenFromString(id);
        if (component == null) return null;
        try {
            PackageManager pm = context.getPackageManager();
            ActivityInfo info = pm.getActivityInfo(component, 0);
            if (!info.exported || !info.enabled || !info.applicationInfo.enabled) return null;
            int enabled = pm.getComponentEnabledSetting(component);
            if (enabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || enabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || enabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) return null;
            int appEnabled = pm.getApplicationEnabledSetting(component.getPackageName());
            if (appEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || appEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || appEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) return null;
            return new Entry(component, String.valueOf(info.loadLabel(pm)), info.applicationInfo.category);
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException | SecurityException e) {
            return null;
        }
    }
    static Intent launch(Entry entry, boolean adjacent) {
        Intent intent = Intent.makeMainActivity(entry.component);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (adjacent) intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        return intent;
    }
}
