package com.goofish.emm;

import com.afwsamples.testdpc.PolicyManagementActivity;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

public class VolumeButtonService extends Service {

    private static final int MAX_CLICK_DURATION = 1000; // 毫秒
    private static final int CLICK_COUNT = 3;

    private int volumeUpClickCount = 0;
    private long lastClickTime = 0;

    private BroadcastReceiver volumeUpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (VolumeButtonReceiver.VOLUME_UP_ACTION.equals(intent.getAction())) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < MAX_CLICK_DURATION) {
                    volumeUpClickCount++;
                    if (volumeUpClickCount == CLICK_COUNT) {
                        volumeUpClickCount = 0;
                        lastClickTime = 0;
                        // 启动新的 Activity
                        Intent newActivityIntent = new Intent(context, PolicyManagementActivity.class);
                        newActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(newActivityIntent);
                    }
                } else {
                    volumeUpClickCount = 1;
                }
                lastClickTime = currentTime;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册广播接收器
        IntentFilter filter = new IntentFilter(VolumeButtonReceiver.VOLUME_UP_ACTION);
        registerReceiver(volumeUpReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        unregisterReceiver(volumeUpReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}