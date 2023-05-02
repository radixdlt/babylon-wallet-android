package com.babylon.wallet.android.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.utils.isOlympiaAccount
import java.math.BigDecimal

data class AccountResources(
    val address: String,
    val displayName: String,
    val appearanceID: Int,
    val isOlympiaAccount: Boolean = false,
    private val factorSourceState: FactorSourceState = FactorSourceState.Valid,
    val fungibleTokens: ImmutableList<OwnedFungibleToken> = persistentListOf(),
    val nonFungibleTokens: ImmutableList<OwnedNonFungibleToken> = persistentListOf()
) {
    fun hasXrdToken(): Boolean {
        return fungibleTokens.any {
            it.token.metadata[MetadataConstants.KEY_SYMBOL] == MetadataConstants.SYMBOL_XRD
        }
    }

    fun hasXrdWithEnoughBalance(minimumBalance: Long): Boolean {
        return fungibleTokens.any {
            it.token.metadata[MetadataConstants.KEY_SYMBOL] == MetadataConstants.SYMBOL_XRD &&
                it.amount >= BigDecimal(minimumBalance)
        }
    }

    fun needMnemonicBackup(): Boolean {
        return hasXrdWithEnoughBalance(1L) && factorSourceState == FactorSourceState.NeedMnemonicBackup
    }

    fun needMnemonicRecovery(): Boolean {
        return factorSourceState == FactorSourceState.NeedMnemonicRecovery
    }

    enum class FactorSourceState {
        NeedMnemonicRecovery, NeedMnemonicBackup, Valid
    }
}

fun List<AccountResources>.findAccountWithEnoughXRDBalance(minimumBalance: Long) = find {
    it.hasXrdWithEnoughBalance(minimumBalance)
}

fun Network.Account.toDomainModel(): AccountResources {
    return AccountResources(
        address = this.address,
        displayName = displayName,
        appearanceID = this.appearanceID,
        isOlympiaAccount = isOlympiaAccount()
    )
}
