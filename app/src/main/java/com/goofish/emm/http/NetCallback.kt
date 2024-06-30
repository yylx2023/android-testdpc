// NetCallback.kt
package com.goofish.emm.http

interface NetCallback<T> {
    fun onSuccess(resp: Resp.Common<T>, data: ByteArray)
    fun onNetError(statusCode: Int, msg: String)
}