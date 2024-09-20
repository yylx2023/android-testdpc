package com.goofish.emm.download

import com.tonyodev.fetch2.Download

interface DownloadCallback {
    fun onStart()

    fun onProgress(progress: Int)

    fun onCompleted(download: Download)
}