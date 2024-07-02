/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goofish.emm.locktask;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.PolicyManagementActivity;
import com.afwsamples.testdpc.R;
import com.afwsamples.testdpc.common.Util;
import com.azhon.appupdate.manager.DownloadManager;
import com.blankj.utilcode.util.AppUtils;
import com.goofish.emm.EmmApp;
import com.goofish.emm.EmmDebugActivity;
import com.goofish.emm.http.ApiService;
import com.goofish.emm.http.NetCallback;
import com.goofish.emm.http.NetworkManager;
import com.goofish.emm.http.Resp;
import com.goofish.emm.http.RetrofitClient;
import com.goofish.emm.http.VersionCheckRequest;
import com.goofish.emm.http.VersionCheckResponse;
import com.goofish.emm.tutu.TutuUtil;
import com.goofish.emm.util.DeviceUtil;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.dialogs.InputDialog;
import com.kongzue.dialogx.interfaces.OnInputDialogButtonClickListener;
import com.lzf.easyfloat.EasyFloat;
import com.lzf.easyfloat.enums.ShowPattern;
import com.lzf.easyfloat.interfaces.OnInvokeView;
import com.petterp.floatingx.FloatingX;
import com.petterp.floatingx.assist.FxDisplayMode;
import com.petterp.floatingx.assist.FxScopeType;
import com.petterp.floatingx.assist.helper.FxAppHelper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static android.os.UserManager.DISALLOW_ADD_USER;
import static android.os.UserManager.DISALLOW_FACTORY_RESET;
import static android.os.UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA;
import static android.os.UserManager.DISALLOW_SAFE_BOOT;
import static android.os.UserManager.DISALLOW_UNINSTALL_APPS;

import androidx.annotation.NonNull;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import retrofit2.Call;

/**
 * Shows the list of apps passed in the {@link #LOCKED_APP_PACKAGE_LIST} extra (or previously saved
 * in shared preferences if the extra is not found) in single app mode:
 *
 * <ul>
 *   <li>The status bar and keyguard are disabled
 *   <li>Several user restrictions are set to prevent the user from escaping this mode (e.g. safe
 *       boot mode and factory reset are disabled)
 *   <li>This activity is set as the Home intent receiver
 * </ul>
 *
 * If the user taps on one of the apps, it is launched in lock tack mode. Tapping on the back or
 * home buttons will bring the user back to the app list. The list also contains a row to exit
 * single app mode and finish this activity.
 */
@TargetApi(VERSION_CODES.M)
public class KioskModeActivity extends Activity {
    private static final String TAG = "KioskModeActivity";

    private static final String KIOSK_PREFERENCE_FILE = "kiosk_preference_file";
    private static final String KIOSK_APPS_KEY = "kiosk_apps";

    public static final String LOCKED_APP_PACKAGE_LIST =
            "com.afwsamples.testdpc.policy.locktask.LOCKED_APP_PACKAGE_LIST";

    //    public static final String[] DEF_LOCK_TASK = {"com.android.permissioncontroller"};
//    public static final String[] DEF_LOCK_TASK = {"com.android.packageinstaller"};
    public static final String[] DEF_LOCK_TASK = {TutuUtil.TUTU_PKG, "com.android.packageinstaller"};
    private static final String[] KIOSK_USER_RESTRICTIONS = {
            DISALLOW_SAFE_BOOT,
            DISALLOW_FACTORY_RESET,
            DISALLOW_ADD_USER,
            DISALLOW_MOUNT_PHYSICAL_MEDIA,
//            DISALLOW_ADJUST_VOLUME,
            DISALLOW_UNINSTALL_APPS,
//            DISALLOW_DEBUGGING_FEATURES
    };

    private ComponentName mAdminComponentName;
    private ArrayList<String> mKioskPackages;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;


    private int COUNT = 10;
    private long[] mHits = new long[COUNT];
    private int DURATION = 5000;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TutuUtil.ACTION_EXIT_LOCKTASK.equals(intent.getAction())) {
                onBackdoorClicked();
            }
        }


    };

    private void register() {
        IntentFilter filter = new IntentFilter(TutuUtil.ACTION_EXIT_LOCKTASK);
        LocalBroadcastManager.getInstance(EmmApp.app).registerReceiver(receiver, filter);
    }

    private void unregister() {
        LocalBroadcastManager.getInstance(EmmApp.app).unregisterReceiver(receiver);
    }

    boolean shouldForward() {
        //每次点击时，数组向前移动一位
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        //为数组最后一位赋值
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
            mHits = new long[COUNT]; //重新初始化数组
            return true;
        }
        return false;
    }

    private void showFloat() {
        FxAppHelper helper = FxAppHelper.builder()
                .setLayout(R.layout.item_floating)
                .setScopeType(FxScopeType.SYSTEM)
                .setContext(this)
                // 设置启用日志,tag可以自定义，最终显示为FloatingX-xxx
                .setEnableLog(true, "自定义的tag")

                //1. 是否允许全局显示悬浮窗,默认true
                .setEnableAllInstall(true)
                //2. 禁止插入Activity的页面, setEnableAllBlackClass(true)时,此方法生效
//                .addInstallBlackClass(BlackActivity.class)
                //3. 允许插入Activity的页面, setEnableAllBlackClass(false)时,此方法生效
//                .addInstallWhiteClass(MainActivity.class, ScopeActivity.class)

                // 设置启用边缘吸附
                .setEnableEdgeAdsorption(true)
                // 设置边缘偏移量
                .setEdgeOffset(10f)
                // 设置启用悬浮窗可屏幕外回弹
                .setEnableScrollOutsideScreen(true)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (shouldForward()) {
                            Intent intent = new Intent();
                            intent.setClass(KioskModeActivity.this, PolicyManagementActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mHits = new long[COUNT];
                                }
                            }, 5000);
                        }

                    }
                })
                // 设置辅助方向辅助
                // 设置点击事件
//                .setOnClickListener()
                // 设置view-lifecycle监听
                //  .setViewLifecycle()
                // 设置启用动画
//                .setEnableAnimation(true)
                // 设置启用动画实现
//                .setAnimationImpl(new FxAnimationImpl())
                // 设置方向保存impl
//                .setSaveDirectionImpl(new FxConfigStorageToSpImpl(this))

                // 设置底部偏移量
                .setBottomBorderMargin(100f)
                // 设置顶部偏移量
//            setTopBorderMargin(100f)
                // 设置左侧偏移量
                .setLeftBorderMargin(100f)
                // 设置右侧偏移量
                .setRightBorderMargin(100f)
                // 设置浮窗展示类型，默认可移动可点击，无需配置
                .setDisplayMode(FxDisplayMode.Normal)
                //启用悬浮窗,即默认会插入到允许的activity中
                // 启用悬浮窗,相当于一个标记,会自动插入允许的activity中
                .build();
        FloatingX.install(helper).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "onCreate");
        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mPackageManager = getPackageManager();

        checkVersion();

        //showFloat();

        EasyFloat.with(this)
                .setLayout(R.layout.item_floating, new OnInvokeView() {
                    @Override
                    public void invoke(View view) {
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                               /* new InputDialog("请输入密码", "请输入密码", "确定", "取消", "password")
                                        .setCancelable(false)
                                        .setOkButton(new OnInputDialogButtonClickListener<InputDialog>() {
                                            @Override
                                            public boolean onClick(InputDialog baseDialog, View v, String inputStr) {
                                                Toast.makeText(KioskModeActivity.this, inputStr, Toast.LENGTH_LONG).show();
                                                return false;
                                            }
                                        })
                                        .show();*/

                                if (shouldForward()) {

                                    Intent intent = new Intent();
                                    intent.setClass(KioskModeActivity.this, EmmDebugActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                } else {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mHits = new long[COUNT];
                                        }
                                    }, 5000);
                                }
                            }
                        });
                    }
                })
                .setShowPattern(ShowPattern.ALL_TIME)
                .show();
        // check if a new list of apps was sent, otherwise fall back to saved list
        String[] packageArray = getIntent().getStringArrayExtra(LOCKED_APP_PACKAGE_LIST);
        if (packageArray != null) {
            mKioskPackages = new ArrayList<>();

            Collections.addAll(mKioskPackages, packageArray);

            Collections.addAll(mKioskPackages, DEF_LOCK_TASK);

            mKioskPackages.remove(getPackageName());
            mKioskPackages.add(getPackageName());

            setDefaultKioskPolicies(true);
        } else {
            // after a reboot there is no need to set the policies again
            SharedPreferences sharedPreferences =
                    getSharedPreferences(KIOSK_PREFERENCE_FILE, MODE_PRIVATE);
            mKioskPackages =
                    new ArrayList<>(sharedPreferences.getStringSet(KIOSK_APPS_KEY, new HashSet<String>()));
            setDefaultKioskPolicies(true);
        }

        // remove TestDPC package and add to end of list; it will act as back door
        mKioskPackages.remove(getPackageName());
        mKioskPackages.add(getPackageName());

        // create list view with all kiosk packages
//        final KioskAppsArrayAdapter kioskAppsArrayAdapter =
//                new KioskAppsArrayAdapter(this, R.id.pkg_name, mKioskPackages);
//        ListView listView = new ListView(this);
//        listView.setAdapter(kioskAppsArrayAdapter);
//        listView.setOnItemClickListener(
//                new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                        kioskAppsArrayAdapter.onItemClick(parent, view, position, id);
//                    }
//                });
//        setContentView(listView);
        setContentView(R.layout.activity_empty);

        register();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregister();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        startApp(TutuUtil.TUTU_PKG);
//        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
        // start lock task mode if it's not already active
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // ActivityManager.getLockTaskModeState api is not available in pre-M.
        if (Util.SDK_INT < VERSION_CODES.M) {
            if (!am.isInLockTaskMode()) {
                startLockTask();
            }
        } else {
            if (am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }

    }

    public void onBackdoorClicked() {
        stopLockTask();
        setDefaultKioskPolicies(false);
        mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                mAdminComponentName, getPackageName());
        mPackageManager.setComponentEnabledSetting(
                new ComponentName(getPackageName(), getClass().getName()),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP);
        finish();
        startActivity(new Intent(this, PolicyManagementActivity.class));
    }

    private void setUserRestriction(String restriction, boolean disallow) {
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction);
        }
    }

    private void setDefaultKioskPolicies(boolean active) {
        // restore or save previous configuration
        if (active) {
            saveCurrentConfiguration();
//            setUserRestriction(DISALLOW_SAFE_BOOT, active);
//            setUserRestriction(DISALLOW_FACTORY_RESET, active);
//            setUserRestriction(DISALLOW_ADD_USER, active);
//            setUserRestriction(DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
//            setUserRestriction(DISALLOW_ADJUST_VOLUME, active);
            for (String userRestriction : KIOSK_USER_RESTRICTIONS) {
                setUserRestriction(userRestriction, active);
            }
        } else {
            restorePreviousConfiguration();
        }

        // set lock task packages
        mDevicePolicyManager.setLockTaskPackages(
                mAdminComponentName, active ? mKioskPackages.toArray(new String[]{}) : new String[]{});
        SharedPreferences sharedPreferences = getSharedPreferences(KIOSK_PREFERENCE_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (active) {
            editor.putStringSet(KIOSK_APPS_KEY, new HashSet<>(mKioskPackages));
        } else {
            editor.remove(KIOSK_APPS_KEY);
        }
        editor.commit();
    }

    @TargetApi(VERSION_CODES.N)
    private void saveCurrentConfiguration() {
        if (Util.SDK_INT >= VERSION_CODES.N) {
            Bundle settingsBundle = mDevicePolicyManager.getUserRestrictions(mAdminComponentName);
            SharedPreferences.Editor editor =
                    getSharedPreferences(KIOSK_PREFERENCE_FILE, MODE_PRIVATE).edit();

            for (String userRestriction : KIOSK_USER_RESTRICTIONS) {
                boolean currentSettingValue = settingsBundle.getBoolean(userRestriction);
                editor.putBoolean(userRestriction, currentSettingValue);
            }
            editor.commit();
        }
    }

    private void restorePreviousConfiguration() {
        if (Util.SDK_INT >= VERSION_CODES.N) {
            SharedPreferences sharedPreferences =
                    getSharedPreferences(KIOSK_PREFERENCE_FILE, MODE_PRIVATE);

            for (String userRestriction : KIOSK_USER_RESTRICTIONS) {
                boolean prevSettingValue = sharedPreferences.getBoolean(userRestriction, false);
                setUserRestriction(userRestriction, prevSettingValue);
            }
        }
    }

    private class KioskAppsArrayAdapter extends ArrayAdapter<String>
            implements AdapterView.OnItemClickListener {

        public KioskAppsArrayAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ApplicationInfo applicationInfo;
            try {
                applicationInfo = mPackageManager.getApplicationInfo(getItem(position), 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Fail to retrieve application info for the entry: " + position, e);
                return null;
            }

            if (convertView == null) {
                convertView =
                        LayoutInflater.from(getContext()).inflate(R.layout.kiosk_mode_item, parent, false);
            }
            ImageView iconImageView = (ImageView) convertView.findViewById(R.id.pkg_icon);
            iconImageView.setImageDrawable(applicationInfo.loadIcon(mPackageManager));
            TextView pkgNameTextView = (TextView) convertView.findViewById(R.id.pkg_name);
            if (getPackageName().equals(getItem(position))) {
                // back door
                pkgNameTextView.setText(getString(R.string.stop_kiosk_mode));
            } else {
                pkgNameTextView.setText(applicationInfo.loadLabel(mPackageManager));
            }
            return convertView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (getPackageName().equals(getItem(position))) {
                onBackdoorClicked();
                return;
            }
            PackageManager pm = getPackageManager();
            Intent launchAppIntent;
            String appPackage = getItem(position);

            if (Util.isRunningOnTvDevice(getContext())) {
                launchAppIntent = pm.getLeanbackLaunchIntentForPackage(appPackage);
            } else {
                launchAppIntent = pm.getLaunchIntentForPackage(appPackage);
            }
            if (launchAppIntent == null) {
                Toast.makeText(KioskModeActivity.this, "此应用无法打开", Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(launchAppIntent);
        }
    }

    private void startApp(String pkg) {
        AppUtils.launchApp(pkg);
    }

    private void checkVersion() {
        ApiService apiService = RetrofitClient.INSTANCE.getApiService();

        VersionCheckRequest request = new VersionCheckRequest(DeviceUtil.getDeviceImei(KioskModeActivity.this), AppUtils.getAppVersionCode());
        Call<Resp.Common<VersionCheckResponse>> call = apiService.versionCheck(request);
        NetworkManager.INSTANCE.makeRequest(call, new NetCallback<VersionCheckResponse>() {
            @Override
            public void onSuccess(@NonNull Resp.Common<VersionCheckResponse> resp, @NonNull byte[] data) {
                if (Resp.SUCCESS.equals(resp.getCode())) {
                    VersionCheckResponse d = resp.getData();
                    DownloadManager manager = new DownloadManager.Builder(KioskModeActivity.this)
                            .apkUrl(d.getApkUrl())
                            .apkName("appupdate.apk")
                            .smallIcon(R.drawable.ic_launcher)
                            .forcedUpgrade(true)
                            //设置了此参数，那么内部会自动判断是否需要显示更新对话框，否则需要自己判断是否需要更新
                            .apkVersionCode(d.getVersionCode())
                            //同时下面三个参数也必须要设置
                            .apkVersionName(d.getVersionName())
                            .apkSize(d.getSize())
                            .apkDescription(d.getUpgradeMsg())
                            //省略一些非必须参数...
                            .build();
                    manager.download();
                }
            }

            @Override
            public void onNetError(int statusCode, @NonNull String msg) {

            }
        });
    }
}
