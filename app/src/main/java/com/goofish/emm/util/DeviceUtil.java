package com.goofish.emm.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import androidx.annotation.RequiresApi;

public class DeviceUtil {
    @SuppressLint("NewApi")
    public static String getDeviceImei(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = "";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                imei = telephonyManager.getImei();
            }
            if (!TextUtils.isEmpty(imei)) {
                return imei;
            }
            return Build.getSerial();
        } catch (Exception e) {
            return "xxxxxyyyyy";
        }

    }

    public static String generateCode(String sn) {
        return extractChars(getMD5Hash(sn)).toLowerCase();
    }

    // 计算MD5哈希值
    public static String getMD5Hash(String originalString) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(originalString.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    // 从MD5哈希值中提取特定位置的字符
    public static String extractChars(String md5Hash) {
        StringBuilder sb = new StringBuilder();
        sb.append(md5Hash.charAt(0)); // 第0位
        sb.append(md5Hash.charAt(8)); // 第8位
        sb.append(md5Hash.charAt(16)); // 第16位
        sb.append(md5Hash.charAt(24)); // 第24位
        // 注意：MD5哈希值是32位的，所以没有第32位字符，索引范围为0-31
        return sb.toString();
    }
}
