package com.babylon.wallet.android.presentation.ui

import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate.SeedPhraseWord
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.olympiaimport.OlympiaAccountDetails
import rdx.works.profile.olympiaimport.OlympiaAccountType

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
            address = "account_address_1",
            publicKey = "publicKey1",
            accountName = "account one",
            derivationPath = DerivationPath(path = "path", scheme = DerivationPathScheme.BIP_44_OLYMPIA),
            newBabylonAddress = "babylon_account_address_1",
            appearanceId = 0
        ),

        OlympiaAccountDetails(
            index = 1,
            type = OlympiaAccountType.Hardware,
            address = "account_address_2",
            publicKey = "publicKey2",
            accountName = "account two",
            derivationPath = DerivationPath(path = "path", scheme = DerivationPathScheme.BIP_44_OLYMPIA),
            newBabylonAddress = "babylon_account_address_2",
            appearanceId = 1
        ),
        OlympiaAccountDetails(
            index = 2,
            type = OlympiaAccountType.Hardware,
            address = "account_address_3",
            publicKey = "publicKey3",
            accountName = "account three",
            derivationPath = DerivationPath(path = "path", scheme = DerivationPathScheme.BIP_44_OLYMPIA),
            newBabylonAddress = "babylon_account_address_3",
            appearanceId = 3
        ),
    )

    val accountItemUiModelsList = persistentListOf(
        AccountItemUiModel(
            displayName = "Account name 1",
            address = "account_address_1",
            appearanceID = 1,
            isSelected = true
        ),
        AccountItemUiModel(
            displayName = "Account name 2",
            address = "account_address_2",
            appearanceID = 2,
            isSelected = false
        ),
        AccountItemUiModel(
            displayName = "Account name 3",
            address = "account_address_3",
            appearanceID = 3,
            isSelected = false
        ),
        AccountItemUiModel(
            displayName = "Account name 4",
            address = "account_address_4",
            appearanceID = 4,
            isSelected = false
        )
    )
}
