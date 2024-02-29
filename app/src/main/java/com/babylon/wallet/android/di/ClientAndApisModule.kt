package com.babylon.wallet.android.di

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.PeerdroidClientImpl
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.apis.StreamApi
import com.babylon.wallet.android.data.gateway.apis.TokenPriceApi
import com.babylon.wallet.android.data.gateway.apis.TransactionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.repository.ProfileRepository
import retrofit2.Converter
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClientAndApisModule {

    @Provides
    @Singleton
    @CurrentGatewayHttpClient
    fun provideCurrentGatewayHttpClient(
        baseUrlInterceptor: NetworkModule.BaseUrlInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @ShortTimeoutGatewayHttpClient
    fun provideShortTimeoutGatewayHttpClient(
        baseUrlInterceptor: NetworkModule.BaseUrlInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(SHORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun providePeerdroidClient(
        peerdroidConnector: PeerdroidConnector,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PeerdroidClient = PeerdroidClientImpl(
        peerdroidConnector = peerdroidConnector,
        ioDispatcher = ioDispatcher
    )

    @Provides
    fun provideStateApi(
        @CurrentGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory,
        profileRepository: ProfileRepository
    ): StateApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Radix.Gateway.default.url,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    @ShortTimeoutStateApi
    fun provideStateApiWithShortTimeout(
        @ShortTimeoutGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory,
        profileRepository: ProfileRepository
    ): StateApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Radix.Gateway.default.url,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    fun provideTransactionApi(
        @CurrentGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory,
        profileRepository: ProfileRepository
    ): TransactionApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Radix.Gateway.default.url,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    fun provideStreamApi(
        @CurrentGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory,
        profileRepository: ProfileRepository
    ): StreamApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Radix.Gateway.default.url,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    fun provideTokenPriceApi(
        @SimpleHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Converter.Factory
    ): TokenPriceApi = buildApi(
        baseUrl = TokenPriceApi.BASE_URL,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )
}

private const val SHORT_TIMEOUT_SECONDS = 5L
