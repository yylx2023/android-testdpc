package com.goofish.emm.tutu;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.Build;

import java.security.Permission;

public class TutuUtil {
    public static final String TUTU_PKG = "com.doutu.tutupad";

    public static void grantPermission(DevicePolicyManager devicePolicyManager, ComponentName cn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }


        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission_group.PHONE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission_group.LOCATION, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission_group.CAMERA, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission_group.MICROPHONE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission_group.STORAGE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.NEARBY_WIFI_DEVICES, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.POST_NOTIFICATIONS, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.CAMERA, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.CALL_PHONE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.READ_PHONE_STATE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.ACCESS_FINE_LOCATION, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.ACCESS_BACKGROUND_LOCATION, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.ACCESS_COARSE_LOCATION, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.RECORD_AUDIO, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.READ_MEDIA_AUDIO, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.READ_MEDIA_VIDEO, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.READ_MEDIA_IMAGES, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.READ_EXTERNAL_STORAGE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.WRITE_EXTERNAL_STORAGE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.BLUETOOTH_ADVERTISE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.BLUETOOTH_SCAN, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        devicePolicyManager.setPermissionGrantState(cn, TUTU_PKG, Manifest.permission.BLUETOOTH_CONNECT, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
    }
}
