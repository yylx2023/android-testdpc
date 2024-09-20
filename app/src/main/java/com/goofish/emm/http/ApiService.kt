// ApiService.kt
package com.goofish.emm.http

import androidx.annotation.Keep
import com.goofish.emm.appstore.App
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/v1/active")
    fun activate(@Body request: ActivationRequest): Call<Resp.Common<ActivationResponse>>

    @POST("/v1/versionCheck")
    fun versionCheck(@Body request: CommonRequest): Call<Resp.Common<VersionCheckResponse>>

    @POST("/v1/appList")
    fun appList(@Body request: CommonRequest): Call<Resp.Common<AppListResponse>>
}

@Keep
data class ActivationRequest(val sn: String)

@Keep
data class ActivationResponse(val token: String)

@Keep
data class CommonRequest(
    val sn: String,
    val versionCode: Int
)

@Keep
data class VersionCheckResponse(
    val apkUrl: String,
    val md5: String,
    val upgradeMsg: String,
    val versionName: String,
    val versionCode: Int,
    val size: String
)


@Keep
data class AppListResponse(
    val apps: MutableList<App>
)