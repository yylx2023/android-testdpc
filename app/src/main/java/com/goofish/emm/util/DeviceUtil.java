package com.goofish.emm.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

public class DeviceUtil {
    @SuppressLint("NewApi")
    public static String getDeviceImei(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            imei = telephonyManager.getImei();
        }
        if (!TextUtils.isEmpty(imei)){
            return imei;
        }
        return Build.getSerial();
    }
}
