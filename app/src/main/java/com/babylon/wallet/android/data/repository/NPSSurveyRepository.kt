package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.gateway.survey.NPSSurveyApi
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
            params = mapOf(
                NPSSurveyApi.PARAM_ID to BuildConfig.REFINER_USER_ID,
                NPSSurveyApi.PARAM_FORM_UUID to BuildConfig.REFINER_FORM_UUID,
                NPSSurveyApi.PARAM_NPS to npsQuestion,
                NPSSurveyApi.PARAM_WHAT_DO_YOU_VALUE to reason
            )
        ).toResult()
    }
}
