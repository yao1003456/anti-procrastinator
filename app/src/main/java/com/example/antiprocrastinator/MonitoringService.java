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

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d("MonitoringService", "Service Connected");
        blockedAppsManager = new BlockedAppsManager(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            
            // Re-instantiate or just use the manager. The manager reads from SP.
            // For real-time updates from the UI, reading from SP is the easiest way to ensure we have the latest data.
            // Optimization: The SharedPreference object internally handles caching and listener updates, 
            // but `getStringSet` creates a new Set copy. 
            if (blockedAppsManager.isBlocked(packageName)) {
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
            if (System.currentTimeMillis() - sAllowTime < gracePeriodMs) {
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
