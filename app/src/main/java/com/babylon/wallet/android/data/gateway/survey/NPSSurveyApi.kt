package com.babylon.wallet.android.data.gateway.survey

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface NPSSurveyApi {

    @POST("v1/responses")
    fun storeSurveyResponse(
        @Body request: SurveyRequest
    ): Call<SurveyResponse>
}

@Serializable
data class SurveyRequest(
    @SerialName("id")
    val id: String,

    @SerialName("form_uuid")
    val formUuid: String,

    @SerialName("nps")
    val nps: String,

    @SerialName("what_do_you_value_most_about_our_service")
    val whatDoYouValue: String

)

@Serializable
data class SurveyResponse(
    @SerialName("message")
    val message: String,

    @SerialName("uuid")
    val uuid: String
)
