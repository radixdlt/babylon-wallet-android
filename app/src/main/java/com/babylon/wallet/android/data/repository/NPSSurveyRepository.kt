package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.gateway.survey.NPSSurveyApi
import com.babylon.wallet.android.data.gateway.survey.SurveyRequest
import com.babylon.wallet.android.data.gateway.survey.SurveyResponse
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

interface NPSSurveyRepository {

    suspend fun submitSurveyResponse(
        npsQuestion: String,
        reason: String,
    ): Result<SurveyResponse>
}

class NPSSurveyRepositoryImpl @Inject constructor(
    private val nPSSurveyApi: NPSSurveyApi,
    private val preferencesManager: PreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NPSSurveyRepository {

    override suspend fun submitSurveyResponse(
        npsQuestion: String,
        reason: String,
    ): Result<SurveyResponse> {
        return with(ioDispatcher) {
            val uuid = preferencesManager.surveyUuid.first()
            nPSSurveyApi.storeSurveyResponse(
                SurveyRequest(
                    id = uuid,
                    formUuid = BuildConfig.REFINER_FORM_UUID,
                    nps = npsQuestion,
                    whatDoYouValue = reason
                )
            ).toResult()
        }
    }
}
