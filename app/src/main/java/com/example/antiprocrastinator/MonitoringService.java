package com.example.antiprocrastinator;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class MonitoringService extends AccessibilityService {

    public static String sTemporarilyAllowedPackage = "";
    public static long sLastExitTime = 0;

    private BlockedAppsManager blockedAppsManager;
    private android.content.SharedPreferences.OnSharedPreferenceChangeListener listener;
    private Set<String> cachedBlockedPackages = new HashSet<>();
    private boolean isOccupyingAllowedApp = false;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d("MonitoringService", "Service Connected");
        blockedAppsManager = new BlockedAppsManager(this);

        // Optimization: Cache the blocked packages to avoid reading from
        // SharedPreferences (and creating new Set objects)
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
        // Create a new copy to ensure thread safety if accessed concurrently (though
        // main thread is primary)
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

            // Check if we are interacting with the temporarily allowed app
            if (packageName.equals(sTemporarilyAllowedPackage)) {
                if (shouldBlock(packageName)) {
                    isOccupyingAllowedApp = false; // Blocked, so technically not occupying successfully
                    Intent intent = new Intent(this, BlockActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("PACKAGE_NAME", packageName);
                    startActivity(intent);
                } else {
                    // Allowed to be here
                    isOccupyingAllowedApp = true;
                }
            } else {
                // We are in some other app.
                // If it's NOT our own BlockActivity, then we consider this an "Exit".
                if (!packageName.equals(getPackageName())) {
                    if (isOccupyingAllowedApp) {
                        sLastExitTime = System.currentTimeMillis();
                        isOccupyingAllowedApp = false;
                    }
                }

                // Also check if this 'other app' is in the blocked list (standard blocking
                // logic)
                if (cachedBlockedPackages.contains(packageName)) {
                    // Since packageName != sTemporarilyAllowedPackage (checked in first if), this
                    // is a blocked app.
                    // (Unless sTemporarilyAllowedPackage is empty, which matches nothing).

                    // Exception: If we just switched FROM the Allowed Package TO a Blocked Package?
                    // Logic above handles "Exit".
                    // Now we block this new package.
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
            // usage: If we are already occupying, we are fine (user changing screens inside
            // app).
            if (isOccupyingAllowedApp) {
                return false;
            }

            // If re-entering, check time since last exit
            long gracePeriodMs = blockedAppsManager.getTimeoutSeconds() * 1000L;
            if (System.currentTimeMillis() - sLastExitTime < gracePeriodMs) {
                return false; // Within grace period
            }
        }
        return true;
    }

    @Override
    public void onInterrupt() {
        Log.d("MonitoringService", "Service Interrupted");
    }
}
