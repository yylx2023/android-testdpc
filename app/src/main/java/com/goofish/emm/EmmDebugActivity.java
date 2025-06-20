package com.goofish.emm;

import com.afwsamples.testdpc.R;
import com.azhon.appupdate.manager.DownloadManager;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.goofish.emm.http.ApiService;
import com.goofish.emm.http.CommonRequest;
import com.goofish.emm.http.NetCallback;
import com.goofish.emm.http.NetworkManager;
import com.goofish.emm.http.Resp;
import com.goofish.emm.http.RetrofitClient;
import com.goofish.emm.http.VersionCheckResponse;
import com.goofish.emm.locktask.KioskModeActivity;
import com.goofish.emm.tutu.TutuUtil;
import com.goofish.emm.util.DeviceUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import retrofit2.Call;

public class EmmDebugActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        final EditText passwordEditText = findViewById(R.id.passwordEditText);
        Button submitPasswordButton = findViewById(R.id.submitPasswordButton);
        TextView tv = findViewById(R.id.tvSn);
        String sn = DeviceUtil.getDeviceImei(this);
        tv.setText(sn);
        submitPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = passwordEditText.getText().toString();
                if (DeviceUtil.generateCode(sn).equals(password)) {
                    // 如果密码正确，显示隐藏的按钮
                    LocalBroadcastManager.getInstance(EmmApp.app).sendBroadcast(new Intent(TutuUtil.ACTION_EXIT_LOCKTASK));
                } else {
                    // 密码错误，可以在这里给出提示
                    ToastUtils.showShort("密码错误请联系管理员");
                }
            }
        });

        Button btnCheck = findViewById(R.id.btnCheckVersion);

        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkVersion();
            }
        });

    }

    private void checkVersion() {
        ApiService apiService = RetrofitClient.INSTANCE.getApiService();

        CommonRequest request = new CommonRequest(DeviceUtil.getDeviceImei(EmmDebugActivity.this), AppUtils.getAppVersionCode());
        Call<Resp.Common<VersionCheckResponse>> call = apiService.versionCheck(request);
        NetworkManager.INSTANCE.makeRequest(call, new NetCallback<VersionCheckResponse>() {
            @Override
            public void onSuccess(@NonNull Resp.Common<VersionCheckResponse> resp, @NonNull byte[] data) {
                if (Resp.SUCCESS.equals(resp.getCode())) {
                    VersionCheckResponse d = resp.getData();
                    DownloadManager manager = new DownloadManager.Builder(EmmDebugActivity.this).apkUrl(d.getApkUrl()).apkName("appupdate.apk").smallIcon(R.drawable.ic_launcher).forcedUpgrade(true)
                            //设置了此参数，那么内部会自动判断是否需要显示更新对话框，否则需要自己判断是否需要更新
                            .apkVersionCode(d.getVersionCode())
                            //同时下面三个参数也必须要设置
                            .apkVersionName(d.getVersionName()).apkSize(d.getSize()).apkDescription(d.getUpgradeMsg())
                            //省略一些非必须参数...
                            .build();
                    manager.download();
                }else {
                    Toast.makeText(EmmDebugActivity.this, resp.getCode(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNetError(int statusCode, @NonNull String msg) {
                Toast.makeText(EmmDebugActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}