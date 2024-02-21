package com.babylon.wallet.android.data.repository.tokenprice

import com.babylon.wallet.android.data.gateway.apis.TokenPriceApi
import com.babylon.wallet.android.data.gateway.model.TokenPriceResponse
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import rdx.works.peerdroid.di.IoDispatcher
import retrofit2.Converter
import javax.inject.Inject

interface TokenPriceRepository {

    suspend fun getTokensPrice(): Result<List<TokenPriceResponse>>
}

class TokenPriceRepositoryImpl @Inject constructor(
    @SimpleHttpClient private val okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
) : TokenPriceRepository {

    override suspend fun getTokensPrice(): Result<List<TokenPriceResponse>> = withContext(ioDispatcher) {
        buildApi<TokenPriceApi>(
            baseUrl = BASE_URL,
            okHttpClient = okHttpClient,
            jsonConverterFactory = jsonConverterFactory
        ).tokens().toResult()
    }

    companion object {
        const val BASE_URL = "https://dev-token-price.extratools.works/"
    }
}
