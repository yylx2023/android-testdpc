package com.goofish.emm.util;

import android.content.Context;
import android.telephony.TelephonyManager;

public class DeviceUtil {
    public static String getDeviceImei(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            imei = telephonyManager.getImei();
        }
        return imei;
    }
}
