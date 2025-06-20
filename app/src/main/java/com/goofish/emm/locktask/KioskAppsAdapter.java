package com.goofish.emm.locktask;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afwsamples.testdpc.R;
import com.afwsamples.testdpc.common.Util;

import java.util.List;

/**
 * Kiosk模式应用网格适配器
 */
public class KioskAppsAdapter extends RecyclerView.Adapter<KioskAppsAdapter.AppViewHolder> {
    private static final String TAG = "KioskAppsAdapter";
    
    private List<AppInfo> appList;
    private Context context;
    private OnAppClickListener onAppClickListener;

    public interface OnAppClickListener {
        void onAppClick(AppInfo appInfo);
    }

    public KioskAppsAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
    }

    public void setOnAppClickListener(OnAppClickListener listener) {
        this.onAppClickListener = listener;
    }

    /**
     * 更新应用列表数据
     */
    public void updateAppList(List<AppInfo> newAppList) {
        if (newAppList != null) {
            this.appList.clear();
            this.appList.addAll(newAppList);
            notifyDataSetChanged();
            Log.d(TAG, "App list updated with " + newAppList.size() + " apps");
        }
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.kiosk_app_item, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        holder.bind(appInfo);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        private ImageView appIcon;
        private TextView appName;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && onAppClickListener != null) {
                        onAppClickListener.onAppClick(appList.get(position));
                    }
                }
            });
        }

        public void bind(AppInfo appInfo) {
            appIcon.setImageDrawable(appInfo.getAppIcon());
            appName.setText(appInfo.getAppName());
        }
    }

    /**
     * 启动应用
     */
    public static void launchApp(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent launchAppIntent;

        if (Util.isRunningOnTvDevice(context)) {
            launchAppIntent = pm.getLeanbackLaunchIntentForPackage(packageName);
        } else {
            launchAppIntent = pm.getLaunchIntentForPackage(packageName);
        }

        if (launchAppIntent == null) {
            Toast.makeText(context, "此应用无法打开", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Cannot launch app: " + packageName);
            return;
        }

        try {
            context.startActivity(launchAppIntent);
        } catch (Exception e) {
            Toast.makeText(context, "启动应用失败", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to launch app: " + packageName, e);
        }
    }
}
