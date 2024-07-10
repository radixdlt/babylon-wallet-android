package com.babylon.wallet.android.data.gateway.survey

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SurveyRequest(
    @SerialName("id")
    val id: String,

    @SerialName("form_uuid")
    val formUuid: String,

    @EncodeDefault(mode = EncodeDefault.Mode.NEVER)
    @SerialName("nps")
    val nps: Int? = null,

    @EncodeDefault(mode = EncodeDefault.Mode.NEVER)
    @SerialName("what_do_you_value_most_about_our_service")
    val whatDoYouValue: String? = null

)

@Serializable
data class SurveyResponse(
    @SerialName("message")
    val message: String,

    @SerialName("uuid")
    val uuid: String
)
