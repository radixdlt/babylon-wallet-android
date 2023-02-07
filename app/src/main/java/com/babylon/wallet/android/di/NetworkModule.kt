package com.babylon.wallet.android.di

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.PeerdroidClientImpl
import com.babylon.wallet.android.data.gateway.DynamicUrlApi
import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.converter.Serializer
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.profile.data.repository.ProfileDataSource
import retrofit2.Retrofit
import timber.log.Timber
import java.net.URL
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(profileDataSource: ProfileDataSource): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.d(message)
        }
        val baseUrlInterceptor = Interceptor { chain ->
            runBlocking {
                val baseUrl = profileDataSource.getCurrentNetworkBaseUrl()
                val url = URL(baseUrl)
                val updatedUrl = chain.request().url.newBuilder().host(url.host).scheme(url.protocol).build()
                val request = chain.request().newBuilder().url(updatedUrl).build()
                chain.proceed(request)
            }
        }
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder().addInterceptor(baseUrlInterceptor).addInterceptor(loggingInterceptor).build()
    }

    @Provides
    @Singleton
    fun provideJsonDeserializer(): Json {
        return Serializer.kotlinxSerializationJson
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideGatewayApi(okHttpClient: OkHttpClient, json: Json): GatewayApi {
        val retrofitBuilder = Retrofit.Builder().client(okHttpClient)
            .baseUrl(BuildConfig.GATEWAY_API_URL)
            .addConverterFactory(json.asConverterFactory(Serializer.MIME_TYPE.toMediaType()))
            .build()
        return retrofitBuilder.create(GatewayApi::class.java)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    fun provideGatewayInfoApi(json: Json): DynamicUrlApi {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.d(message)
        }
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val httpClient = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
        val retrofitBuilder = Retrofit.Builder().client(httpClient)
            .baseUrl(BuildConfig.GATEWAY_API_URL)
            .addConverterFactory(json.asConverterFactory(Serializer.MIME_TYPE.toMediaType()))
            .build()
        return retrofitBuilder.create(DynamicUrlApi::class.java)
    }

    @Provides
    @Singleton
    fun providePeerdroidClient(
        peerdroidConnector: PeerdroidConnector
    ): PeerdroidClient = PeerdroidClientImpl(
        peerdroidConnector = peerdroidConnector
    )
}
