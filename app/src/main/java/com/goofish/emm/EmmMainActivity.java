package com.goofish.emm;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.R;
import com.afwsamples.testdpc.common.Util;
import com.blankj.utilcode.util.PhoneUtils;
import com.goofish.emm.locktask.KioskModeActivity;
import com.goofish.emm.locktask.LockTaskAppInfoArrayAdapter;
import com.goofish.emm.tutu.TutuUtil;
import com.goofish.emm.util.DeviceUtil;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;

public class EmmMainActivity extends Activity {

    private static final String TAG = "EmmMainActivity";

    private PackageManager mPackageManager;

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;

    interface ManageLockTaskListCallback {
        void onPositiveButtonClicked(String[] lockTaskArray);
    }


    private void grantPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }


        mDevicePolicyManager.setPermissionGrantState(mAdminComponentName, getPackageName(), Manifest.permission.READ_PHONE_STATE, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emm_main);

        mPackageManager = getPackageManager();

        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        grantPermission();

        Button myButton = findViewById(R.id.my_button);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button clicked!");

                showManageLockTaskListPrompt(
                        R.string.kiosk_select_title,
                        new ManageLockTaskListCallback() {
                            @Override
                            public void onPositiveButtonClicked(String[] lockTaskArray) {
                                startKioskMode(lockTaskArray);
                            }
                        });
            }
        });

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