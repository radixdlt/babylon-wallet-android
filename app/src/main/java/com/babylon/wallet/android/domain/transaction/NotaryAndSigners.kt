package com.babylon.wallet.android.domain.transaction

import rdx.works.profile.data.model.pernetwork.AccountSigner

data class NotaryAndSigners(
    val notarySigner: AccountSigner,
    val signers: List<AccountSigner>
)
