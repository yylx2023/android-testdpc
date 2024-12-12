package com.goofish.emm

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.afwsamples.testdpc.R
import com.azhon.appupdate.manager.DownloadManager
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.goofish.emm.http.CommonRequest
import com.goofish.emm.http.NetCallback
import com.goofish.emm.http.NetworkManager.makeRequest
import com.goofish.emm.http.Resp
import com.goofish.emm.http.Resp.Common
import com.goofish.emm.http.RetrofitClient.apiService
import com.goofish.emm.http.VersionCheckResponse
import com.goofish.emm.tutu.TutuUtil
import com.goofish.emm.util.DeviceUtil
import com.kongzue.dialogx.dialogs.InputDialog
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element


class About : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about);

        val adsElement = Element()
        adsElement.setTitle("Advertise with us")

        val aboutPage: View =
            AboutPage(this).isRTL(false).setDescription(getString(R.string.app_name))
                .setImage(R.drawable.icon)
                .addItem(Element().setTitle("版本号: ${AppUtils.getAppVersionName()}"))
                .addItem(Element().setTitle("检查更新").setOnClickListener {
                    checkVersion()
//                UpdateChecker.checkVersion(this@About, true)
                }).addItem(Element().setTitle("退出锁定模式").setOnClickListener {
                    InputDialog(
                        "${DeviceUtil.getDeviceImei(this@About)}",
                        "请输入密码",
                        "确定",
                        "取消",
                        "正在输入的文字"
                    ).setInputText("").setOkButton { baseDialog, v, inputStr ->

                        if (DeviceUtil.generateCode(DeviceUtil.getDeviceImei(this@About)) == inputStr) {
                            // 如果密码正确，显示隐藏的按钮
                            LocalBroadcastManager.getInstance(EmmApp.app)
                                .sendBroadcast(Intent(TutuUtil.ACTION_EXIT_LOCKTASK))
                            false
                        } else {
                            // 密码错误，可以在这里给出提示
                            ToastUtils.showShort("密码错误请联系管理员")
                            true
                        }

                    }
                        .setCancelable(false)
                        .show()
                })
//                .addItem(Element().setTitle("刷新配置").setOnClickListener {
//
//                })
//                .addItem(Element().setTitle("上传日志").setOnClickListener {
//                    // 将该目录：LogConfig.getLogDir() 压缩为zip，文件名为YYYYMMddHHmmss.zip
//                    // 将该zip文件通过接口上传到平台 ,LogConfig.getLogDir()/YYYYMMddHHmmss.zip
//                    //ZipUtils.zipFile()
//
//
//                })
//                .addItem(Element().setTitle("个人信息收集清单").setOnClickListener {
//
//                })
//                .addItem(Element().setTitle("第三方信息共享清单").setOnClickListener {
//
//                })
                .addGroup("硬件信息")
                .addItem(Element().setTitle("SN: ${DeviceUtil.getDeviceImei(this@About)}"))
                .addItem(Element().setTitle("MODEL: ${Build.MODEL}")).create()

        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        aboutPage.layoutParams = params
        findViewById<TableLayout>(R.id.table).addView(aboutPage, 0, params)

    }


    private fun checkVersion() {
        val apiService = apiService

        val request = CommonRequest(
            DeviceUtil.getDeviceImei(this@About), AppUtils.getAppVersionCode()
        )
        val call = apiService.versionCheck(request)
        makeRequest<VersionCheckResponse>(call, object : NetCallback<VersionCheckResponse> {
            override fun onSuccess(resp: Common<VersionCheckResponse>, data: ByteArray) {
                if (Resp.SUCCESS == resp.code) {
                    val d = resp.data
                    val manager: DownloadManager = DownloadManager.Builder(this@About).apkUrl(
                        d!!.apkUrl
                    ).apkName("appupdate.apk").smallIcon(R.drawable.icon)
                        .forcedUpgrade(true) //设置了此参数，那么内部会自动判断是否需要显示更新对话框，否则需要自己判断是否需要更新
                        .apkVersionCode(d!!.versionCode) //同时下面三个参数也必须要设置
                        .apkVersionName(d!!.versionName).apkSize(d!!.size)
                        .apkDescription(d!!.upgradeMsg) //省略一些非必须参数...
                        .build()
                    manager.download()
                } else {
                    ToastUtils.showShort("当前已经是最新版本~")
                }
            }

            override fun onNetError(statusCode: Int, msg: String) {
                ToastUtils.showShort("网络异常请稍后重试~${statusCode}")
            }
        }!!)
    }
}
