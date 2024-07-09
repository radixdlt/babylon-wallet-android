package com.babylon.wallet.android.di

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.radixdlt.sargon.Gateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.default
import rdx.works.profile.data.repository.ProfileRepository
import retrofit2.Converter.Factory
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
annotation class DynamicGatewayHttpClient

/**
 * Same as [DynamicGatewayHttpClient] but with shorter timeout
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ShortTimeoutDynamicGatewayHttpClient

/**
 * A simple [OkHttpClient] **without** dynamic change of the base url.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class GatewayHttpClient

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
    @GatewayHttpClient
    fun provideGatewayHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor,
        headerInterceptor: HeaderInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @DynamicGatewayHttpClient
    fun provideDynamicGatewayHttpClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        headerInterceptor: HeaderInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(headerInterceptor)
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @ShortTimeoutDynamicGatewayHttpClient
    fun provideShortTimeoutDynamicGatewayHttpClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        headerInterceptor: HeaderInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(SHORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(headerInterceptor)
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

    class BaseUrlInterceptor @Inject constructor(
        private val profileRepository: ProfileRepository
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Gateway.default.url
            val updatedUrl = chain.request().url
                .newBuilder()
                .host(url.host)
                .scheme(url.scheme)
                .build()

            val request = chain.request().newBuilder()
                .url(updatedUrl)
                .build()
            return chain.proceed(request)
        }
    }

    class HeaderInterceptor @Inject constructor() : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .addHeader(HEADER_RDX_CLIENT_NAME, HEADER_VALUE_RDX_CLIENT_NAME)
                .addHeader(HEADER_RDX_CLIENT_VERSION, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                .build()

            return chain.proceed(request)
        }
    }

    private const val SHORT_TIMEOUT_SECONDS = 5L
}
