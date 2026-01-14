package com.example.antiprocrastinator;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView statusTextView;
    private Button enableServiceButton;
    private androidx.recyclerview.widget.RecyclerView rvApps;
    private android.widget.EditText etTimeout;
    private Button btnSaveTimeout;
    private BlockedAppsManager blockedAppsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        blockedAppsManager = new BlockedAppsManager(this);
        
        // Apply Theme
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(blockedAppsManager.getThemeMode());

        statusTextView = findViewById(R.id.tv_status);
        enableServiceButton = findViewById(R.id.btn_enable_service);
        rvApps = findViewById(R.id.rv_apps);
        etTimeout = findViewById(R.id.et_timeout);
        btnSaveTimeout = findViewById(R.id.btn_save_timeout);
        com.google.android.material.button.MaterialButton btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        
        btnThemeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentMode = blockedAppsManager.getThemeMode();
                int newMode;
                String modeName;
                
                if (currentMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) {
                    newMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
                    modeName = "Dark Mode";
                } else if (currentMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) {
                    newMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    modeName = "System Default";
                } else {
                    newMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
                    modeName = "Light Mode";
                }
                
                blockedAppsManager.setThemeMode(newMode);
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newMode);
                Toast.makeText(MainActivity.this, "Theme: " + modeName, Toast.LENGTH_SHORT).show();
            }
        });

        enableServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        // Timeout Logic
        etTimeout.setText(String.valueOf(blockedAppsManager.getTimeoutSeconds()));
        btnSaveTimeout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int seconds = Integer.parseInt(etTimeout.getText().toString());
                    blockedAppsManager.setTimeoutSeconds(seconds);
                    Toast.makeText(MainActivity.this, "Timeout saved!", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid number", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        loadInstalledApps();
    }

    private void loadInstalledApps() {
        // Optimization: Move heavy package manager query to background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                final List<android.content.pm.ResolveInfo> pkgAppsList = getPackageManager().queryIntentActivities(mainIntent, 0);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rvApps.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(MainActivity.this));
                        AppAdapter adapter = new AppAdapter(MainActivity.this, pkgAppsList);
                        rvApps.setAdapter(adapter);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private void updateServiceStatus() {
        if (isAccessibilityServiceEnabled(this, MonitoringService.class)) {
            statusTextView.setText("Service is ACTIVE. You are being protected.");
            enableServiceButton.setVisibility(View.GONE);
        } else {
            statusTextView.setText("Service is INACTIVE. Please enable it to start.");
            enableServiceButton.setVisibility(View.VISIBLE);
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getInstalledAccessibilityServiceList();
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            // We need to check if it's actually enabled, referencing the ID
            // Simple way: check the string setting
            String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return prefString != null && prefString.contains(context.getPackageName() + "/" + service.getName());
        }
        return false;
    }
}
