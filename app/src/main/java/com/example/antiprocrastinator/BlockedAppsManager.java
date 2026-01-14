package com.example.antiprocrastinator;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class BlockedAppsManager {
    public static final String PREF_NAME = "blocked_apps_pref";
    public static final String KEY_BLOCKED_PACKAGES = "blocked_packages";

    private SharedPreferences sharedPreferences;

    public BlockedAppsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void addBlock(String packageName) {
        Set<String> blocked = getBlockedApps();
        blocked.add(packageName);
        saveBlockedApps(blocked);
    }

    public void removeBlock(String packageName) {
        Set<String> blocked = getBlockedApps();
        blocked.remove(packageName);
        saveBlockedApps(blocked);
    }

    public boolean isBlocked(String packageName) {
        return getBlockedApps().contains(packageName);
    }

    public Set<String> getBlockedApps() {
        return sharedPreferences.getStringSet(KEY_BLOCKED_PACKAGES, new HashSet<>());
    }

    private void saveBlockedApps(Set<String> blocked) {
        sharedPreferences.edit().putStringSet(KEY_BLOCKED_PACKAGES, blocked).apply();
    }
    
    public int getTimeoutSeconds() {
        return sharedPreferences.getInt("pref_timeout_seconds", 60);
    }
    
    public void setTimeoutSeconds(int seconds) {
        sharedPreferences.edit().putInt("pref_timeout_seconds", seconds).apply();
    }
    
    public int getThemeMode() {
        return sharedPreferences.getInt("pref_theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setThemeMode(int mode) {
        sharedPreferences.edit().putInt("pref_theme_mode", mode).apply();
    }

    public int getIntentionDelaySeconds() {
        return sharedPreferences.getInt("pref_intention_delay_seconds", 0);
    }

    public void setIntentionDelaySeconds(int seconds) {
        sharedPreferences.edit().putInt("pref_intention_delay_seconds", seconds).apply();
    }
}
