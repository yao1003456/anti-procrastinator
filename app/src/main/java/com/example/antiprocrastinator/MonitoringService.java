package com.example.antiprocrastinator;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class MonitoringService extends AccessibilityService {

    public static String sTemporarilyAllowedPackage = "";
    public static long sAllowTime = 0;

    private BlockedAppsManager blockedAppsManager;
    private android.content.SharedPreferences.OnSharedPreferenceChangeListener listener;
    private Set<String> cachedBlockedPackages = new HashSet<>();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d("MonitoringService", "Service Connected");
        blockedAppsManager = new BlockedAppsManager(this);
        
        // Optimization: Cache the blocked packages to avoid reading from SharedPreferences (and creating new Set objects)
        // on every single window state change event.
        updateBlockedPackagesCache();
        
        android.content.SharedPreferences prefs = getSharedPreferences(BlockedAppsManager.PREF_NAME, MODE_PRIVATE);
        listener = new android.content.SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(android.content.SharedPreferences sharedPreferences, String key) {
                if (BlockedAppsManager.KEY_BLOCKED_PACKAGES.equals(key)) {
                    updateBlockedPackagesCache();
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    private void updateBlockedPackagesCache() {
        // Create a new copy to ensure thread safety if accessed concurrently (though main thread is primary)
        cachedBlockedPackages = new HashSet<>(blockedAppsManager.getBlockedApps());
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            getSharedPreferences(BlockedAppsManager.PREF_NAME, MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            
            // Optimization: Check against local cache instead of asking manager (which asks SP)
            if (cachedBlockedPackages.contains(packageName)) {
                if (shouldBlock(packageName)) {
                    // Log.d("MonitoringService", "Blocking package: " + packageName);
                    Intent intent = new Intent(this, BlockActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("PACKAGE_NAME", packageName);
                    startActivity(intent);
                }
            }
        }
    }

    private boolean shouldBlock(String packageName) {
        if (packageName.equals(sTemporarilyAllowedPackage)) {
            long gracePeriodMs = blockedAppsManager.getTimeoutSeconds() * 1000L;
            // Bug Fix: Refresh the session as long as the user is "active" (navigating)
            // This changes the logic from "Time since unlock" to "Time since last activity"
            if (System.currentTimeMillis() - sAllowTime < gracePeriodMs) {
                sAllowTime = System.currentTimeMillis(); 
                return false; // Still in grace period
            }
        }
        return true;
    }

    @Override
    public void onInterrupt() {
        Log.d("MonitoringService", "Service Interrupted");
    }
}
