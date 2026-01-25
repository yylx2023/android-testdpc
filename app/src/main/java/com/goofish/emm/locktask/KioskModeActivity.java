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
import com.goofish.emm.About;
import com.goofish.emm.EmmApp;
import com.goofish.emm.EmmDebugActivity;
import com.goofish.emm.appstore.AppstoreActivity;
import com.goofish.emm.http.ApiService;
import com.goofish.emm.http.DeviceInfoRequest;
import com.goofish.emm.http.DeviceInfoResponse;
import com.goofish.emm.http.NetCallback;
import com.goofish.emm.http.NetworkManager;
import com.goofish.emm.http.Resp;
import com.goofish.emm.http.RetrofitClient;
import com.goofish.emm.http.CommonRequest;
import com.goofish.emm.http.VersionCheckResponse;
import com.goofish.emm.tutu.TutuUtil;
import com.goofish.emm.util.DeviceInfoHelper;
import com.goofish.emm.util.DeviceUtil;
import com.goofish.emm.util.Dpm;
import com.lzf.easyfloat.EasyFloat;
import com.lzf.easyfloat.enums.ShowPattern;
import com.lzf.easyfloat.interfaces.OnFloatCallbacks;
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
import android.content.pm.ResolveInfo;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import static android.os.UserManager.DISALLOW_ADD_USER;
import static android.os.UserManager.DISALLOW_FACTORY_RESET;
import static android.os.UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA;
import static android.os.UserManager.DISALLOW_SAFE_BOOT;
import static android.os.UserManager.DISALLOW_UNINSTALL_APPS;

import static android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_HOME;
import static android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS;

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

    private static final String KIOSK_PREFERENCE_FILE = Dpm.KIOSK_PREFERENCE_FILE;
    private static final String KIOSK_APPS_KEY = Dpm.KIOSK_APPS_KEY;

    public static final String LOCKED_APP_PACKAGE_LIST = "com.afwsamples.testdpc.policy.locktask.LOCKED_APP_PACKAGE_LIST";

    //    public static final String[] DEF_LOCK_TASK = {"com.android.permissioncontroller"};
//    public static final String[] DEF_LOCK_TASK = {"com.android.packageinstaller"};

    //在桌面显示的app
    public static final String[] APPS = {TutuUtil.TUTU_PKG, "com.tencent.wemeet.app"};

    //无需在桌面显示的
    public static final String[] DEF_LOCK_TASK = {"com.android.packageinstaller"};

    // 是否在 Lock Task 模式下启用 HOME 键
    private static final boolean ENABLE_HOME_KEY_IN_LOCK_TASK = true;

    // 是否在 Lock Task 模式下启用通知功能
    private static final boolean ENABLE_NOTIFICATIONS_IN_LOCK_TASK = false;

    // 内部组件的 action
    private static final String INTERNAL_COMPONENT_ACTION = "com.goofish.emm.action.ON_DESK";
    private static final String[] KIOSK_USER_RESTRICTIONS = {DISALLOW_SAFE_BOOT, DISALLOW_FACTORY_RESET, DISALLOW_ADD_USER, DISALLOW_MOUNT_PHYSICAL_MEDIA,
//            DISALLOW_ADJUST_VOLUME,
            DISALLOW_UNINSTALL_APPS,
//            DISALLOW_DEBUGGING_FEATURES
    };

    private ComponentName mAdminComponentName;
    private ArrayList<String> mKioskPackages;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;

    private RecyclerView mAppsRecyclerView;
    private KioskAppsAdapter mAppsAdapter;
    private List<AppInfo> mAppInfoList;




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

    // 应用安装/卸载监听器 - 只处理 APPS 白名单中的应用
    private BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
                Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(action)) {

                String packageName = getPackageNameFromIntent(intent);
                boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

                Log.i(TAG, "Package event: " + action + ", package: " + packageName + ", replacing: " + replacing);

                // 检查是否需要处理这个包的变化
                boolean shouldProcess = isPackageInWhitelist(packageName) ||
                                       getPackageName().equals(packageName);

                if (!shouldProcess) {
                    Log.d(TAG, "Package " + packageName + " not in APPS whitelist and not current app, ignoring");
                    return;
                }

                Log.i(TAG, "Package " + packageName + " needs processing, refreshing app list");

                // 如果是替换操作（更新），在 PACKAGE_REPLACED 时才刷新
                if (Intent.ACTION_PACKAGE_REPLACED.equals(action) || !replacing) {
                    // 延迟刷新，确保系统完成包管理操作
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshAppList();
                        }
                    }, 500);
                }
            }
        }

        private String getPackageNameFromIntent(Intent intent) {
            if (intent.getData() == null) {
                return null;
            }
            return intent.getData().getSchemeSpecificPart();
        }
    };

    private void register() {
        // 注册退出 Lock Task 广播
        IntentFilter filter = new IntentFilter(TutuUtil.ACTION_EXIT_LOCKTASK);
        LocalBroadcastManager.getInstance(EmmApp.app).registerReceiver(receiver, filter);

        // 注册应用安装/卸载广播
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addDataScheme("package");
        registerReceiver(packageReceiver, packageFilter);

        Log.i(TAG, "Broadcast receivers registered");
    }

    private void unregister() {
        try {
            LocalBroadcastManager.getInstance(EmmApp.app).unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister local receiver: " + e.getMessage());
        }

        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister package receiver: " + e.getMessage());
        }

        Log.i(TAG, "Broadcast receivers unregistered");
    }

    /**
     * 检查包名是否在 APPS 白名单中
     */
    private boolean isPackageInWhitelist(String packageName) {
        if (packageName == null) {
            return false;
        }

        for (String whitelistPackage : APPS) {
            if (whitelistPackage.equals(packageName)) {
                return true;
            }
        }
        return false;
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
        FxAppHelper helper = FxAppHelper.builder().setLayout(R.layout.item_floating).setScopeType(FxScopeType.SYSTEM).setContext(this)
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
                .setEnableScrollOutsideScreen(true).setOnClickListener(new View.OnClickListener() {
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkVersion();
            }
        }, 30 * 1000L);


        //showFloat();

        /*EasyFloat.with(this).setLayout(R.layout.item_floating, new OnInvokeView() {
            @Override
            public void invoke(View view) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                               *//* new InputDialog("请输入密码", "请输入密码", "确定", "取消", "password")
                                        .setCancelable(false)
                                        .setOkButton(new OnInputDialogButtonClickListener<InputDialog>() {
                                            @Override
                                            public boolean onClick(InputDialog baseDialog, View v, String inputStr) {
                                                Toast.makeText(KioskModeActivity.this, inputStr, Toast.LENGTH_LONG).show();
                                                return false;
                                            }
                                        })
                                        .show();*//*

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

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Intent intent = new Intent();
                        intent.setClass(KioskModeActivity.this, AppstoreActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return false;
                    }
                });
            }
        }).setShowPattern(ShowPattern.ALL_TIME).show();*/



        /*EasyFloat.with(this).setLayout(R.layout.item_floating, new OnInvokeView() {
            @Override
            public void invoke(View view) {
                final GestureDetector gestureDetector = new GestureDetector(KioskModeActivity.this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        if (shouldForward()) {
                            Intent intent = new Intent(KioskModeActivity.this, EmmDebugActivity.class);
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
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        Intent intent = new Intent(KioskModeActivity.this, AppstoreActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });

                view.setOnTouchListener(new View.OnTouchListener() {
                    private float dX, dY;
                    private boolean isDragging = false;
                    private long pressStartTime;
                    private static final long LONG_PRESS_THRESHOLD = 500; // 长按阈值，单位毫秒

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (gestureDetector.onTouchEvent(event)) {
                            return true;
                        }

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                dX = v.getX() - event.getRawX();
                                dY = v.getY() - event.getRawY();
                                pressStartTime = System.currentTimeMillis();
                                isDragging = false;
                                break;

                            case MotionEvent.ACTION_MOVE:
                                long pressDuration = System.currentTimeMillis() - pressStartTime;
                                if (pressDuration < LONG_PRESS_THRESHOLD) {
                                    isDragging = true;
                                }
                                if (isDragging) {
                                    v.animate()
                                            .x(event.getRawX() + dX)
                                            .y(event.getRawY() + dY)
                                            .setDuration(0)
                                            .start();
                                }
                                break;

                            case MotionEvent.ACTION_UP:
                                if (!isDragging && System.currentTimeMillis() - pressStartTime < LONG_PRESS_THRESHOLD) {
                                    v.performClick();
                                }
                                break;

                            default:
                                return false;
                        }
                        return true;
                    }
                });
            }
        }).setShowPattern(ShowPattern.ALL_TIME).show();*/

        EasyFloat.with(this)
                .setLayout(R.layout.item_floating)
                .setMatchParent(false, false)
                .setShowPattern(ShowPattern.ALL_TIME)
                .setDragEnable(true)
                .setTag("myFloatView")
                .registerCallbacks(new OnFloatCallbacks() {
                    @Override
                    public void show(@NonNull View view) {

                    }

                    @Override
                    public void hide(@NonNull View view) {

                    }

                    private long touchStartTime;
                    private float touchStartX, touchStartY;
                    private boolean isDragging = false;
                    private static final long LONG_PRESS_DURATION = 500; // milliseconds
                    private static final float CLICK_THRESHOLD = 10; // pixels

                    @Override
                    public void createdResult(boolean isCreated, String msg, View view) {
                        Log.i(TAG, "Float window created: " + isCreated + ", msg: " + msg);
                        if (isCreated) {
                            Log.i(TAG, "Float window successfully created and should be visible");
                            // 可以在这里进行一些初始化操作
                        } else {
                            Log.e(TAG, "Failed to create float window: " + msg);
                        }
                    }

                    @Override
                    public void touchEvent(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                touchStartTime = System.currentTimeMillis();
                                touchStartX = event.getRawX();
                                touchStartY = event.getRawY();
                                isDragging = false;
                                view.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                float deltaX = Math.abs(event.getRawX() - touchStartX);
                                float deltaY = Math.abs(event.getRawY() - touchStartY);
                                if (deltaX > CLICK_THRESHOLD || deltaY > CLICK_THRESHOLD) {
                                    isDragging = true;
                                    view.removeCallbacks(longPressRunnable);
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                                view.removeCallbacks(longPressRunnable);
                                long pressDuration = System.currentTimeMillis() - touchStartTime;
                                Log.d(TAG, "ACTION_UP: isDragging=" + isDragging + ", pressDuration=" + pressDuration + ", LONG_PRESS_DURATION=" + LONG_PRESS_DURATION);
                                if (!isDragging && pressDuration < LONG_PRESS_DURATION) {
                                    Log.i(TAG, "Calling handleClick()");
                                    handleClick();
                                } else {
                                    Log.d(TAG, "Not calling handleClick: isDragging=" + isDragging + " or pressDuration too long");
                                }
                                break;
                        }
                    }

                    private Runnable longPressRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!isDragging) {
                                handleLongPress();
                            }
                        }
                    };


                    @Override
                    public void dismiss() {
                    }

                    @Override
                    public void drag(View view, MotionEvent event) {
                    }

                    @Override
                    public void dragEnd(View view) {
                    }
                })
                .show();

        // check if a new list of apps was sent, otherwise fall back to saved list
        String[] packageArray = getIntent().getStringArrayExtra(LOCKED_APP_PACKAGE_LIST);
        if (packageArray != null) {
            // 使用LinkedHashSet去重，保持顺序
            LinkedHashSet<String> packageSet = new LinkedHashSet<>();

            Collections.addAll(packageSet, packageArray);
            Collections.addAll(packageSet, APPS);
            Collections.addAll(packageSet, DEF_LOCK_TASK);

            // 转换为ArrayList
            mKioskPackages = new ArrayList<>(packageSet);

            // 确保当前应用在最后，作为后门
            mKioskPackages.remove(getPackageName());
            mKioskPackages.add(getPackageName());

            setDefaultKioskPolicies(true);
        } else {
            // after a reboot there is no need to set the policies again
            SharedPreferences sharedPreferences = getSharedPreferences(KIOSK_PREFERENCE_FILE, MODE_PRIVATE);
            LinkedHashSet<String> packageSet = new LinkedHashSet<>(
                    sharedPreferences.getStringSet(KIOSK_APPS_KEY, new HashSet<String>())
            );
            Collections.addAll(packageSet, APPS);
            Collections.addAll(packageSet, DEF_LOCK_TASK);

            // 转换为ArrayList
            mKioskPackages = new ArrayList<>(packageSet);

            // 确保当前应用在最后，作为后门
            mKioskPackages.remove(getPackageName());
            mKioskPackages.add(getPackageName());

            setDefaultKioskPolicies(true);
        }

        setContentView(R.layout.activity_empty);

        // 调试信息：检查当前 Intent
        Intent currentIntent = getIntent();
        if (currentIntent != null) {
            Log.i(TAG, "onCreate - Intent action: " + currentIntent.getAction());
            Log.i(TAG, "onCreate - Intent categories: " + currentIntent.getCategories());
        }

        // 检查是否为默认 HOME Activity
        checkIfDefaultHomeActivity();

        // 初始化RecyclerView
        initRecyclerView();

        register();

        // 上报设备信息
        reportDeviceInfo();
    }

    private void handleClick() {
        Log.i(TAG, "handleClick() called");
        boolean shouldForwardResult = shouldForward();
        Log.i(TAG, "shouldForward() returned: " + shouldForwardResult);

        if (shouldForwardResult) {
            // 连续点击10次：跳转到 About 页面（保持原逻辑）
            Log.i(TAG, "10 consecutive clicks detected, launching About activity");
            Intent intent = new Intent(this, About.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            // 单击：返回到 KioskModeActivity 页面
            Log.i(TAG, "Single click detected, returning to KioskModeActivity");
            Intent intent = new Intent(this, KioskModeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);

            // 延迟重置计数器（保持原逻辑）
            new Handler().postDelayed(() -> mHits = new long[COUNT], 5000);
        }
    }

    private void handleLongPress() {
        Intent intent = new Intent(this, AppstoreActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregister();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume - KioskModeActivity is now visible");

        // 不再自动启动应用，让用户可以看到桌面并选择应用
        // 这样 HOME 键就能正确返回到这个桌面界面
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent called - Intent: " + intent);
        if (intent != null) {
            Log.i(TAG, "Intent action: " + intent.getAction());
            Log.i(TAG, "Intent categories: " + intent.getCategories());
        }

        // 当用户按 HOME 键时，会触发这个方法
        // 确保 Activity 回到前台并显示桌面
        setIntent(intent);

        // 确保界面刷新
        if (mAppsAdapter != null) {
            mAppsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");

        // start lock task mode if it's not already active
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // ActivityManager.getLockTaskModeState api is not available in pre-M.
        if (Util.SDK_INT < VERSION_CODES.M) {
            boolean isInLockTask = am.isInLockTaskMode();
            Log.d(TAG, "Pre-M device, isInLockTaskMode: " + isInLockTask);
            if (!isInLockTask) {
                Log.i(TAG, "Starting Lock Task mode (Pre-M)");
                startLockTask();
            }
        } else {
            int lockTaskState = am.getLockTaskModeState();
            Log.d(TAG, "Lock Task Mode State: " + lockTaskState + " (NONE=" + ActivityManager.LOCK_TASK_MODE_NONE + ")");
            if (lockTaskState == ActivityManager.LOCK_TASK_MODE_NONE) {
                Log.i(TAG, "Starting Lock Task mode (M+)");
                startLockTask();
            }
        }

        // 检查当前的 Lock Task Features
        if (Util.SDK_INT >= VERSION_CODES.P) {
            try {
                int currentFeatures = mDevicePolicyManager.getLockTaskFeatures(mAdminComponentName);
                Log.i(TAG, "Current Lock Task Features: " + currentFeatures);
                Log.i(TAG, "HOME feature enabled: " + ((currentFeatures & LOCK_TASK_FEATURE_HOME) != 0));
            } catch (Exception e) {
                Log.e(TAG, "Failed to get Lock Task Features: " + e.getMessage());
            }
        }
    }

    public void onBackdoorClicked() {
        stopLockTask();
        setDefaultKioskPolicies(false);
        mDevicePolicyManager.clearPackagePersistentPreferredActivities(mAdminComponentName, getPackageName());
        mPackageManager.setComponentEnabledSetting(new ComponentName(getPackageName(), getClass().getName()), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
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

            // 设置 Lock Task Features 以启用 HOME 键
            setLockTaskFeatures(active);
        } else {
            restorePreviousConfiguration();

            // 清除 Lock Task Features
            setLockTaskFeatures(active);
        }

        // set lock task packages
        mDevicePolicyManager.setLockTaskPackages(mAdminComponentName, active ? mKioskPackages.toArray(new String[]{}) : new String[]{});
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
            SharedPreferences.Editor editor = getSharedPreferences(KIOSK_PREFERENCE_FILE, MODE_PRIVATE).edit();

            for (String userRestriction : KIOSK_USER_RESTRICTIONS) {
                boolean currentSettingValue = settingsBundle.getBoolean(userRestriction);
                editor.putBoolean(userRestriction, currentSettingValue);
            }
            editor.commit();
        }
    }

    private void restorePreviousConfiguration() {
        if (Util.SDK_INT >= VERSION_CODES.N) {
            SharedPreferences sharedPreferences = getSharedPreferences(KIOSK_PREFERENCE_FILE, MODE_PRIVATE);

            for (String userRestriction : KIOSK_USER_RESTRICTIONS) {
                boolean prevSettingValue = sharedPreferences.getBoolean(userRestriction, false);
                setUserRestriction(userRestriction, prevSettingValue);
            }
        }
    }

    /**
     * 设置 Lock Task Features 以控制在 Lock Task 模式下可用的功能
     * @param enable 是否启用功能
     */
    @TargetApi(VERSION_CODES.P)
    private void setLockTaskFeatures(boolean enable) {
        Log.d(TAG, "setLockTaskFeatures called with enable=" + enable + ", ENABLE_HOME_KEY_IN_LOCK_TASK=" + ENABLE_HOME_KEY_IN_LOCK_TASK);
        Log.d(TAG, "Current SDK_INT=" + Util.SDK_INT + ", VERSION_CODES.P=" + VERSION_CODES.P);

        if (Util.SDK_INT >= VERSION_CODES.P) {
            try {
                if (enable && ENABLE_HOME_KEY_IN_LOCK_TASK) {
                    // 构建功能标志：启用 HOME 键，可选启用通知功能，禁用多任务键（OVERVIEW）
                    int features = LOCK_TASK_FEATURE_HOME;
                    if (ENABLE_NOTIFICATIONS_IN_LOCK_TASK) {
                        features |= LOCK_TASK_FEATURE_NOTIFICATIONS;
                    }

                    mDevicePolicyManager.setLockTaskFeatures(mAdminComponentName, features);
                    Log.i(TAG, "Lock Task Features enabled: HOME(" + LOCK_TASK_FEATURE_HOME + ")");
                    Log.i(TAG, "NOTIFICATIONS: " + (ENABLE_NOTIFICATIONS_IN_LOCK_TASK ? "ENABLED" : "DISABLED"));
                    Log.i(TAG, "OVERVIEW (Recent Tasks): DISABLED");
                    Log.i(TAG, "Total features value: " + features);

                    // 验证设置是否成功
                    int currentFeatures = mDevicePolicyManager.getLockTaskFeatures(mAdminComponentName);
                    Log.i(TAG, "Verified current Lock Task Features: " + currentFeatures);
                } else {
                    // 禁用所有功能（默认行为）
                    mDevicePolicyManager.setLockTaskFeatures(mAdminComponentName, 0);
                    Log.i(TAG, "Lock Task Features disabled");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to set Lock Task Features: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Lock Task Features require API level 28 (Android P) or higher, current SDK: " + Util.SDK_INT);
        }
    }

    private class KioskAppsArrayAdapter extends ArrayAdapter<String> implements AdapterView.OnItemClickListener {

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
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.kiosk_mode_item, parent, false);
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

    /**
     * 上报设备信息到平台
     * 在 KioskModeActivity 启动时调用
     */
    private void reportDeviceInfo() {
        Log.i(TAG, "Starting device info report");

        ApiService apiService = RetrofitClient.INSTANCE.getApiService();

        // 创建设备信息请求
        DeviceInfoRequest request = DeviceInfoHelper.createDeviceInfoRequest(this);

        // 打印设备信息摘要
        Log.i(TAG, "Reporting: " + DeviceInfoHelper.getDeviceInfoSummary(this));

        Call<Resp.Common<DeviceInfoResponse>> call = apiService.reportDeviceInfo(request);
        NetworkManager.INSTANCE.makeRequest(call, new NetCallback<DeviceInfoResponse>() {
            @Override
            public void onSuccess(@NonNull Resp.Common<DeviceInfoResponse> resp, @NonNull byte[] data) {
                if (Resp.SUCCESS.equals(resp.getCode())) {
                    Log.i(TAG, "Device info reported successfully");
                } else {
                    Log.w(TAG, "Device info report failed: " + resp.getMsg());
                }
            }

            @Override
            public void onNetError(int statusCode, @NonNull String msg) {
                Log.e(TAG, "Device info report network error: " + statusCode + " - " + msg);
            }
        });
    }

    private void checkVersion() {
        ApiService apiService = RetrofitClient.INSTANCE.getApiService();

        CommonRequest request = new CommonRequest(DeviceUtil.getDeviceImei(KioskModeActivity.this), AppUtils.getAppVersionCode());
        Call<Resp.Common<VersionCheckResponse>> call = apiService.versionCheck(request);
        NetworkManager.INSTANCE.makeRequest(call, new NetCallback<VersionCheckResponse>() {
            @Override
            public void onSuccess(@NonNull Resp.Common<VersionCheckResponse> resp, @NonNull byte[] data) {
                if (Resp.SUCCESS.equals(resp.getCode())) {
                    VersionCheckResponse d = resp.getData();
                    DownloadManager manager = new DownloadManager.Builder(KioskModeActivity.this).apkUrl(d.getApkUrl()).apkName("appupdate.apk").smallIcon(R.drawable.ic_launcher).forcedUpgrade(true)
                            //设置了此参数，那么内部会自动判断是否需要显示更新对话框，否则需要自己判断是否需要更新
                            .apkVersionCode(d.getVersionCode())
                            //同时下面三个参数也必须要设置
                            .apkVersionName(d.getVersionName()).apkSize(d.getSize()).apkDescription(d.getUpgradeMsg())
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

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        mAppsRecyclerView = findViewById(R.id.apps_recycler_view);

        // 设置网格布局管理器，根据屏幕宽度动态计算列数
        int spanCount = calculateSpanCount();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        mAppsRecyclerView.setLayoutManager(gridLayoutManager);

        // 准备应用数据
        prepareAppData();

        // 创建并设置适配器
        mAppsAdapter = new KioskAppsAdapter(this, mAppInfoList);
        mAppsAdapter.setOnAppClickListener(new KioskAppsAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(AppInfo appInfo) {
                handleAppClick(appInfo);
            }
        });
        mAppsRecyclerView.setAdapter(mAppsAdapter);
    }

    /**
     * 准备应用数据 - 包括外部应用和内部组件
     */
    private void prepareAppData() {
        mAppInfoList = new ArrayList<>();

        // 1. 加载 APPS 白名单中的外部应用
        loadExternalApps();

        // 2. 加载本应用中具有 ON_DESK action 的内部组件
        loadInternalComponents();

        Log.i(TAG, "Prepared app data: " + mAppInfoList.size() + " items total");
    }

    /**
     * 加载外部应用
     */
    private void loadExternalApps() {
        for (String packageName : APPS) {
            try {
                ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(packageName, 0);
                String appName = applicationInfo.loadLabel(mPackageManager).toString();
                android.graphics.drawable.Drawable appIcon = applicationInfo.loadIcon(mPackageManager);

                AppInfo appInfo = new AppInfo(packageName, appName, appIcon);
                mAppInfoList.add(appInfo);
                Log.d(TAG, "Added external app: " + appName + " (" + packageName + ")");
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "External app not found: " + packageName, e);
            }
        }
    }

    /**
     * 加载本应用中具有 ON_DESK action 的内部组件
     */
    private void loadInternalComponents() {
        Intent intent = new Intent(INTERNAL_COMPONENT_ACTION);
        intent.setPackage(getPackageName()); // 只查询本应用的组件

        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent, 0);

        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo != null) {
                try {
                    String componentName = resolveInfo.activityInfo.packageName + "/" + resolveInfo.activityInfo.name;
                    String appName = resolveInfo.loadLabel(mPackageManager).toString();
                    android.graphics.drawable.Drawable appIcon = resolveInfo.loadIcon(mPackageManager);

                    AppInfo appInfo = new AppInfo(componentName, appName, appIcon, INTERNAL_COMPONENT_ACTION);
                    mAppInfoList.add(appInfo);
                    Log.d(TAG, "Added internal component: " + appName + " (" + componentName + ")");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to load internal component: " + resolveInfo.activityInfo.name, e);
                }
            }
        }
    }

    /**
     * 刷新应用列表 - 只刷新 UI，不更新 Kiosk 包列表
     */
    private void refreshAppList() {
        Log.i(TAG, "Refreshing app list due to package changes in APPS whitelist");

        // 重新准备应用数据（只处理 APPS 白名单中的应用）
        prepareAppData();

        // 在主线程中更新UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAppsAdapter != null) {
                    // 更新适配器数据
                    mAppsAdapter.updateAppList(mAppInfoList);
                    Log.i(TAG, "App list refreshed, total apps: " + mAppInfoList.size());
                } else {
                    Log.w(TAG, "Apps adapter is null, cannot refresh");
                }
            }
        });
    }



    /**
     * 处理应用点击事件 - 支持外部应用和内部组件
     */
    private void handleAppClick(AppInfo appInfo) {
        if (appInfo.isExternalApp()) {
            // 处理外部应用点击
            handleExternalAppClick(appInfo);
        } else if (appInfo.isInternalComponent()) {
            // 处理内部组件点击
            handleInternalComponentClick(appInfo);
        } else {
            Log.w(TAG, "Unknown app info type: " + appInfo.getItemType());
        }
    }

    /**
     * 处理外部应用点击
     */
    private void handleExternalAppClick(AppInfo appInfo) {
        String packageName = appInfo.getPackageName();

        if (getPackageName().equals(packageName)) {
            // 如果点击的是当前应用，执行后门操作
            onBackdoorClicked();
            return;
        }

        // 启动外部应用
        Log.i(TAG, "Launching external app: " + appInfo.getAppName() + " (" + packageName + ")");
        KioskAppsAdapter.launchApp(this, packageName);
    }

    /**
     * 处理内部组件点击
     */
    private void handleInternalComponentClick(AppInfo appInfo) {
        try {
            String componentName = appInfo.getComponentName();
            String action = appInfo.getAction();

            Log.i(TAG, "Launching internal component: " + appInfo.getAppName() + " (" + componentName + ")");

            Intent intent = new Intent(action);
            intent.setComponent(android.content.ComponentName.unflattenFromString(componentName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch internal component: " + appInfo.getAppName(), e);
            Toast.makeText(this, "无法启动 " + appInfo.getAppName(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 根据屏幕宽度计算网格列数
     */
    private int calculateSpanCount() {
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        // 每个应用项大约需要120dp宽度（包括padding）
        int spanCount = (int) (dpWidth / 200);

        // 最少2列，最多6列
        return Math.max(2, Math.min(spanCount, 6));
    }

    /**
     * 检查当前应用是否为默认的 HOME Activity
     */
    private void checkIfDefaultHomeActivity() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);

            ResolveInfo resolveInfo = mPackageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String defaultHome = resolveInfo.activityInfo.packageName + "/" + resolveInfo.activityInfo.name;
                String currentActivity = getPackageName() + "/" + getClass().getName();

                Log.i(TAG, "Default HOME Activity: " + defaultHome);
                Log.i(TAG, "Current Activity: " + currentActivity);
                Log.i(TAG, "Is default HOME: " + defaultHome.equals(currentActivity));
            } else {
                Log.w(TAG, "Could not resolve default HOME Activity");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking default HOME Activity: " + e.getMessage());
        }
    }
}
