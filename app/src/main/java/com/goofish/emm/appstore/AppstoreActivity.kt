package com.goofish.emm.appstore

import AppGridAdapter
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afwsamples.testdpc.R
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.goofish.emm.EmmApp
import com.goofish.emm.http.AppListResponse
import com.goofish.emm.http.CommonRequest
import com.goofish.emm.http.NetCallback
import com.goofish.emm.http.NetworkManager
import com.goofish.emm.http.Resp
import com.goofish.emm.http.Resp.Common
import com.goofish.emm.http.RetrofitClient.apiService
import com.goofish.emm.util.DeviceUtil
import kotlin.math.roundToInt

class AppstoreActivity : Activity() {
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var downloadProgressCard: CardView
    private lateinit var globalProgressBar: ProgressBar
    private lateinit var progressPercentage: TextView
    private lateinit var downloadTitle: TextView
    private lateinit var downloadInfo: TextView
    private lateinit var adapter: AppGridAdapter

    private var currentDownloadApp: App? = null
    private var downloadStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appstore)

        // Setup ActionBar with back button
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "应用商店"
        }

        // Initialize views
        swipeRefresh = findViewById(R.id.swipe_refresh)
        recyclerView = findViewById(R.id.app_grid)
        downloadProgressCard = findViewById(R.id.download_progress_card)
        globalProgressBar = findViewById(R.id.global_progress_bar)
        progressPercentage = findViewById(R.id.progress_percentage)
        downloadTitle = findViewById(R.id.download_title)
        downloadInfo = findViewById(R.id.download_info)

        // Setup RecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        adapter = AppGridAdapter(
            EmmApp.app,
            onDownloadStart = { app -> showGlobalProgressBar(app) },
            onDownloadProgress = { progress -> updateGlobalProgressBar(progress) },
            onDownloadComplete = { hideGlobalProgressBar() }
        )
        recyclerView.adapter = adapter

        // Setup SwipeRefreshLayout
        setupSwipeRefresh()

        loadApps()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Log.i("AppstoreActivity", "Back button clicked, finishing activity")
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupSwipeRefresh() {
        // 设置下拉刷新的颜色
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // 设置下拉刷新监听器
        swipeRefresh.setOnRefreshListener {
            Log.i("AppstoreActivity", "Swipe refresh triggered")
            refreshApps()
        }
    }

    private fun refreshApps() {
        Log.i("AppstoreActivity", "Refreshing app list...")
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
                // 停止刷新动画
                swipeRefresh.isRefreshing = false

                if (Resp.SUCCESS == resp.code) {
                    val apps = resp.data!!.apps
                    adapter.setData(apps)
                    Log.i("AppstoreActivity", "App list loaded successfully: ${apps.size} apps")
                    ToastUtils.showShort("刷新成功")
                } else {
                    Log.e("AppstoreActivity", "Failed to load apps: ${resp.msg}")
                    ToastUtils.showShort("刷新失败: ${resp.msg}")
                }
            }

            override fun onNetError(statusCode: Int, msg: String) {
                // 停止刷新动画
                swipeRefresh.isRefreshing = false

                Log.e("AppstoreActivity", "Network error: $statusCode - $msg")
                ToastUtils.showShort("网络错误: $msg")
            }

        })

    }

    private fun showGlobalProgressBar(app: App) {
        currentDownloadApp = app
        downloadStartTime = System.currentTimeMillis()

        Handler(Looper.getMainLooper()).post {
            downloadProgressCard.visibility = View.VISIBLE
            globalProgressBar.progress = 0
            progressPercentage.text = "0%"
            downloadTitle.text = "正在下载 ${app.name}"
            downloadInfo.text = "准备下载..."
        }
    }

    private fun updateGlobalProgressBar(progress: Int) {
        Handler(Looper.getMainLooper()).post {
            globalProgressBar.progress = progress
            progressPercentage.text = "$progress%"

            // Calculate download info
            val elapsedTime = (System.currentTimeMillis() - downloadStartTime) / 1000.0
            val downloadedSize = (currentDownloadApp?.size ?: 0) * progress / 100
            val totalSize = currentDownloadApp?.size ?: 0

            val downloadedMB = downloadedSize / (1024.0 * 1024.0)
            val totalMB = totalSize / (1024.0 * 1024.0)

            val speed = if (elapsedTime > 0) {
                downloadedSize / elapsedTime / 1024.0 // KB/s
            } else {
                0.0
            }

            val remainingSize = totalSize - downloadedSize
            val remainingTime = if (speed > 0) {
                (remainingSize / 1024.0 / speed).roundToInt()
            } else {
                0
            }

            val infoText = buildString {
                append(String.format("%.1f MB / %.1f MB", downloadedMB, totalMB))
                if (speed > 0) {
                    append(" • ")
                    if (speed > 1024) {
                        append(String.format("%.1f MB/s", speed / 1024.0))
                    } else {
                        append(String.format("%.0f KB/s", speed))
                    }
                }
                if (remainingTime > 0) {
                    append(" • 剩余 ")
                    when {
                        remainingTime < 60 -> append("${remainingTime}秒")
                        remainingTime < 3600 -> append("${remainingTime / 60}分钟")
                        else -> append("${remainingTime / 3600}小时")
                    }
                }
            }

            downloadInfo.text = infoText
        }
    }

    private fun hideGlobalProgressBar() {
        Handler(Looper.getMainLooper()).post {
            downloadProgressCard.visibility = View.GONE
            currentDownloadApp = null
        }
    }
}