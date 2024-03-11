package com.babylon.wallet.android.data.gateway.survey

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface NPSSurveyApi {

    @POST("v1/responses")
    fun storeSurveyResponse(
        @QueryMap params: Map<String, String>
    ): Call<SurveyResponse>

    companion object {
        const val PARAM_ID = "id"
        const val PARAM_FORM_UUID = "form_uuid"
        const val PARAM_NPS = "nps"
        const val PARAM_WHAT_DO_YOU_VALUE = "what_do_you_value_most_about_our_service"
    }
}

@Serializable
data class SurveyResponse(
    @SerialName("message")
    val message: String,

    @SerialName("uuid")
    val uuid: String
)
