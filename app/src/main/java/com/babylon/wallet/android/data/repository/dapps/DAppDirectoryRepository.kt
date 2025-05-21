package com.babylon.wallet.android.data.repository.dapps

import com.babylon.wallet.android.data.repository.cache.database.DAppDirectoryDao
import com.babylon.wallet.android.data.repository.cache.database.DirectoryDefinitionEntity
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.DirectoryDefinition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import rdx.works.core.InstantGenerator
import rdx.works.core.di.GatewayHttpClient
import retrofit2.Converter
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface DAppDirectoryRepository {

    suspend fun getDirectory(isRefreshing: Boolean): Result<List<DirectoryDefinition>>

}

class DAppDirectoryRepositoryImpl @Inject constructor(
    @GatewayHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
    private val dAppDirectoryDao: DAppDirectoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DAppDirectoryRepository {

    override suspend fun getDirectory(isRefreshing: Boolean): Result<List<DirectoryDefinition>> =
        withContext(ioDispatcher) {
            val cachedDirectory = dAppDirectoryDao.getDirectory(
                minValidity = directoryValidity(isRefreshing = isRefreshing)
            )

            if (cachedDirectory.isEmpty()) {
                fetchDirectory()
                    .onSuccess { directory ->
                        val synced = InstantGenerator()
                        dAppDirectoryDao.insertDirectory(
                            directory = directory.map {
                                DirectoryDefinitionEntity.from(definition = it, synced = synced)
                            }
                        )
                    }
            } else {
                Result.success(cachedDirectory.map { it.toDirectoryDefinition() })
            }
        }

    private suspend fun fetchDirectory(): Result<List<DirectoryDefinition>> =
//        buildApi<DAppDirectoryApi>(
//            baseUrl = BASE_URL,
//            okHttpClient = okHttpClient,
//            jsonConverterFactory = jsonConverterFactory
//        ).directory().toResult()
        runCatching {
            delay(200)
            Json.decodeFromString<List<DirectoryDefinition>>(dAppsJson)
        }

    companion object {
        private const val BASE_URL = "https://dapps-list.radixdlt.com"
        private val directoryCacheDuration = 24.toDuration(DurationUnit.HOURS)

        fun directoryValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else directoryCacheDuration.inWholeMilliseconds

        private val dAppsJson = """
            [
              {
                "name": "Caviarnine",
                "address": "account_rdx12yrjl8m5a4cn9aap2ez2lmvw6g64zgyqnlj4gvugzstye4gnj6assc",
                "tags": ["DeFi", "DEX", "Token", "Trade"]
              },
              {
                "name": "Ociswap",
                "address": "account_rdx12x2ecj3kp4mhq9u34xrdh7njzyz0ewcz4szv0jw5jksxxssnjh7z6z",
                "tags": ["DeFi", "DEX"]
              },
              {
                "name": "Trove",
                "address": "account_rdx128s5u7yqc7p5rw6k7xcq2ug9vz0k2zc94zytaw9s67hxre34e2k5sk",
                "tags": ["Marketplace", "NFTs", "Trade"]
              },
              {
                "name": "Root Finance",
                "address": "account_rdx12ykkpf2v0f3hdqtez9yjhyt04u5ct455aqkq5scd5hlecwf20hcvd2",
                "tags": ["Lending"]
              },
              {
                "name": "Radix Charts",
                "address": "account_rdx16x9mmsy3gasxrn7d2jey6cnzflk8a5w24fghjg082xqa3ncgxjqct3",
                "tags": ["Tools"]
              },
              {
                "name": "Surge",
                "address": "account_rdx12yn43ckkkre9un54424nvck48vf70cgyq8np4ajsrwkc9q3m20ndmd",
                "tags": ["DeFi", "DEX"]
              },
              {
                "name": "Gable Finance",
                "address": "account_rdx128ku70k3nxy9q0ekcwtwucdwm5jt80xsmxnqm5pfqj2dyjswgh3rm3",
                "tags": ["Lending"]
              },
              {
                "name": "Xrd Domains",
                "address": "account_rdx12yctqxnlfqjyn68hrtnxjxkqcvs6hcg4sa6fnst9gfkpruzfeanjke",
                "tags": ["Marketplace", "Tools"]
              },
              {
                "name": "Early",
                "address": "account_rdx1280jw5unz0w3ktmrfpenym6fjxfs7gqedf2rsyqnsgc888ue66ktwl",
                "tags": ["Token"]
              },
              {
                "name": "RadLock",
                "address": "account_rdx12xtayjlrzs0d9rga27w48256d98ycgrpde6yrqpmavmyrr0e4svsqy",
                "tags": ["DeFi", "Tools"]
              },
              {
                "name": "SRWA",
                "address": "account_rdx129f39fsvwt07jlwqhc0pyew8vnh4xxtpxdgz0t9vcyfn07j0jdulrc",
                "tags": ["DeFi", "Lending"]
              },
              {
                "name": "Radix Billboard",
                "address": "account_rdx129vf97vuy4lwcz23gezjhvcflsx5mvfcaqqgy8keunfmfj3kkhhk2f",
                "tags": ["Marketplace"]
              },
              {
                "name": "Jewell Swap",
                "address": "account_rdx12xx94vq4egddx308gg4tkd793yhcsruxyfwrdnxthkt0qfmt6lqhju",
                "tags": ["DeFi", "DEX"]
              },
              {
                "name": "IN:DE",
                "address": "account_rdx12yx3x8fh577ua33hve4r8mw94k6f6chh2mkgfjypm4yht986ns0xep",
                "tags": ["Marketplace", "NFTs"]
              },
              {
                "name": "Astrolescent",
                "address": "account_rdx128y905cfjwhah5nm8mpx5jnlkshmlamfdd92qnqpy6pgk428qlqxcf",
                "tags": ["DeFi", "DEX", "Token"]
              },
              {
                "name": "Shardspace",
                "address": "account_rdx16x5l69u3cpuy59g8n0g7xpv3u3dfmxcgvj8t7y2ukvkjn8pjz2v492",
                "tags": ["Dashboard", "Tools"]
              },
              {
                "name": "XRDEGEN",
                "address": "account_rdx129jet2tlflnxh2l4dhuusdq43s2lmarznw7392rh3dud4560qg6jc2",
                "tags": ["Marketplace", "NFTs"]
              },
              {
                "name": "Radxplorer",
                "address": "account_rdx12840cvuphs90sfzapuuxsu45qx5nalv9p5m43u6nsu4nwwtlvk7r9t",
                "tags": ["Dashboard", "Tools"]
              },
              {
                "name": "Delphibets",
                "address": "account_rdx128m799c5dketq0v07kqukamuxy6zfca0vqttyjj5av6gcdhlkwpy2r",
                "tags": ["DeFi"]
              },
              {
                "name": "Radix API",
                "address": "account_rdx16xz467phhcv969yutwqxv7acy9n2q5ml7hdngfwa5f3vtnldclz49d",
                "tags": ["Tools"]
              },
              {
                "name": "Dexter",
                "address": "account_rdx168qrzyngejus9nazhp7rw9z3qn2r7uk3ny89m5lwvl299ayv87vpn5",
                "tags": ["DeFi", "DEX"]
              },
              {
                "name": "Defiplaza",
                "address": "account_rdx12x2a5dft0gszufcce98ersqvsd8qr5kzku968jd50n8w4qyl9awecr",
                "tags": ["Defi", "DEX"]
              },
              {
                "name": "Weft",
                "address": "account_rdx168r05zkmtvruvqfm4rfmgnpvhw8a47h6ln7vl3rgmyrlzmfvdlfgcg",
                "tags": ["DeFi", "Lending"]
              }
            ]
        """.trimIndent()
    }
}