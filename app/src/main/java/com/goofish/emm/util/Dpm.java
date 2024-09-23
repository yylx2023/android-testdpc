package com.goofish.emm.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.goofish.emm.EmmApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Dpm {
    private static volatile Dpm instance;

    private ComponentName componentName;
    private DevicePolicyManager devicePolicyManager;

    public static final String KIOSK_PREFERENCE_FILE = "kiosk_preference_file";
    public static final String KIOSK_APPS_KEY = "kiosk_apps";

    private SharedPreferences preferences;

    private ArrayList<String> mKioskPackages = new ArrayList<>();

    private Dpm() {
        componentName = DeviceAdminReceiver.getComponentName(EmmApp.app);
        devicePolicyManager = (DevicePolicyManager) EmmApp.app.getSystemService(Context.DEVICE_POLICY_SERVICE);

        preferences = EmmApp.app.getSharedPreferences(KIOSK_PREFERENCE_FILE, Context.MODE_PRIVATE);
    }

    public static Dpm getInstance() {
        if (instance == null) {
            synchronized (Dpm.class) {
                if (instance == null) {
                    instance = new Dpm();
                }
            }
        }

        return instance;
    }

    public void addLockTask(String pkg) {
        String[] packages = devicePolicyManager.getLockTaskPackages(componentName);
        Set<String> set = new HashSet<>(Arrays.asList(packages));
        set.add(pkg);

        devicePolicyManager.setLockTaskPackages(componentName, set.toArray(new String[0]));
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }

    public ArrayList<String> getKioskPackages() {
        return new ArrayList<>(preferences.getStringSet(KIOSK_APPS_KEY, new HashSet<String>()));
    }

    public void addKioskPackages(List<String> packages) {
        HashSet<String> set = new HashSet<>();
        set.addAll(packages);
        set.addAll(getKioskPackages());
        preferences.edit().putStringSet(KIOSK_APPS_KEY, set).commit();
    }
}
