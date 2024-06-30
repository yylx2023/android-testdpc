// LoggingInterceptor.kt
package com.goofish.emm.http

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class LoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Log the response body
        val responseBody = response.body
        val content = responseBody?.string()

        // Re-create the response before returning it because body can be read only once
        val newResponse = response.newBuilder()
            .body(okhttp3.ResponseBody.create(responseBody?.contentType(), content ?: ""))
            .build()

        return newResponse
    }
}