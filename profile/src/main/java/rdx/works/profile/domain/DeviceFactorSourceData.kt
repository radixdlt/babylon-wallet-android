package rdx.works.profile.domain

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Persona
import rdx.works.core.sargon.notHiddenAccounts

data class DeviceFactorSourceData(
    val deviceFactorSource: FactorSource.Device,
    val allAccounts: List<Account> = emptyList(),
    val personas: List<Persona> = emptyList(),
    val mnemonicState: MnemonicState = MnemonicState.NotBackedUp,
    val isBabylon: Boolean = false,
    val hasOnlyHiddenAccounts: Boolean = false
) {
    enum class MnemonicState {
        BackedUp, NotBackedUp, NeedRecover
    }

    val notHiddenAccounts = allAccounts.notHiddenAccounts()
}
