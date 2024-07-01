package com.goofish.emm;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;

import com.afwsamples.testdpc.PolicyManagementActivity;
import com.afwsamples.testdpc.R;
import com.goofish.emm.locktask.KioskModeActivity;
import com.goofish.emm.util.ClickUtils;
import com.kongzue.dialogx.DialogX;
import com.petterp.floatingx.FloatingX;
import com.petterp.floatingx.assist.FxDisplayMode;
import com.petterp.floatingx.assist.FxScopeType;
import com.petterp.floatingx.assist.helper.FxAppHelper;
import com.tencent.mmkv.MMKV;

public class EmmApp extends Application {


    private int COUNT = 10;
    private long[] mHits = new long[COUNT];
    private int DURATION = 5000;

    @Override
    public void onCreate() {
        super.onCreate();

        MMKV.initialize(this);

        DialogX.init(this);
        DialogX.globalHoverWindow = true;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //showFloat();
            }
        }, 60 * 1000);

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
                            intent.setClass(EmmApp.this, PolicyManagementActivity.class);
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
}
