package com.goofish.emm.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.goofish.emm.http.DeviceInfoRequest;

/**
 * 设备信息辅助类
 * 用于收集和上报设备信息
 */
public class DeviceInfoHelper {
    private static final String TAG = "DeviceInfoHelper";

    /**
     * 获取设备 SN（序列号）
     * 优先获取 IMEI，如果获取失败则使用 Build.getSerial()
     */
    public static String getDeviceSN(Context context) {
        return DeviceUtil.getDeviceImei(context);
    }

    /**
     * 获取设备型号
     * 例如：Pixel 5, SM-G991B
     */
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    /**
     * 获取 Android 版本
     * 例如：13, 12, 11
     */
    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * 获取 Android SDK 版本
     * 例如：33 (Android 13), 31 (Android 12), 30 (Android 11)
     */
    public static int getAndroidSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取应用版本名称
     * 例如：V1.0.0-12011200-123-abc123
     */
    public static String getAppVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get app version name", e);
            return "unknown";
        }
    }

    /**
     * 获取应用版本号
     * 例如：123
     */
    public static int getAppVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get app version code", e);
            return 0;
        }
    }

    /**
     * 创建设备信息请求对象
     * 包含所有需要上报的设备信息
     */
    public static DeviceInfoRequest createDeviceInfoRequest(Context context) {
        String sn = getDeviceSN(context);
        String model = getDeviceModel();
        String androidVersion = getAndroidVersion();
        int androidSdkVersion = getAndroidSdkVersion();
        String appVersionName = getAppVersionName(context);
        int appVersionCode = getAppVersionCode(context);

        Log.i(TAG, "Device Info:");
        Log.i(TAG, "  SN: " + sn);
        Log.i(TAG, "  Model: " + model);
        Log.i(TAG, "  Android Version: " + androidVersion);
        Log.i(TAG, "  Android SDK Version: " + androidSdkVersion);
        Log.i(TAG, "  App Version Name: " + appVersionName);
        Log.i(TAG, "  App Version Code: " + appVersionCode);

        return new DeviceInfoRequest(
                sn,
                model,
                androidVersion,
                androidSdkVersion,
                appVersionName,
                appVersionCode
        );
    }

    /**
     * 获取设备信息摘要（用于日志）
     */
    public static String getDeviceInfoSummary(Context context) {
        return String.format(
                "Device[SN=%s, Model=%s, Android=%s(SDK:%d), App=%s(%d)]",
                getDeviceSN(context),
                getDeviceModel(),
                getAndroidVersion(),
                getAndroidSdkVersion(),
                getAppVersionName(context),
                getAppVersionCode(context)
        );
    }
}

