package com.babylon.wallet.android.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network

data class DeviceFactorSourceData(
    val deviceFactorSource: DeviceFactorSource,
    val accounts: ImmutableList<Network.Account> = persistentListOf(),
    val personas: ImmutableList<Network.Persona> = persistentListOf(),
    val mnemonicState: MnemonicState = MnemonicState.NotBackedUp,
    val isBabylon: Boolean = false
) {
    enum class MnemonicState {
        BackedUp, NotBackedUp, NeedRecover
    }
}
