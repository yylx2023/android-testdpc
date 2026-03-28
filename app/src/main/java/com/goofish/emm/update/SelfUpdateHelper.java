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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class SelfUpdateHelper {
    private static final String TAG = "SelfUpdateHelper";
    private static volatile boolean sUpdateInProgress = false;
    private static AlertDialog sProgressDialog;
    private static TextView sDialogMessageView;
    private static LinearLayout sProgressContainer;
    private static ProgressBar sHorizontalProgressBar;
    private static TextView sProgressPercentView;

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
                    if (sUpdateInProgress) {
                        toast(activity, "更新任务正在进行中");
                        return;
                    }
                    sUpdateInProgress = true;
                    Log.i(TAG, "Start self-update download, version=" + d.getVersionName() + "(" + d.getVersionCode() + "), url=" + d.getApkUrl());
                    switchDialogToProgress(activity, "正在下载更新", 0);
                    downloadAndInstallApk(activity, d.getApkUrl());


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



    private static void downloadAndInstallApk(@NonNull Activity activity, @NonNull String apkUrl) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(apkUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Self-update download failed", e);
                dismissProgress(activity);
                sUpdateInProgress = false;
                toast(activity, "下载更新失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Self-update download failed, http=" + response.code());
                    dismissProgress(activity);
                    sUpdateInProgress = false;
                    toast(activity, "下载更新失败");
                    return;
                }
                File updateDir = new File(activity.getCacheDir(), "app_update_cache");
                if (!updateDir.exists() && !updateDir.mkdirs()) {
                    Log.e(TAG, "Failed to create update dir: " + updateDir.getAbsolutePath());
                    dismissProgress(activity);
                    sUpdateInProgress = false;
                    toast(activity, "创建更新目录失败");
                    return;
                }
                File apkFile = new File(updateDir, "appupdate.apk");
                long total = response.body().contentLength();
                long downloaded = 0;
                try (InputStream in = response.body().byteStream();
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
                Log.i(TAG, "Self-update apk downloaded: " + apkFile.getAbsolutePath() + ", size=" + apkFile.length());
                updateStatusText(activity, "正在安装更新", "安装包已下载完成，正在静默安装，请稍候...");

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
            dismissProgress(activity);
            sUpdateInProgress = false;
            if (KioskConfig.DISALLOW_INSTALL) {
                dpm.addUserRestriction(admin, DISALLOW_INSTALL_APPS);
                Log.i(TAG, "Restored DISALLOW_INSTALL_APPS after pre-commit failure");
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
            sProgressDialog = dialog;
            sDialogMessageView = messageView;
            sProgressContainer = progressContainer;
            sHorizontalProgressBar = progressBar;
            sProgressPercentView = percentView;
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

    private static void switchDialogToProgress(@NonNull Activity activity, @NonNull String title, int progress) {
        activity.runOnUiThread(() -> {
            if (sProgressDialog == null) {
                return;
            }
            sProgressDialog.setTitle(title);
            if (sProgressContainer != null) {
                sProgressContainer.setVisibility(View.VISIBLE);
            }
            if (sHorizontalProgressBar != null) {
                sHorizontalProgressBar.setProgress(progress);
            }
            if (sProgressPercentView != null) {
                sProgressPercentView.setText(progress + "%");
            }
        });
    }

    private static void updateProgress(@NonNull Activity activity, @NonNull String title, int progress) {
        activity.runOnUiThread(() -> {
            if (sProgressDialog == null) {
                return;
            }
            sProgressDialog.setTitle(title);
            if (sProgressContainer != null) {
                sProgressContainer.setVisibility(View.VISIBLE);
            }
            if (sHorizontalProgressBar != null) {
                sHorizontalProgressBar.setProgress(progress);
            }
            if (sProgressPercentView != null) {
                sProgressPercentView.setText(progress + "%");
            }
        });
    }

    private static void updateStatusText(@NonNull Activity activity, @NonNull String title, @NonNull String status) {
        activity.runOnUiThread(() -> {
            if (sProgressDialog == null) {
                return;
            }
            sProgressDialog.setTitle(title);
            if (sProgressContainer != null) {
                sProgressContainer.setVisibility(View.VISIBLE);
            }
            if (sDialogMessageView != null) {
                sDialogMessageView.setText(status);
            }
        });
    }

    public static void dismissProgress(@NonNull Activity activity) {
        activity.runOnUiThread(() -> {
            if (sProgressDialog != null && sProgressDialog.isShowing()) {
                sProgressDialog.dismiss();
            }
            sProgressDialog = null;
            sDialogMessageView = null;
            sProgressContainer = null;
            sHorizontalProgressBar = null;
            sProgressPercentView = null;
        });
    }

    private static int dp(@NonNull Activity activity, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics());
    }



    public static void onInstallFinished(boolean success) {
        sUpdateInProgress = false;
        sProgressDialog = null;
    }


    public static void onInstallFinished(@NonNull Activity activity, boolean success, @NonNull String message) {
        dismissProgress(activity);
        sUpdateInProgress = false;
        toast(activity, success ? "更新安装成功" : message);
    }

    private static void toast(@NonNull Activity activity, @NonNull String msg) {
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }


}

