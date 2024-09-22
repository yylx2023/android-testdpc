package com.goofish.emm.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.goofish.emm.EmmApp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Dpm {
    private static volatile Dpm instance;

    private ComponentName componentName;
    private DevicePolicyManager devicePolicyManager;

    private Dpm(){
        componentName = DeviceAdminReceiver.getComponentName(EmmApp.app);
        devicePolicyManager = (DevicePolicyManager) EmmApp.app.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public static  Dpm getInstance(){
        if (instance == null){
            synchronized (Dpm.class){
                if (instance == null){
                    instance = new Dpm();
                }
            }
        }

        return instance;
    }

    public void addLockTask(String pkg){
        String[] packages = devicePolicyManager.getLockTaskPackages(componentName);
        Set<String> set = new HashSet<>(Arrays.asList(packages));
        set.add(pkg);

        devicePolicyManager.setLockTaskPackages(componentName, set.toArray(new String[0]));
    }
}
