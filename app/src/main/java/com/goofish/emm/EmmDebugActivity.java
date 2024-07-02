package com.goofish.emm;

import com.afwsamples.testdpc.R;
import com.blankj.utilcode.util.ToastUtils;
import com.goofish.emm.tutu.TutuUtil;
import com.goofish.emm.util.DeviceUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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

    }
}