package com.goofish.emm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.R;
import com.afwsamples.testdpc.common.Util;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.goofish.emm.http.ActivationResponse;
import com.goofish.emm.http.ActivationRequest;
import com.goofish.emm.http.ApiService;
import com.goofish.emm.http.NetCallback;
import com.goofish.emm.http.NetworkManager;
import com.goofish.emm.http.Resp;
import com.goofish.emm.http.RetrofitClient;
import com.goofish.emm.locktask.KioskModeActivity;
import com.goofish.emm.locktask.LockTaskAppInfoArrayAdapter;
import com.goofish.emm.tutu.TutuUtil;
import com.goofish.emm.util.AppPref;
import com.goofish.emm.util.DeviceUtil;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;

public class EmmMainActivity extends Activity {

    private static final String TAG = "EmmMainActivity";

    private PackageManager mPackageManager;

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;

    private RelativeLayout relativeLayout;

    interface ManageLockTaskListCallback {
        void onPositiveButtonClicked(String[] lockTaskArray);
    }


    private void grantPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        mDevicePolicyManager.setPermissionGrantState(mAdminComponentName, getPackageName(), Manifest.permission.READ_PHONE_STATE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        mDevicePolicyManager.setPermissionGrantState(mAdminComponentName, getPackageName(), Manifest.permission.SYSTEM_ALERT_WINDOW, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
    }

    private void showDeviceOwnerAlertDialog() {
        new AlertDialog.Builder(this)
                .setTitle("APP尚未授权")
                .setMessage("请先通过工具给app授权")
                .setCancelable(false) // 点击对话框外部不消失
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish(); // 关闭Activity
                    }
                })
                .create()
                .show();
    }

    private void showTutuAlertDialog() {
        new AlertDialog.Builder(this)
                .setTitle("APP尚未安装")
                .setMessage("请先安装说多多APP")
                .setCancelable(false) // 点击对话框外部不消失
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish(); // 关闭Activity
                    }
                })
                .create()
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emm_main);


        mPackageManager = getPackageManager();

        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // 检查是否是设备所有者
        if (!mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            showDeviceOwnerAlertDialog();
            return; // 不继续执行后续代码
        }

        grantPermission();

        //检查说多多app是否安装
        if (!AppUtils.isAppInstalled(TutuUtil.TUTU_PKG)) {
            showTutuAlertDialog();
        }

        //设备已经激活
        if (!TextUtils.isEmpty(AppPref.getInstance().getMMKV().decodeString("token"))) {
            startKioskMode(new String[]{TutuUtil.TUTU_PKG});
            return;
        }
        //checkVersion();

        Button myButton = findViewById(R.id.my_button);
        myButton.setText(DeviceUtil.getDeviceImei(this));
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button clicked!");

                relativeLayout.setVisibility(View.VISIBLE); // 显示 ProgressBar
                relativeLayout.setTooltipText("正在激活中请稍候～～～");
                ApiService apiService = RetrofitClient.INSTANCE.getApiService();

                ActivationRequest request = new ActivationRequest(DeviceUtil.getDeviceImei(EmmMainActivity.this));
                Call<Resp.Common<ActivationResponse>> call = apiService.activate(request);
                NetworkManager.INSTANCE.makeRequest(call, new NetCallback<ActivationResponse>() {
                    @Override
                    public void onSuccess(@NonNull Resp.Common<ActivationResponse> resp, @NonNull byte[] data) {
                        Log.e("ggg", "ggg" + resp.getCode());
                        relativeLayout.setVisibility(View.GONE); // 显示 ProgressBar
                        //成功
                        if (Resp.SUCCESS.equals(resp.getCode())) {
                            AppPref.getInstance().getMMKV().encode("token", resp.getData().getToken());
                            startKioskMode(new String[]{TutuUtil.TUTU_PKG});
                        } else {
                            ToastUtils.showShort(resp.getMsg());
                        }

                    }

                    @Override
                    public void onNetError(int statusCode, @NonNull String msg) {
                        relativeLayout.setVisibility(View.GONE); // 显示 ProgressBar

                        ToastUtils.showShort("网络异常: " + statusCode);
                    }
                });

//                showManageLockTaskListPrompt(
//                        R.string.kiosk_select_title,
//                        new ManageLockTaskListCallback() {
//                            @Override
//                            public void onPositiveButtonClicked(String[] lockTaskArray) {
//                                startKioskMode(lockTaskArray);
//                            }
                //     });
            }
        });

        relativeLayout = findViewById(R.id.prl);


        Log.e(TAG, "imei = " + DeviceUtil.getDeviceImei(this));
    }


    private void startKioskMode(String[] lockTaskArray) {
        if (!Util.isDeviceOwner(this)) {
            Toast.makeText(this, "请先将设备设置为设备管理者", Toast.LENGTH_LONG).show();
            return;
        }

        TutuUtil.grantPermission(mDevicePolicyManager, mAdminComponentName);
        final ComponentName customLauncher = new ComponentName(this, KioskModeActivity.class);

        // enable custom launcher (it's disabled by default in manifest)
        mPackageManager.setComponentEnabledSetting(
                customLauncher,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        // set custom launcher as default home activity
        mDevicePolicyManager.addPersistentPreferredActivity(
                mAdminComponentName, Util.getHomeIntentFilter(), customLauncher);
        Intent launchIntent = Util.getHomeIntent();
        launchIntent.putExtra(KioskModeActivity.LOCKED_APP_PACKAGE_LIST, lockTaskArray);

        startActivity(launchIntent);
        finish();
    }

    /**
     * Shows a list of primary user apps in a dialog.
     *
     * @param dialogTitle the title to show for the dialog
     * @param callback    will be called with the list apps that the user has selected when he
     *                    closes the
     *                    dialog. The callback is not fired if the user cancels.
     */
    private void showManageLockTaskListPrompt(
            int dialogTitle, final ManageLockTaskListCallback callback) {
        if (isFinishing()) {
            return;
        }
        Intent launcherIntent = Util.getLauncherIntent(this);
        final List<ResolveInfo> primaryUserAppList =
                mPackageManager.queryIntentActivities(launcherIntent, 0);
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        // Also show the default launcher in this list
        final ResolveInfo defaultLauncher = mPackageManager.resolveActivity(homeIntent, 0);
        primaryUserAppList.add(defaultLauncher);
        if (primaryUserAppList.isEmpty()) {
//            showToast(R.string.no_primary_app_available);
            Log.e(TAG, "No primary app is available");
        } else {
            Collections.sort(primaryUserAppList, new ResolveInfo.DisplayNameComparator(mPackageManager));
            final LockTaskAppInfoArrayAdapter appInfoArrayAdapter =
                    new LockTaskAppInfoArrayAdapter(this, R.id.pkg_name, primaryUserAppList);
            ListView listView = new ListView(this);
            listView.setAdapter(appInfoArrayAdapter);
            listView.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            appInfoArrayAdapter.onItemClick(parent, view, position, id);
                        }
                    });

            new AlertDialog.Builder(this)
                    .setTitle(getString(dialogTitle))
                    .setView(listView)
                    .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] lockTaskEnabledArray = appInfoArrayAdapter.getLockTaskList();
                                    callback.onPositiveButtonClicked(lockTaskEnabledArray);
                                }
                            })
                    .setNegativeButton(
                            android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                    .show();
        }
    }
}