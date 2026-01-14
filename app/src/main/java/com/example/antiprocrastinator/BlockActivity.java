package com.example.antiprocrastinator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class BlockActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);

        Button btnContinue = findViewById(R.id.btn_continue);
        Button btnHome = findViewById(R.id.btn_home);

        BlockedAppsManager manager = new BlockedAppsManager(this);
        int delaySeconds = manager.getIntentionDelaySeconds();

        if (delaySeconds > 0) {
            btnContinue.setEnabled(false);
            new android.os.CountDownTimer(delaySeconds * 1000L, 1000) {
                public void onTick(long millisUntilFinished) {
                    btnContinue.setText("Wait " + (millisUntilFinished / 1000 + 1) + "s");
                }

                public void onFinish() {
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue to App");
                }
            }.start();
        }

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Determine which package we are unblocking.
                // For now, we just finish, and the Service will need to know not to block immediately again.
                // We can send a broadcast or use a shared preference/singleton to notify the service.
                
                // Let's use a static flag in the Service or a shared preference for simplicity in this MVP.
                // Actually, sending a broadcast is cleaner or just setting a global state.
                MonitoringService.sTemporarilyAllowedPackage = getIntent().getStringExtra("PACKAGE_NAME");
                MonitoringService.sAllowTime = System.currentTimeMillis();
                
                // Explicitly launch the app we just allowed to ensure we don't go back to Anti-Procrastinator
                String packageName = getIntent().getStringExtra("PACKAGE_NAME");
                if (packageName != null) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                         launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                         startActivity(launchIntent);
                    }
                }
                
                finish();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go to home screen
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
                finish();
            }
        });
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from bypassing the screen easily, or just go home
        super.onBackPressed();
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
