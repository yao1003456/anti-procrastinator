# Anti-Procrastinator - Developer Documentation

**Version:** 1.0.0
**Date:** 2026-01-13

This document provides a comprehensive technical overview of the **Anti-Procrastinator** Android application. It covers the app's architecture, file responsibilities, detailed function descriptions, and the core operational workflow.

---

## 1. Project Overview

**Anti-Procrastinator** is a utility app designed to help users reduce screen time on specific distracting applications. It utilizes an **Accessibility Service** to monitor foreground activity and intervene when a blocked app is launched.

**Key Features:**
*   **Accessibility Service Monitoring:** Real-time detection of app switching.
*   **Block List Management:** User-selectable list of installed apps to block.
*   **Configurable Timeout:** Custom grace periods (temporarily allowing blocked apps).
*   **Theme Customization:** Support for Light, Dark, and System themes.
*   **Privacy Focused:** No internet permission; all processing is local.

---

## 2. File & Function Reference

### 2.1 `MainActivity.java`
**Location:** `app/src/main/java/com/example/antiprocrastinator/MainActivity.java`
**Purpose:** The primary user interface (Dashboard). It handles configuration, permission requests, and status visualization.

**Functions:**
*   `onCreate(Bundle savedInstanceState)`: Initializes the UI, instantiates `BlockedAppsManager`, applies the saved theme, and sets up button listeners (Timeout Save, Theme Toggle, Enable Service).
*   `loadInstalledApps()`: Spawns a background thread to query the `PackageManager` for all launchable apps, ensuring the UI remains responsive. Post-execution, it updates the `RecyclerView` on the main thread.
*   `onResume()`: Called when the activity enters the foreground. Triggers `updateServiceStatus()` to refresh the "Active/Inactive" status card.
*   `updateServiceStatus()`: visually updates the status text and "Enable" button visibility based on whether the Accessibility Service is running.
*   `isAccessibilityServiceEnabled(Context, Class)`: Static utility that checks Android system settings to see if the specific service ID is enabled.

### 2.2 `MonitoringService.java`
**Location:** `app/src/main/java/com/example/antiprocrastinator/MonitoringService.java`
**Purpose:** The core engine. An implementation of `AccessibilityService` that runs in the background to detect app usage.

**Functions:**
*   `onServiceConnected()`: Lifecycle method called when the service starts. Initializes the `BlockedAppsManager`.
*   `onAccessibilityEvent(AccessibilityEvent)`:  The main listening loop.
    *   Checks if event type is `TYPE_WINDOW_STATE_CHANGED`.
    *   Retrieves the current `packageName`.
    *   Checks `blockedAppsManager.isBlocked()`.
    *   Execute `shouldBlock()` logic.
    *   If blocked, launches `BlockActivity`.
*   `shouldBlock(String packageName)`: Determines if the blocking screen should appear. It checks if the package is "temporarily allowed" and if the "grace period" (allow time) has not yet expired.
*   `onInterrupt()`: Required override for handling service interruptions (system killing service, etc.).

### 2.3 `BlockActivity.java`
**Location:** `app/src/main/java/com/example/antiprocrastinator/BlockActivity.java`
**Purpose:** The full-screen blocking interface ("Stop Scrolling"). It intercepts the user's view.

**Functions:**
*   `onCreate(Bundle)`: Sets up the full-screen layout.
    *   **Continue Button**: Sets the target package as `sTemporarilyAllowedPackage` in `MonitoringService`, records the current time `sAllowTime`, and re-launches the target app.
    *   **Home Button**: Navigates user back to the Android Home Screen.
*   `onBackPressed()`: Overrides the physical back button to prevent bypassing. Redirects to Home Screen.

### 2.4 `BlockedAppsManager.java`
**Location:** `app/src/main/java/com/example/antiprocrastinator/BlockedAppsManager.java`
**Purpose:** A helper class for persistent data storage using `SharedPreferences`.

**Functions:**
*   `addBlock(String packageName)`: Adds a package to the blocked set and saves.
*   `removeBlock(String packageName)`: Removes a package from the blocked set and saves.
*   `isBlocked(String packageName)`: Returns `true` if the package is in the blocked set.
*   `getBlockedApps()`: Retrieves the `Set<String>` of all blocked packages.
*   `saveBlockedApps(Set<String>)`: Internal method to write changes to `SharedPreferences`.
*   `getTimeoutSeconds()` / `setTimeoutSeconds(int)`: Getters/Setters for the user's custom timeout preference.
*   `getThemeMode()` / `setThemeMode(int)`: Getters/Setters for the user's theme preference (Light/Dark/System).

### 2.5 `AppAdapter.java`
**Location:** `app/src/main/java/com/example/antiprocrastinator/AppAdapter.java`
**Purpose:** `RecyclerView.Adapter` to handle the efficient display of the app list.

**Functions:**
*   `AppAdapter(Context, List<ResolveInfo>)`: Constructor. Filters and converts raw system `ResolveInfo` into our internal `AppInfo` model, sorts them alphabetically, and checks blocked status.
*   `onCreateViewHolder(...)`: Inflates `item_app.xml`.
*   `onBindViewHolder(...)`: Binds data (Icon, Name, Checkbox) to the view. Sets up the Listener to update `BlockedAppsManager` when checked/unchecked.

---

## 3. Workflow Diagram

```mermaid
graph TD
    User-->|Opens App| Main[MainActivity]
    Main-->|Selects Apps| BAM[BlockedAppsManager]
    Main-->|Sets Timeout| BAM
    Main-->|Enables Service| SystemSettings

    System[Android System]-->|Window State Change| Service[MonitoringService]
    
    Service-->|Get Blocked List| BAM
    Service-->|Is Blocked?| Decision{Should Block?}
    
    Decision-->|No (Safe)| Pass[Do Nothing]
    Decision-->|Yes (Blocked)| Grace{Grace Period Active?}
    
    Grace-->|Yes| Pass
    Grace-->|No| Block[Launch BlockActivity]
    
    Block-->|User Clicks Continue| Allow[Set Grace Period & Re-Open App]
    Block-->|User Clicks Home| Home[Go to Home Screen]
```
