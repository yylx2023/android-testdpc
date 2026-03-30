package com.goofish.emm.locktask;

import static android.os.UserManager.DISALLOW_INSTALL_APPS;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;
import android.widget.Toast;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.common.PackageInstallationUtils;
import com.goofish.emm.update.SelfUpdateHelper;



/**
 * 处理自升级安装完成回调。
 *
 * <p>由于当前应用升级自己时，进程可能被杀掉/重启，因此不能依赖 Activity 中动态注册的 receiver。
 * 这里使用 Manifest 静态注册的 BroadcastReceiver，在安装完成后恢复 DISALLOW_INSTALL_APPS 限制。
 */
public class SelfUpdateInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "SelfUpdateInstallRx";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !PackageInstallationUtils.ACTION_INSTALL_COMPLETE.equals(intent.getAction())) {
            return;
        }

        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        if (packageName != null && !context.getPackageName().equals(packageName)) {
            Log.w(TAG, "Ignore install callback for other package: " + packageName);
            return;
        }

        Log.i(TAG, "Install complete callback, status=" + status
                + ", package=" + packageName + ", message=" + message);

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            Log.w(TAG, "Self-update install requires user action: " + confirmIntent);
            if (confirmIntent != null) {
                confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(confirmIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch install confirmation", e);
                    restoreInstallRestriction(context);
                    SelfUpdateHelper.onInstallFinished(false);
                    Toast.makeText(context, "无法打开安装确认页面", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Install confirmation intent is null");
                restoreInstallRestriction(context);
                SelfUpdateHelper.onInstallFinished(false);
                Toast.makeText(context, "安装确认页面为空", Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(context, "安装需要用户确认", Toast.LENGTH_SHORT).show();
            return;
        }

        restoreInstallRestriction(context);
        boolean success = status == PackageInstaller.STATUS_SUCCESS;
        SelfUpdateHelper.onInstallFinished(success);

        if (success) {
            Log.i(TAG, "Self-update install success");
            Toast.makeText(context, "更新安装成功", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.e(TAG, "Self-update install failed, status=" + status + ", msg=" + message);
        Toast.makeText(context,
                "更新安装失败: " + (message == null ? String.valueOf(status) : message),
                Toast.LENGTH_SHORT).show();
    }



    private void restoreInstallRestriction(Context context) {
        if (!KioskConfig.DISALLOW_INSTALL) {
            return;
        }
        try {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName admin = DeviceAdminReceiver.getComponentName(context);
            dpm.addUserRestriction(admin, DISALLOW_INSTALL_APPS);
            Log.i(TAG, "Restored DISALLOW_INSTALL_APPS after self-update callback");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore DISALLOW_INSTALL_APPS", e);
        }
    }
}

