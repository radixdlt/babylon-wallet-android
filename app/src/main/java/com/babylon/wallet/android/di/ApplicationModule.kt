package com.babylon.wallet.android.di

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.MainViewRepositoryImpl
import com.babylon.wallet.android.data.dapp.DAppRepositoryImpl
import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.converter.Serializer
import com.babylon.wallet.android.data.profile.ProfileRepositoryImpl
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.domain.dapp.DAppRepository
import com.babylon.wallet.android.domain.profile.ProfileRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "rdx_datastore"
    )

    @Provides
    @Singleton
    fun provideMainViewRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MainViewRepository = MainViewRepositoryImpl(ioDispatcher)

    @Provides
    @Singleton
    fun provideClipboardManager(
        @ApplicationContext context: Context
    ): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.userDataStore
    }

    @Provides
    @Singleton
    fun provideDAppRepository(): DAppRepository {
        return DAppRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideProfileRepository(): ProfileRepository {
        return ProfileRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.d(message)
        }
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideGatewayApi(): GatewayApi {
        val retrofitBuilder = Retrofit.Builder().client(provideOkHttpClient())
            .baseUrl(BuildConfig.GATEWAY_API_URL)
            .addConverterFactory(Serializer.kotlinxSerializationJson.asConverterFactory(Serializer.MIME_TYPE.toMediaType())).build()
        return retrofitBuilder.create(GatewayApi::class.java)
    }
}
