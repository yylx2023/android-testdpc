package com.goofish.emm.appstore

import AppGridAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afwsamples.testdpc.R
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.goofish.emm.EmmApp
import com.goofish.emm.EmmDebugActivity
import com.goofish.emm.http.AppListResponse
import com.goofish.emm.http.CommonRequest
import com.goofish.emm.http.NetCallback
import com.goofish.emm.http.NetworkManager
import com.goofish.emm.http.Resp
import com.goofish.emm.http.Resp.Common
import com.goofish.emm.http.RetrofitClient.apiService
import com.goofish.emm.util.DeviceUtil
import com.liulishuo.okdownload.core.Util
import com.liulishuo.okdownload.core.Util.Logger
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern

class AppstoreActivity : Activity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var globalProgressBar: ProgressBar
    private lateinit var adapter: AppGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appstore)


        Util.setLogger(object : Logger {
            override fun e(tag: String?, msg: String?, e: Exception?) {
                Log.e(tag, msg, e)
            }

            override fun w(tag: String?, msg: String?) {
                Log.w(tag, msg!!)
            }

            override fun d(tag: String?, msg: String?) {
                Log.d(tag, msg!!)
            }

            override fun i(tag: String?, msg: String?) {
                Log.i(tag, msg!!)
            }

        })
        recyclerView = findViewById(R.id.app_grid)
        globalProgressBar = findViewById(R.id.global_progress_bar)

        recyclerView.layoutManager = GridLayoutManager(this, 4)

        adapter = AppGridAdapter(EmmApp.app,
            onDownloadStart = { showGlobalProgressBar() },
            onDownloadProgress = { progress -> updateGlobalProgressBar(progress) },
            onDownloadComplete = { hideGlobalProgressBar() })
        recyclerView.adapter = adapter

        loadApps()

    }


    private fun loadApps() {
        // In a real app, this might be an asynchronous operation
//        val apps = listOf(
//            App(
//                "腾讯会议",
//                "https://vip.123pan.cn/1821818211/emm/app/ic_wechatmeet.png",
//                "com.tencent.wemeet.app",
//                "https://vip.123pan.cn/1821818211/emm/app/TencentMeeting_0300000000_3.28.21.417.publish.officialwebsite.apk"
//            ),
//            App(
//                "腾讯会议",
//                "https://vip.123pan.cn/1821818211/emm/app/ic_wechatmeet.png",
//                "com.example.app2",
//                "https://vip.123pan.cn/1821818211/emm/app/TencentMeeting_0300000000_3.28.21.417.publish.officialwebsite.apk",
//                isInstalled = true
//            ),
//            // Add more apps here
//        )
//        adapter.setData(apps)

        val apiService = apiService

        val request = CommonRequest(
            DeviceUtil.getDeviceImei(this@AppstoreActivity), AppUtils.getAppVersionCode()
        )
        val call = apiService.appList(request)
        NetworkManager.makeRequest<AppListResponse>(call, object : NetCallback<AppListResponse> {
            override fun onSuccess(resp: Common<AppListResponse>, data: ByteArray) {
                if (Resp.SUCCESS == resp.code) {
                    val apps = resp.data!!.apps
                    adapter.setData(apps)
                }
            }

            override fun onNetError(statusCode: Int, msg: String) {
                ToastUtils.showShort(statusCode)
            }

        })

    }

    private fun showGlobalProgressBar() {
        globalProgressBar.visibility = View.VISIBLE
        globalProgressBar.progress = 0
    }

    private fun updateGlobalProgressBar(progress: Int) {
        globalProgressBar.progress = progress
    }

    private fun hideGlobalProgressBar() {
        Handler(Looper.getMainLooper()).post { globalProgressBar.visibility = View.GONE }
    }
}