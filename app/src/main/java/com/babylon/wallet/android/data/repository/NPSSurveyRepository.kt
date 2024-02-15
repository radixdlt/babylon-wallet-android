package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.gateway.survey.NPSSurveyApi
import com.babylon.wallet.android.data.gateway.survey.SurveyRequest
import com.babylon.wallet.android.data.gateway.survey.SurveyResponse
import javax.inject.Inject

interface NPSSurveyRepository {

    suspend fun submitSurveyResponse(
        npsQuestion: String,
        reason: String,
    ): Result<SurveyResponse>
}

class NPSSurveyRepositoryImpl @Inject constructor(private val nPSSurveyApi: NPSSurveyApi) : NPSSurveyRepository {

    override suspend fun submitSurveyResponse(
        npsQuestion: String,
        reason: String,
    ): Result<SurveyResponse> {
        return nPSSurveyApi.storeSurveyResponse(
            survetRequest = SurveyRequest(
                userId = BuildConfig.REFINER_USER_ID,
                formUuid = BuildConfig.REFINER_FORM_UUID,
                npsQuestion = npsQuestion,
                reason = reason
            )
        ).toResult()
    }
}
