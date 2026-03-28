package com.goofish.emm.update;

import static android.os.UserManager.DISALLOW_INSTALL_APPS;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;


import android.content.ComponentName;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.common.PackageInstallationUtils;
import com.blankj.utilcode.util.AppUtils;
import com.goofish.emm.http.ApiService;
import com.goofish.emm.http.CommonRequest;
import com.goofish.emm.http.NetCallback;
import com.goofish.emm.http.NetworkManager;
import com.goofish.emm.http.Resp;
import com.goofish.emm.http.RetrofitClient;
import com.goofish.emm.http.VersionCheckResponse;
import com.goofish.emm.locktask.KioskConfig;
import com.goofish.emm.util.DeviceUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class SelfUpdateHelper {
    private static final String TAG = "SelfUpdateHelper";

    private SelfUpdateHelper() {}

    public static void checkVersionAndUpdate(@NonNull Activity activity) {
        checkVersionInternal(activity, false);
    }

    public static void checkVersionAndConfirmUpdate(@NonNull Activity activity) {
        checkVersionInternal(activity, true);
    }

    private static void checkVersionInternal(@NonNull Activity activity, boolean showConfirmDialog) {
        ApiService apiService = RetrofitClient.INSTANCE.getApiService();
        CommonRequest request = new CommonRequest(DeviceUtil.getDeviceImei(activity), AppUtils.getAppVersionCode());
        retrofit2.Call<Resp.Common<VersionCheckResponse>> call = apiService.versionCheck(request);
        NetworkManager.INSTANCE.makeRequest(call, new NetCallback<VersionCheckResponse>() {
            @Override
            public void onSuccess(@NonNull Resp.Common<VersionCheckResponse> resp, @NonNull byte[] data) {
                if (!Resp.SUCCESS.equals(resp.getCode())) {
                    toast(activity, "当前已经是最新版本~");
                    return;
                }
                VersionCheckResponse d = resp.getData();
                if (d == null || d.getApkUrl() == null || d.getApkUrl().isEmpty()) {
                    Log.e(TAG, "Invalid version response: apkUrl is empty");
                    toast(activity, "更新地址无效");
                    return;
                }
                Runnable startUpdate = () -> {
                    Log.i(TAG, "Start self-update download, version=" + d.getVersionName() + "(" + d.getVersionCode() + "), url=" + d.getApkUrl());
                    downloadAndInstallApk(activity, d.getApkUrl());
                };
                if (showConfirmDialog) {
                    activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                            .setTitle("发现新版本")
                            .setMessage("版本: " + d.getVersionName() + "\n大小: " + d.getSize() + "\n\n" + d.getUpgradeMsg())
                            .setCancelable(true)
                            .setNegativeButton("取消", null)
                            .setPositiveButton("立即更新", (dialog, which) -> startUpdate.run())
                            .show());
                } else {
                    startUpdate.run();
                }
            }

            @Override
            public void onNetError(int statusCode, @NonNull String msg) {
                Log.e(TAG, "Version check failed, code=" + statusCode + ", msg=" + msg);
                toast(activity, "网络异常请稍后重试~" + statusCode);
            }


        });
    }



    private static void downloadAndInstallApk(@NonNull Activity activity, @NonNull String apkUrl) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(apkUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Self-update download failed", e);
                toast(activity, "下载更新失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Self-update download failed, http=" + response.code());
                    toast(activity, "下载更新失败");
                    return;
                }
                File updateDir = new File(activity.getCacheDir(), "app_update_cache");
                if (!updateDir.exists() && !updateDir.mkdirs()) {
                    Log.e(TAG, "Failed to create update dir: " + updateDir.getAbsolutePath());
                    toast(activity, "创建更新目录失败");
                    return;
                }
                File apkFile = new File(updateDir, "appupdate.apk");
                try (InputStream in = response.body().byteStream();
                     FileOutputStream out = new FileOutputStream(apkFile, false)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
                Log.i(TAG, "Self-update apk downloaded: " + apkFile.getAbsolutePath() + ", size=" + apkFile.length());
                silentInstallApk(activity, apkFile);
            }
        });
    }

    private static void silentInstallApk(@NonNull Activity activity, @NonNull File apk) {
        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(Activity.DEVICE_POLICY_SERVICE);
        ComponentName admin = DeviceAdminReceiver.getComponentName(activity);
        try {
            if (KioskConfig.DISALLOW_INSTALL) {
                dpm.clearUserRestriction(admin, DISALLOW_INSTALL_APPS);
                Log.i(TAG, "Temporarily cleared DISALLOW_INSTALL_APPS for self-update");
            }
            FileInputStream fis = new FileInputStream(apk);
            boolean success = PackageInstallationUtils.installPackage(activity, fis, activity.getPackageName());
            Log.i(TAG, "Silent install commit sent, success=" + success);
        } catch (IOException e) {
            Log.e(TAG, "Silent install failed before commit", e);
            if (KioskConfig.DISALLOW_INSTALL) {
                dpm.addUserRestriction(admin, DISALLOW_INSTALL_APPS);
                Log.i(TAG, "Restored DISALLOW_INSTALL_APPS after pre-commit failure");
            }
        }
    }

    private static void toast(@NonNull Activity activity, @NonNull String msg) {
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }
}

