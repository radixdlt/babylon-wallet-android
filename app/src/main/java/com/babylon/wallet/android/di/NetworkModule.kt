package com.babylon.wallet.android.di

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.PeerdroidClientImpl
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.apis.TransactionApi
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.repository.ProfileRepository
import retrofit2.Converter.Factory
import retrofit2.Retrofit
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class JsonConverterFactory

/**
 * An [OkHttpClient] that can dynamically change the base url of the network,
 * even if the [Retrofit] builder has already created the api class,
 * based on the current gateway
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class CurrentGatewayHttpClient

/**
 * A simple [OkHttpClient] **without** dynamic change of the base url.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class SimpleHttpClient

private const val HEADER_RDX_CLIENT_NAME = "RDX-Client-Name"
private const val HEADER_RDX_CLIENT_VERSION = "RDX-Client-Version"

private const val HEADER_VALUE_RDX_CLIENT_NAME = "Android Wallet"

/**
 * A helper method to build an API class
 */
inline fun <reified T> buildApi(
    baseUrl: String,
    okHttpClient: OkHttpClient,
    jsonConverterFactory: Factory
): T = Retrofit.Builder()
    .client(okHttpClient)
    .baseUrl(baseUrl)
    .addConverterFactory(jsonConverterFactory)
    .build()
    .create(T::class.java)

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.d(message)
        }.apply {
            level = if (BuildConfig.DEBUG_MODE) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    @SimpleHttpClient
    fun provideSimpleHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @CurrentGatewayHttpClient
    fun provideCurrentGatewayHttpClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideJsonDeserializer(): Json {
        return Serializer.kotlinxSerializationJson
    }

    @Provides
    @JsonConverterFactory
    fun provideJsonConverterFactory(json: Json): Factory {
        return json.asConverterFactory(Serializer.MIME_TYPE.toMediaType())
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
        @JsonConverterFactory jsonConverterFactory: Factory,
        profileRepository: ProfileRepository
    ): StateApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Radix.Gateway.default.url,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    @Provides
    fun provideTransactionApi(
        @CurrentGatewayHttpClient okHttpClient: OkHttpClient,
        @JsonConverterFactory jsonConverterFactory: Factory,
        profileRepository: ProfileRepository
    ): TransactionApi = buildApi(
        baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Radix.Gateway.default.url,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    )

    class BaseUrlInterceptor @Inject constructor(
        private val profileRepository: ProfileRepository
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = URL(profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Radix.Gateway.default.url)
            val updatedUrl = chain.request().url
                .newBuilder()
                .host(url.host)
                .scheme(url.protocol)
                .build()

            val request = chain.request().newBuilder()
                .url(updatedUrl)
                .addHeader(HEADER_RDX_CLIENT_NAME, HEADER_VALUE_RDX_CLIENT_NAME)
                .addHeader(HEADER_RDX_CLIENT_VERSION, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                .build()
            return chain.proceed(request)
        }
    }
}
