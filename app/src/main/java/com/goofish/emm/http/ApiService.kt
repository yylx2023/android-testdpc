// ApiService.kt
package com.goofish.emm.http

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/v1/active")
    fun activate(@Body request: ActivationRequest): Call<Resp.Common<AccivationResponse>>

    @POST("/v1/versionCheck")
    fun versionCheck(@Body request: VersionCheckRequest): Call<Resp.Common<VersionCheckResponse>>
}

data class ActivationRequest(val sn: String)

data class AccivationResponse(val token: String)

data class VersionCheckRequest(
    val sn: String,
    val versionCode: Int
)

data class VersionCheckResponse(
    val apkUrl: String,
    val md5: String,
    val upgradeMsg: String,
    val versionName: String,
    val versionCode: Int,
    val size: String
)
