package com.goofish.emm.util;

import com.tencent.mmkv.MMKV;

public class AppPref {

    private static  volatile AppPref instance;

    private MMKV mmkv;

    private AppPref(){
        mmkv = MMKV.defaultMMKV();
    }

    public static AppPref getInstance(){
        if (null == instance){
            synchronized (AppPref.class){
                if (null == instance){
                    instance = new AppPref();
                }
            }
        }
        return instance;
    }

    public MMKV getMMKV(){
        return mmkv;
    }
}
