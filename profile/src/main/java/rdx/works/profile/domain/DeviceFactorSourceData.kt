package rdx.works.profile.domain

import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network

data class DeviceFactorSourceData(
    val deviceFactorSource: DeviceFactorSource,
    val accounts: List<Network.Account> = emptyList(),
    val personas: List<Network.Persona> = emptyList(),
    val mnemonicState: MnemonicState = MnemonicState.NotBackedUp,
    val isBabylon: Boolean = false
) {
    enum class MnemonicState {
        BackedUp, NotBackedUp, NeedRecover
    }
}
