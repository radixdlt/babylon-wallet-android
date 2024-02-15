package com.babylon.wallet.android.data.gateway.survey

import com.babylon.wallet.android.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface NPSSurveyApi {

    @Headers("Authorization: Bearer ${BuildConfig.REFINER_API_KEY}")
    @POST("responses")
    fun storeSurveyResponse(
        @Body survetRequest: SurveyRequest
    ): Call<SurveyResponse>
}

@Serializable
data class SurveyRequest(
    @SerialName("id")
    val userId: String,

    @SerialName("form_uuid")
    val formUuid: String,

    @SerialName("nps_question")
    val npsQuestion: String,

    @SerialName("what_do_you_value_most_about_our_service")
    val reason: String
)

@Serializable
data class SurveyResponse(
    @SerialName("message")
    val message: String,

    @SerialName("uuid")
    val uuid: String
)
