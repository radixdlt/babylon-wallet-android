package com.babylon.wallet.android.di

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.PeerdroidClientImpl
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.apis.StreamApi
import com.babylon.wallet.android.data.gateway.apis.TokenPriceApi
import com.babylon.wallet.android.data.gateway.apis.TransactionApi
import com.babylon.wallet.android.data.gateway.survey.NPSSurveyApi
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.extensions.string
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import rdx.works.core.di.DynamicGatewayHttpClient
import rdx.works.core.di.GatewayHttpClient
import rdx.works.core.di.ShortTimeoutDynamicGatewayHttpClient
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.default
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.profile.data.repository.ProfileRepository
import retrofit2.Converter
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ShortTimeoutStateApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkApiModule {

    @Provides
    @Singleton
    fun providePeerdroidClient(
        peerdroidConnector: PeerdroidConnector,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        json: Json
    ): PeerdroidClient = PeerdroidClientImpl(
        peerdroidConnector = peerdroidConnector,
        ioDispatcher = ioDispatcher,
        json = json
    )

    @Provides
    fun provideStateApi(
        @DynamicGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory,
        profileRepository: ProfileRepository
    ): StateApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.string ?: Gateway.default.string,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    @ShortTimeoutStateApi
    fun provideStateApiWithShortTimeout(
        @ShortTimeoutDynamicGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory,
        profileRepository: ProfileRepository
    ): StateApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.string ?: Gateway.default.string,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    fun provideTransactionApi(
        @DynamicGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory,
        profileRepository: ProfileRepository
    ): TransactionApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.string ?: Gateway.default.string,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    fun provideStreamApi(
        @DynamicGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory,
        profileRepository: ProfileRepository
    ): StreamApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.string ?: Gateway.default.string,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    fun provideTokenPriceApi(
        @GatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory
    ): TokenPriceApi = buildApi(
        baseUrl = TokenPriceApi.BASE_URL,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    fun provideNPSSurveyApi(
        @GatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory
    ): NPSSurveyApi = buildApi(
        baseUrl = BuildConfig.NPS_SURVEY_URL,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )
}
