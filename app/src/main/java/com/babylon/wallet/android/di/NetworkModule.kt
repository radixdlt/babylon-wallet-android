package com.babylon.wallet.android.di

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.dapp.PeerdroidClientImpl
import com.babylon.wallet.android.data.gateway.apis.DynamicUrlApi
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.apis.StatusApi
import com.babylon.wallet.android.data.gateway.apis.TransactionApi
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
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
        loggingInterceptor.level = if (BuildConfig.DEBUG_MODE) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
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
    fun provideStateApi(okHttpClient: OkHttpClient, json: Json): StateApi {
        val retrofitBuilder = Retrofit.Builder().client(okHttpClient)
            .baseUrl(BuildConfig.GATEWAY_API_URL)
            .addConverterFactory(json.asConverterFactory(Serializer.MIME_TYPE.toMediaType()))
            .build()
        return retrofitBuilder.create(StateApi::class.java)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideTransactionApi(okHttpClient: OkHttpClient, json: Json): TransactionApi {
        val retrofitBuilder = Retrofit.Builder().client(okHttpClient)
            .baseUrl(BuildConfig.GATEWAY_API_URL)
            .addConverterFactory(json.asConverterFactory(Serializer.MIME_TYPE.toMediaType()))
            .build()
        return retrofitBuilder.create(TransactionApi::class.java)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideStatusApi(okHttpClient: OkHttpClient, json: Json): StatusApi {
        val retrofitBuilder = Retrofit.Builder().client(okHttpClient)
            .baseUrl(BuildConfig.GATEWAY_API_URL)
            .addConverterFactory(json.asConverterFactory(Serializer.MIME_TYPE.toMediaType()))
            .build()
        return retrofitBuilder.create(StatusApi::class.java)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    fun provideGatewayInfoApi(json: Json): DynamicUrlApi {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.d(message)
        }
        loggingInterceptor.level = if (BuildConfig.DEBUG_MODE) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
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
