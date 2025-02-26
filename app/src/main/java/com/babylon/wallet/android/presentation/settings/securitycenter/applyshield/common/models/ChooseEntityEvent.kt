package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models

import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.radixdlt.sargon.AddressOfAccountOrPersona

sealed interface ChooseEntityEvent : OneOffEvent {

    data class EntitiesSelected(
        val addresses: List<AddressOfAccountOrPersona>
    ) : ChooseEntityEvent
}
