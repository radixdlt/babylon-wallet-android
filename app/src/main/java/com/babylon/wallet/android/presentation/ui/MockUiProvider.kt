package com.babylon.wallet.android.presentation.ui

import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.Bip44LikePath
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.LegacyOlympiaAccountAddress
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toBabylonAddress
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import rdx.works.profile.olympiaimport.OlympiaAccountType

@UsesSampleValues
object MockUiProvider {

    val seedPhraseWords = persistentListOf(
        SeedPhraseWord(
            index = 0,
            value = "casino",
            state = SeedPhraseWord.State.Valid,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 1,
            value = "frequent",
            state = SeedPhraseWord.State.Valid,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 2,
            value = "value2",
            state = SeedPhraseWord.State.Invalid,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 3,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 4,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 5,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = true
        ),
        SeedPhraseWord(
            index = 6,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 7,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 8,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = true
        )
    )

    val olympiaAccountsList = listOf(
        OlympiaAccountDetails(
            index = 0,
            type = OlympiaAccountType.Software,
            address = LegacyOlympiaAccountAddress.sample(),
            publicKey = PublicKey.Secp256k1.sample(),
            accountName = "account one",
            derivationPath = DerivationPath.Bip44Like(Bip44LikePath.sample()),
            newBabylonAddress = LegacyOlympiaAccountAddress.sample().toBabylonAddress(),
            appearanceId = AppearanceId(0u)
        ),

        OlympiaAccountDetails(
            index = 1,
            type = OlympiaAccountType.Hardware,
            address = LegacyOlympiaAccountAddress.sample.other(),
            publicKey = PublicKey.Secp256k1.sample.other(),
            accountName = "account two",
            derivationPath = DerivationPath.Bip44Like(Bip44LikePath.sample.other()),
            newBabylonAddress = LegacyOlympiaAccountAddress.sample.other().toBabylonAddress(),
            appearanceId = AppearanceId(1u)
        )
    )

    val accountItemUiModelsList = persistentListOf(
        AccountItemUiModel(
            displayName = "Account name 1",
            address = AccountAddress.sampleMainnet.random(),
            appearanceID = AppearanceId(1u),
            isSelected = true
        ),
        AccountItemUiModel(
            displayName = "Account name 2",
            address = AccountAddress.sampleMainnet.random(),
            appearanceID = AppearanceId(2u),
            isSelected = false
        ),
        AccountItemUiModel(
            displayName = "Account name 3",
            address = AccountAddress.sampleMainnet.random(),
            appearanceID = AppearanceId(3u),
            isSelected = false
        ),
        AccountItemUiModel(
            displayName = "Account name 4",
            address = AccountAddress.sampleMainnet.random(),
            appearanceID = AppearanceId(4u),
            isSelected = false
        )
    )
}
