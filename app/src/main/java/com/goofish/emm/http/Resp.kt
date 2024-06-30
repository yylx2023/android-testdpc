// Resp.kt
package com.goofish.emm.http

import androidx.annotation.Keep

@Keep
class Resp {
    companion object {
        const val SUCCESS = "0000"
    }

    @Keep
    data class Common<T>(val code: String, val data: T?, val msg: String)
}

