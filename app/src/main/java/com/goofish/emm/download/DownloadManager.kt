package com.goofish.emm.download

import android.util.Log
import com.goofish.emm.EmmApp
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock


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

            val requestId = request.id

            val fetchListener: FetchListener = object : FetchListener {
                private fun matches(download: Download): Boolean {
                    return download.id == requestId || download.file == path
                }

                private fun removeSelf() {
                    fetch.removeListener(this)
                }

                override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                    if (!matches(download)) return
                    Log.e("eee", "eee onQueued")
                }

                override fun onCompleted(download: Download) {
                    if (!matches(download)) return
                    removeSelf()
                    callback.onCompleted(download)
                }


                override fun onProgress(
                    download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long
                ) {
                    if (!matches(download)) return
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
                    if (!matches(download)) return
                    Log.e("eee", "eee onStarted")
                    callback.onStart()
                }

                override fun onWaitingNetwork(download: Download) {
                }

                override fun onAdded(download: Download) {
                    if (!matches(download)) return
                    Log.e("eee", "eee onAdded")
                }

                override fun onCancelled(download: Download) {
                    if (!matches(download)) return
                    removeSelf()
                }

                override fun onRemoved(download: Download) {
                    if (!matches(download)) return
                    removeSelf()
                }

                override fun onDeleted(download: Download) {
                    if (!matches(download)) return
                    removeSelf()
                }

                override fun onDownloadBlockUpdated(
                    download: Download, downloadBlock: DownloadBlock, totalBlocks: Int
                ) {
                }

                override fun onError(
                    download: Download, error: com.tonyodev.fetch2.Error, throwable: Throwable?
                ) {
                    if (!matches(download)) return
                    removeSelf()
                    Log.e("eee", "eee onError " + error.name)
                }
            }

            fetch.addListener(fetchListener)
            fetch.enqueue(request, { updatedRequest -> }, { error -> fetch.removeListener(fetchListener) })


        }
    }
}