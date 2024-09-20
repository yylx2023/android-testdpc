package com.goofish.emm.appstore

import androidx.annotation.Keep

@Keep
data class App(
    val name: String,
    val iconUrl: String,
    val packageName: String,
    val downloadUrl: String,
    val md5: String,
    val size: Long
)