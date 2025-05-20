package com.babylon.wallet.android.presentation.dappdir

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AccountAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import rdx.works.core.domain.DApp
import rdx.works.core.sargon.serializers.AccountAddressSerializer
import rdx.works.core.then
import javax.inject.Inject

@HiltViewModel
class DAppDirectoryViewModel @Inject constructor(
    val getDAppsUseCase: GetDAppsUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<DAppDirectoryViewModel.State>() {

    private val directoryData: MutableStateFlow<Map<DirectoryDefinition, DApp?>?> =
        MutableStateFlow(null)
    private val filters: MutableStateFlow<DAppDirectoryFilters> =
        MutableStateFlow(DAppDirectoryFilters())

    override fun initialState(): State = State(isLoadingDirectory = true)

    init {
        viewModelScope.launch {
            combine(
                directoryData,
                filters.onEach { _state.update { state -> state.copy(filters = it) } }
            ) { data, filters ->
                val directory = data.orEmpty()
                    .asSequence()
                    .filter { (definition, _) ->
                        if (filters.selectedTags.isEmpty()) {
                            true
                        } else {
                            definition.tags.map { it.lowercase() }
                                .containsAll(filters.selectedTags.map { it.lowercase() })
                        }
                    }
                    .filter { (definition, _) ->
                        val term = filters.searchTerm.trim().lowercase()

                        if (term.isBlank()) true else definition.name.lowercase().contains(term)
                    }
                    .toList()
                    .map { entry ->
                        DirectoryDAppWithDetails(
                            directoryDefinition = entry.key,
                            details = entry.value?.let {
                                DirectoryDAppWithDetails.Details.Data(it)
                            } ?: DirectoryDAppWithDetails.Details.Fetching // TODO
                        )
                    }

                data to directory
            }.onEach { (data, directory) ->
                _state.update { state ->
                    state.copy(
                        isLoadingDirectory = data == null,
                        directory = directory
                    )
                }
            }.flowOn(dispatcher).launchIn(viewModelScope)
        }

        viewModelScope.launch {
            fetchDAppsDirectory()
        }
    }

    private suspend fun fetchDAppsDirectory() {
        delay(200)
        runCatching {
            Json.decodeFromString<List<DirectoryDefinition>>(dAppsJson)
        }.map { directory ->
            directoryData.update { data ->
                directory.associateWith { definition -> data?.get(definition) }
            }

            filters.update { filters ->
                filters.copy(
                    availableTags = directory.map { it.tags }.flatten().toSet()
                )
            }

            directoryData.value.orEmpty().filter { it.value == null }
                .keys
                .map { it.dAppDefinitionAddress }
                .toSet()
        }.then { unknownDefinitions ->
            getDAppsUseCase(definitionAddresses = unknownDefinitions, needMostRecentData = false)
        }.onSuccess { dApps ->
            val dAppDefinitionWithDetails = dApps.associateBy { it.dAppAddress }

            directoryData.update { data ->
                data?.mapValues {
                    dAppDefinitionWithDetails[it.key.dAppDefinitionAddress]
                }
            }
        }.onFailure { error ->
            _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
        }
    }

    fun onSearchTermUpdated(term: String) {
        filters.update { it.copy(searchTerm = term) }
    }

    fun onFilterTagAdded(tag: String) {
        filters.update {
            it.copy(selectedTags = it.selectedTags + tag)
        }
    }

    fun onFilterTagRemoved(tag: String) {
        filters.update {
            it.copy(selectedTags = it.selectedTags - tag)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val isLoadingDirectory: Boolean,
        val directory: List<DirectoryDAppWithDetails> = emptyList(),
        val filters: DAppDirectoryFilters = DAppDirectoryFilters(),
        val uiMessage: UiMessage? = null
    ) : UiState

    /////// TEMP
    companion object {
        private val dAppsJson = """
            [
              {
                "name": "Caviarnine",
                "definition_address": "account_rdx12yrjl8m5a4cn9aap2ez2lmvw6g64zgyqnlj4gvugzstye4gnj6assc",
                "tags": ["DeFi", "DEX", "Token", "Trade"]
              },
              {
                "name": "Ociswap",
                "definition_address": "account_rdx12x2ecj3kp4mhq9u34xrdh7njzyz0ewcz4szv0jw5jksxxssnjh7z6z",
                "tags": ["DeFi", "DEX"]
              },
              {
                "name": "Trove",
                "definition_address": "account_rdx128s5u7yqc7p5rw6k7xcq2ug9vz0k2zc94zytaw9s67hxre34e2k5sk",
                "tags": ["Marketplace", "NFTs", "Trade"]
              },
              {
                "name": "Root Finance",
                "definition_address": "account_rdx12ykkpf2v0f3hdqtez9yjhyt04u5ct455aqkq5scd5hlecwf20hcvd2",
                "tags": ["Lending"]
              },
              {
                "name": "Radix Charts",
                "definition_address": "account_rdx16x9mmsy3gasxrn7d2jey6cnzflk8a5w24fghjg082xqa3ncgxjqct3",
                "tags": ["Tools"]
              },
              {
                "name": "Surge",
                "definition_address": "account_rdx12yn43ckkkre9un54424nvck48vf70cgyq8np4ajsrwkc9q3m20ndmd",
                "tags": ["DeFi", "DEX"]
              },
              {
                "name": "Gable Finance",
                "definition_address": "account_rdx128ku70k3nxy9q0ekcwtwucdwm5jt80xsmxnqm5pfqj2dyjswgh3rm3",
                "tags": ["Lending"]
              },
              {
                "name": "Xrd Domains",
                "definition_address": "account_rdx12yctqxnlfqjyn68hrtnxjxkqcvs6hcg4sa6fnst9gfkpruzfeanjke",
                "tags": ["Marketplace", "Tools"]
              },
              {
                "name": "Early",
                "definition_address": "account_rdx1280jw5unz0w3ktmrfpenym6fjxfs7gqedf2rsyqnsgc888ue66ktwl",
                "tags": ["Token"]
              },
              {
                "name": "RadLock",
                "definition_address": "account_rdx12xtayjlrzs0d9rga27w48256d98ycgrpde6yrqpmavmyrr0e4svsqy",
                "tags": ["DeFi", "Tools"]
              },
              {
                "name": "SRWA",
                "definition_address": "account_rdx129f39fsvwt07jlwqhc0pyew8vnh4xxtpxdgz0t9vcyfn07j0jdulrc",
                "tags": ["DeFi", "Lending"]
              },
              {
                "name": "Radix Billboard",
                "definition_address": "account_rdx129vf97vuy4lwcz23gezjhvcflsx5mvfcaqqgy8keunfmfj3kkhhk2f",
                "tags": ["Marketplace"]
              },
              {
                "name": "Jewell Swap",
                "definition_address": "account_rdx12xx94vq4egddx308gg4tkd793yhcsruxyfwrdnxthkt0qfmt6lqhju",
                "tags": ["DeFi", "DEX"]
              },
              {
                "name": "IN:DE",
                "definition_address": "account_rdx12yx3x8fh577ua33hve4r8mw94k6f6chh2mkgfjypm4yht986ns0xep",
                "tags": ["Marketplace", "NFTs"]
              },
              {
                "name": "Astrolescent",
                "definition_address": "account_rdx128y905cfjwhah5nm8mpx5jnlkshmlamfdd92qnqpy6pgk428qlqxcf",
                "tags": ["DeFi", "DEX", "Token"]
              },
              {
                "name": "Shardspace",
                "definition_address": "account_rdx16x5l69u3cpuy59g8n0g7xpv3u3dfmxcgvj8t7y2ukvkjn8pjz2v492",
                "tags": ["Dashboard", "Tools"]
              },
              {
                "name": "XRDEGEN",
                "definition_address": "account_rdx129jet2tlflnxh2l4dhuusdq43s2lmarznw7392rh3dud4560qg6jc2",
                "tags": ["Marketplace", "NFTs"]
              },
              {
                "name": "Radxplorer",
                "definition_address": "account_rdx12840cvuphs90sfzapuuxsu45qx5nalv9p5m43u6nsu4nwwtlvk7r9t",
                "tags": ["Dashboard", "Tools"]
              },
              {
                "name": "Delphibets",
                "definition_address": "account_rdx128m799c5dketq0v07kqukamuxy6zfca0vqttyjj5av6gcdhlkwpy2r",
                "tags": ["DeFi"]
              },
              {
                "name": "Radix API",
                "definition_address": "account_rdx16xz467phhcv969yutwqxv7acy9n2q5ml7hdngfwa5f3vtnldclz49d",
                "tags": ["Tools"]
              },
              {
                "name": "Dexter",
                "definition_address": "account_rdx168qrzyngejus9nazhp7rw9z3qn2r7uk3ny89m5lwvl299ayv87vpn5",
                "tags": ["DeFi", "DEX"]
              },
              {
                "name": "Defiplaza",
                "definition_address": "account_rdx12x2a5dft0gszufcce98ersqvsd8qr5kzku968jd50n8w4qyl9awecr",
                "tags": ["Defi", "DEX"]
              },
              {
                "name": "Weft",
                "definition_address": "account_rdx168r05zkmtvruvqfm4rfmgnpvhw8a47h6ln7vl3rgmyrlzmfvdlfgcg",
                "tags": ["DeFi", "Lending"]
              }
            ]
        """.trimIndent()
    }
}

@Serializable
data class DirectoryDefinition(
    val name: String,
    @Serializable(with = AccountAddressSerializer::class)
    @SerialName(value = "definition_address")
    val dAppDefinitionAddress: AccountAddress,
    val tags: List<String>
)

data class DirectoryDAppWithDetails(
    val directoryDefinition: DirectoryDefinition,
    val details: Details
) {
    val dApp: DApp? = (details as? Details.Data)?.dApp

    val isFetchingDetails: Boolean = details is Details.Fetching

    val name: String = dApp?.name ?: directoryDefinition.name

    val icon: Uri? = dApp?.iconUrl

    val description: String? = dApp?.description

    val tags: List<String> = directoryDefinition.tags

    sealed interface Details {
        data object Fetching : Details

        data class Data(val dApp: DApp) : Details
    }

    companion object
}

data class DAppDirectoryFilters(
    val searchTerm: String = "",
    val selectedTags: Set<String> = emptySet(),
    val availableTags: Set<String> = emptySet()
) {

    fun isTagSelected(tag: String) = tag in selectedTags

}