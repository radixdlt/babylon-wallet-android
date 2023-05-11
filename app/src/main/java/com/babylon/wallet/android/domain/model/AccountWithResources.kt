package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountWithResources(
    val account: Network.Account,
    val resources: Resources?,
    private val factorSourceState: FactorSourceState = FactorSourceState.Valid
) {

    val fungibleResources: List<FungibleResource>
        get() = resources?.fungibleResources ?: emptyList()

    val nonFungibleResources: List<NonFungibleResource>
        get() = resources?.nonFungibleResources ?: emptyList()

    data class FungibleResource(
        val resourceAddress: String,
        val amount: BigDecimal,
        private val nameMetadataItem: NameMetadataItem? = null,
        private val symbolMetadataItem: SymbolMetadataItem? = null,
        private val descriptionMetadataItem: DescriptionMetadataItem? = null,
        private val iconUrlMetadataItem: IconUrlMetadataItem? = null,
    ) {
        val name: String?
            get() = nameMetadataItem?.name // .orEmpty()

        val symbol: String
            get() = symbolMetadataItem?.symbol.orEmpty()

        val isXrd: Boolean
            get() = symbolMetadataItem?.isXrd ?: false

        val description: String
            get() = descriptionMetadataItem?.description.orEmpty()

        val iconUrl: Uri?
            get() = iconUrlMetadataItem?.url
    }

    data class NonFungibleResource(
        val resourceAddress: String,
        val amount: Long,
        private val nameMetadataItem: NameMetadataItem? = null,
        private val descriptionMetadataItem: DescriptionMetadataItem? = null,
        val nftIds: List<String>, // TODO when gateway is ready
    ) {
        val name: String
            get() = nameMetadataItem?.name.orEmpty()

        val description: String
            get() = descriptionMetadataItem?.description.orEmpty()
    }

    fun hasXrd(minimumBalance: Long = 1L): Boolean {
        return fungibleResources.any {
            it.symbol == MetadataConstants.SYMBOL_XRD && it.amount >= BigDecimal(minimumBalance)
        }
    }

    fun needMnemonicBackup(): Boolean {
        return hasXrd() && factorSourceState == FactorSourceState.NeedMnemonicBackup
    }

    fun needMnemonicRecovery(): Boolean {
        return factorSourceState == FactorSourceState.NeedMnemonicRecovery
    }

    enum class FactorSourceState {
        NeedMnemonicRecovery, NeedMnemonicBackup, Valid
    }
}

data class Resources(
    val fungibleResources: List<AccountWithResources.FungibleResource>,
    val nonFungibleResources: List<AccountWithResources.NonFungibleResource>,
)

fun List<AccountWithResources>.findAccountWithEnoughXRDBalance(minimumBalance: Long) = find {
    it.hasXrd(minimumBalance)
}
