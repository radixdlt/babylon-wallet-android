package rdx.works.profile.domain

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Persona
import rdx.works.core.sargon.active
import rdx.works.core.sargon.isHidden

data class DeviceFactorSourceData(
    val deviceFactorSource: FactorSource.Device,
    val allAccounts: List<Account> = emptyList(),
    val personas: List<Persona> = emptyList(),
    val mnemonicState: MnemonicState = MnemonicState.NotBackedUp,
    val isBabylon: Boolean = false
) {
    enum class MnemonicState {
        BackedUp, NotBackedUp, NeedRecover
    }

    val activeAccounts = allAccounts.active()

    val hasOnlyHiddenAccounts = allAccounts.isNotEmpty() && allAccounts.all { it.isHidden }
}
