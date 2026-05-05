package com.goofish.emm.update;

import static android.os.UserManager.DISALLOW_INSTALL_APPS;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class SelfUpdateHelper {
    private static final String TAG = "SelfUpdateHelper";

    /** 原子布尔，防止并发启动多个更新任务 */
    private static final AtomicBoolean sUpdateInProgress = new AtomicBoolean(false);

    /** 使用 WeakReference 持有 Dialog/View，避免 Activity 内存泄漏 */
    private static WeakReference<AlertDialog> sProgressDialogRef;
    private static WeakReference<TextView> sDialogMessageViewRef;
    private static WeakReference<LinearLayout> sProgressContainerRef;
    private static WeakReference<ProgressBar> sHorizontalProgressBarRef;
    private static WeakReference<TextView> sProgressPercentViewRef;

    /** 复用下载用的 OkHttpClient，避免重复创建 */
    private static volatile OkHttpClient sDownloadClient;

    private SelfUpdateHelper() {}

    private static OkHttpClient getDownloadClient() {
        if (sDownloadClient == null) {
            synchronized (SelfUpdateHelper.class) {
                if (sDownloadClient == null) {
                    sDownloadClient = new OkHttpClient.Builder().build();
                }
            }
        }
        return sDownloadClient;
    }





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
                    Log.i(TAG, "Version check finished without update, code=" + resp.getCode() + ", msg=" + resp.getMsg());
                    if (showConfirmDialog) {
                        String toastMsg = resp.getMsg();
                        if (toastMsg == null || toastMsg.trim().isEmpty()) {
                            toastMsg = "当前已经是最新版本~";
                        }
                        toast(activity, toastMsg);
                    }
                    return;
                }
                VersionCheckResponse d = resp.getData();
                if (d == null || d.getApkUrl() == null || d.getApkUrl().isEmpty()) {
                    Log.e(TAG, "Invalid version response: apkUrl is empty");
                    toast(activity, "更新地址无效");
                    return;
                }
                Runnable startUpdate = () -> {
                    // 使用 CAS 原子操作防止并发启动多个更新任务
                    if (!sUpdateInProgress.compareAndSet(false, true)) {
                        toast(activity, "更新任务正在进行中");
                        return;
                    }
                    Log.i(TAG, "Start self-update download, version=" + d.getVersionName() + "(" + d.getVersionCode() + "), url=" + d.getApkUrl());
                    switchDialogToProgress(activity, "正在下载更新", 0);
                    downloadAndInstallApk(activity, d.getApkUrl(), d.getMd5());
                };
                if (showConfirmDialog) {
                    activity.runOnUiThread(() -> showConfirmDialog(activity,
                            "当前版本: " + AppUtils.getAppVersionName() + "\n新版本: " + d.getVersionName() + "\n大小: " + d.getSize() + "\n\n更新内容:\n" + d.getUpgradeMsg(),
                            startUpdate));
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



    private static void downloadAndInstallApk(@NonNull Activity activity, @NonNull String apkUrl, @NonNull String expectedMd5) {
        OkHttpClient client = getDownloadClient();
        Request request = new Request.Builder().url(apkUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Self-update download failed", e);
                sUpdateInProgress.set(false);
                dismissProgress(activity);
                toast(activity, "下载更新失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Self-update download failed, http=" + response.code());
                    sUpdateInProgress.set(false);
                    dismissProgress(activity);
                    toast(activity, "下载更新失败");
                    return;
                }
                // 使用 try-with-resources 确保 ResponseBody 被关闭
                try (okhttp3.ResponseBody body = response.body()) {
                    File updateDir = new File(activity.getCacheDir(), "app_update_cache");
                    if (!updateDir.exists() && !updateDir.mkdirs()) {
                        Log.e(TAG, "Failed to create update dir: " + updateDir.getAbsolutePath());
                        sUpdateInProgress.set(false);
                        dismissProgress(activity);
                        toast(activity, "创建更新目录失败");
                        return;
                    }
                    File apkFile = new File(updateDir, "appupdate.apk");
                    long total = body.contentLength();
                    long downloaded = 0;
                    try (InputStream in = body.byteStream();
                         FileOutputStream out = new FileOutputStream(apkFile, false)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        int lastPercent = -1;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                            downloaded += len;
                            if (total > 0) {
                                int percent = (int) ((downloaded * 100) / total);
                                if (percent != lastPercent) {
                                    lastPercent = percent;
                                    updateProgress(activity, "正在下载更新", percent);
                                }
                            }
                        }
                        out.flush();
                    }

                    // MD5 完整性校验
                    if (expectedMd5 != null && !expectedMd5.isEmpty()) {
                        String actualMd5 = calculateMd5(apkFile);
                        if (!expectedMd5.equalsIgnoreCase(actualMd5)) {
                            Log.e(TAG, "MD5 mismatch! expected=" + expectedMd5 + ", actual=" + actualMd5);
                            sUpdateInProgress.set(false);
                            dismissProgress(activity);
                            toast(activity, "安装包校验失败，请重试");
                            if (!apkFile.delete()) {
                                Log.w(TAG, "Failed to delete corrupted apk: " + apkFile.getAbsolutePath());
                            }
                            return;
                        }
                        Log.i(TAG, "MD5 verification passed: " + actualMd5);
                    } else {
                        Log.w(TAG, "No MD5 provided in version response, skipping integrity check");
                    }

                    Log.i(TAG, "Self-update apk downloaded: " + apkFile.getAbsolutePath() + ", size=" + apkFile.length());
                    updateStatusText(activity, "正在安装更新", "安装包已下载完成，正在静默安装，请稍候...");
                    silentInstallApk(activity, apkFile);
                }
            }
        });
    }

    /**
     * 计算文件的 MD5 哈希值。
     */
    private static String calculateMd5(@NonNull File file) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 algorithm not available", e);
            return "";
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read file for MD5: " + file.getAbsolutePath(), e);
            return "";
        }
        byte[] md5Bytes = digest.digest();
        BigInteger bigInt = new BigInteger(1, md5Bytes);
        StringBuilder sb = new StringBuilder(bigInt.toString(16));
        // 补齐前导零
        while (sb.length() < 32) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }



    private static void silentInstallApk(@NonNull Activity activity, @NonNull File apk) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity is finishing/destroyed, aborting silent install");
            sUpdateInProgress.set(false);
            return;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(Activity.DEVICE_POLICY_SERVICE);
        ComponentName admin = DeviceAdminReceiver.getComponentName(activity);
        try {
            if (KioskConfig.DISALLOW_INSTALL) {
                dpm.clearUserRestriction(admin, DISALLOW_INSTALL_APPS);
                Log.i(TAG, "Temporarily cleared DISALLOW_INSTALL_APPS for self-update");
            }
            // 使用 try-with-resources 确保 FileInputStream 被关闭
            try (FileInputStream fis = new FileInputStream(apk)) {
                boolean success = PackageInstallationUtils.installPackage(
                        activity,
                        fis,
                        activity.getPackageName(),
                        PackageInstallationUtils.INSTALL_SOURCE_SELF_UPDATE);
                Log.i(TAG, "Silent install commit sent, success=" + success);
            }
        } catch (IOException e) {
            Log.e(TAG, "Silent install failed before commit", e);
            sUpdateInProgress.set(false);
            dismissProgress(activity);
            if (KioskConfig.DISALLOW_INSTALL) {
                try {
                    dpm.addUserRestriction(admin, DISALLOW_INSTALL_APPS);
                    Log.i(TAG, "Restored DISALLOW_INSTALL_APPS after pre-commit failure");
                } catch (Exception restoreError) {
                    Log.e(TAG, "Failed to restore DISALLOW_INSTALL_APPS after pre-commit failure", restoreError);
                }
            }
            toast(activity, "安装更新失败");
        }
    }

    private static void showConfirmDialog(@NonNull Activity activity, @NonNull String message, @NonNull Runnable startUpdate) {
        dismissProgress(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(activity, 20);
        root.setPadding(padding, dp(activity, 12), padding, dp(activity, 8));

        TextView messageView = new TextView(activity);
        messageView.setText(message);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        root.addView(messageView);

        LinearLayout progressContainer = new LinearLayout(activity);
        progressContainer.setOrientation(LinearLayout.VERTICAL);
        progressContainer.setVisibility(View.GONE);
        progressContainer.setPadding(0, dp(activity, 16), 0, 0);

        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressContainer.addView(progressBar,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView percentView = new TextView(activity);
        percentView.setText("0%");
        percentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        percentView.setPadding(0, dp(activity, 8), 0, 0);
        progressContainer.addView(percentView);

        root.addView(progressContainer);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("发现新版本")
                .setView(root)
                .setCancelable(true)
                .setNegativeButton("取消", null)
                .setPositiveButton("立即更新", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            sProgressDialogRef = new WeakReference<>(dialog);
            sDialogMessageViewRef = new WeakReference<>(messageView);
            sProgressContainerRef = new WeakReference<>(progressContainer);
            sHorizontalProgressBarRef = new WeakReference<>(progressBar);
            sProgressPercentViewRef = new WeakReference<>(percentView);
            dialog.setCancelable(false);
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            }
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
            startUpdate.run();
        }));
        dialog.show();
    }

    /**
     * 合并了原 switchDialogToProgress，逻辑完全一致，避免代码重复。
     */
    private static void switchDialogToProgress(@NonNull Activity activity, @NonNull String title, int progress) {
        updateProgress(activity, title, progress);
    }

    private static void updateProgress(@NonNull Activity activity, @NonNull String title, int progress) {
        activity.runOnUiThread(() -> {
            AlertDialog dialog = sProgressDialogRef != null ? sProgressDialogRef.get() : null;
            if (dialog == null) {
                return;
            }
            dialog.setTitle(title);
            LinearLayout container = sProgressContainerRef != null ? sProgressContainerRef.get() : null;
            if (container != null) {
                container.setVisibility(View.VISIBLE);
            }
            ProgressBar bar = sHorizontalProgressBarRef != null ? sHorizontalProgressBarRef.get() : null;
            if (bar != null) {
                bar.setProgress(progress);
            }
            TextView percent = sProgressPercentViewRef != null ? sProgressPercentViewRef.get() : null;
            if (percent != null) {
                percent.setText(progress + "%");
            }
        });
    }

    private static void updateStatusText(@NonNull Activity activity, @NonNull String title, @NonNull String status) {
        activity.runOnUiThread(() -> {
            AlertDialog dialog = sProgressDialogRef != null ? sProgressDialogRef.get() : null;
            if (dialog == null) {
                return;
            }
            dialog.setTitle(title);
            LinearLayout container = sProgressContainerRef != null ? sProgressContainerRef.get() : null;
            if (container != null) {
                container.setVisibility(View.VISIBLE);
            }
            TextView messageView = sDialogMessageViewRef != null ? sDialogMessageViewRef.get() : null;
            if (messageView != null) {
                messageView.setText(status);
            }
        });
    }

    public static void dismissProgress(@NonNull Activity activity) {
        activity.runOnUiThread(() -> {
            AlertDialog dialog = sProgressDialogRef != null ? sProgressDialogRef.get() : null;
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            clearDialogRefs();
        });
    }

    private static void clearDialogRefs() {
        sProgressDialogRef = null;
        sDialogMessageViewRef = null;
        sProgressContainerRef = null;
        sHorizontalProgressBarRef = null;
        sProgressPercentViewRef = null;
    }

    private static int dp(@NonNull Activity activity, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics());
    }



    public static void onInstallFinished(boolean success) {
        sUpdateInProgress.set(false);
        // 先尝试 dismiss 再清引用，防止对话框残留
        AlertDialog dialog = sProgressDialogRef != null ? sProgressDialogRef.get() : null;
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception e) {
                Log.w(TAG, "Failed to dismiss dialog in onInstallFinished", e);
            }
        }
        clearDialogRefs();
    }


    public static void onInstallFinished(@NonNull Activity activity, boolean success, @NonNull String message) {
        dismissProgress(activity);
        sUpdateInProgress.set(false);
        toast(activity, success ? "更新安装成功" : message);
    }

    private static void toast(@NonNull Activity activity, @NonNull String msg) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity is finishing/destroyed, skipping toast: " + msg);
            return;
        }
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


}