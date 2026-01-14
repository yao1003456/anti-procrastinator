package com.example.antiprocrastinator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    public static class AppInfo {
        String name;
        String packageName;
        Drawable icon;
        boolean isBlocked;
        
        public AppInfo(String name, String packageName, Drawable icon, boolean isBlocked) {
            this.name = name;
            this.packageName = packageName;
            this.icon = icon;
            this.isBlocked = isBlocked;
        }
    }

    private List<AppInfo> appList;
    private Context context;
    private BlockedAppsManager blockedAppsManager;

    public AppAdapter(Context context, List<ResolveInfo> rawApps) {
        this.context = context;
        this.blockedAppsManager = new BlockedAppsManager(context);
        this.appList = new ArrayList<>();
        
        PackageManager pm = context.getPackageManager();
        for (ResolveInfo ri : rawApps) {
            String name = ri.loadLabel(pm).toString();
            String packageName = ri.activityInfo.packageName;
            Drawable icon = ri.loadIcon(pm);
            boolean isBlocked = blockedAppsManager.isBlocked(packageName);
            appList.add(new AppInfo(name, packageName, icon, isBlocked));
        }

        // Sort alphabetically
        Collections.sort(appList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        holder.name.setText(appInfo.name);
        holder.icon.setImageDrawable(appInfo.icon);
        
        // Remove listener before setting state to avoid infinite callbacks if using OnCheckedChangeListener
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(appInfo.isBlocked);
        
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appInfo.isBlocked = isChecked;
                if (isChecked) {
                    blockedAppsManager.addBlock(appInfo.packageName);
                } else {
                    blockedAppsManager.removeBlock(appInfo.packageName);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView icon;
        CheckBox checkBox;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_app_name);
            icon = itemView.findViewById(R.id.iv_app_icon);
            checkBox = itemView.findViewById(R.id.cb_block);
        }
    }
}
