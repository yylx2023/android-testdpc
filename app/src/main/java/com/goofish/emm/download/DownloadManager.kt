package com.goofish.emm.download

import android.util.Log
import com.afwsamples.testdpc.common.PackageInstallationUtils
import com.goofish.emm.EmmApp
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock
import java.io.FileInputStream


public class DownloadManager {


    companion object {


        private val fetchConfiguration =
            FetchConfiguration.Builder(EmmApp.app)
                .setDownloadConcurrentLimit(3)
                .build()
        private val fetch = Fetch.Impl.getInstance(fetchConfiguration)


        fun download(url: String, path: String, callback: DownloadCallback) {


            val request = Request(url, path)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL

            fetch.enqueue(request, { updatedRequest -> }, { error -> })

            val fetchListener: FetchListener = object : FetchListener {
                override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                    Log.e("eee", "eee onQueued")
                }

                override fun onCompleted(download: Download) {

                    callback.onCompleted(download)
                    PackageInstallationUtils.installPackage(
                        EmmApp.app,
                        FileInputStream(path),
                        EmmApp.app.packageName
                    )
                }


                override fun onProgress(
                    download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long
                ) {
                    Log.e("eee", "eee progress = " + download.progress)
                    callback.onProgress(download.progress)
                }

                override fun onPaused(download: Download) {
                }

                override fun onResumed(download: Download) {
                }

                override fun onStarted(
                    download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int
                ) {
                    Log.e("eee", "eee onStarted")
                    callback.onStart()
                }

                override fun onWaitingNetwork(download: Download) {
                }

                override fun onAdded(download: Download) {
                    Log.e("eee", "eee onAdded")
                }

                override fun onCancelled(download: Download) {
                }

                override fun onRemoved(download: Download) {
                }

                override fun onDeleted(download: Download) {
                }

                override fun onDownloadBlockUpdated(
                    download: Download, downloadBlock: DownloadBlock, totalBlocks: Int
                ) {
                }

                override fun onError(
                    download: Download, error: com.tonyodev.fetch2.Error, throwable: Throwable?
                ) {
                    Log.e("eee", "eee onError " + error.name)
                }
            }

            fetch.addListener(fetchListener)


        }
    }
}