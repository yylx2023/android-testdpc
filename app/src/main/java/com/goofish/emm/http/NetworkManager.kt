// NetworkManager.kt
package com.goofish.emm.http

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

object NetworkManager {

    fun <T> makeRequest(call: Call<Resp.Common<T>>, callback: NetCallback<T>) {
        call.enqueue(object : Callback<Resp.Common<T>> {
            override fun onResponse(call: Call<Resp.Common<T>>, response: Response<Resp.Common<T>>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == Resp.SUCCESS) {
                        callback.onSuccess(body, ByteArray(0))  // Remove raw body reading
                    } else {
                        callback.onNetError(response.code(), body?.msg ?: "Unknown error")
                    }
                } else {
                    callback.onNetError(response.code(), response.message())
                }
            }

            override fun onFailure(call: Call<Resp.Common<T>>, t: Throwable) {
                if (t is IOException) {
                    callback.onNetError(-1, "Network error: ${t.message}")
                } else {
                    callback.onNetError(-1, "Unknown error: ${t.message}")
                }
            }
        })
    }
}