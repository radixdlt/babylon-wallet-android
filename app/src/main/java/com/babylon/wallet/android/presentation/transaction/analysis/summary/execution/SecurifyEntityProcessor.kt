package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.os.SargonOsManager
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class SecurifyEntityProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
) : PreviewTypeProcessor<DetailedManifestClass.SecurifyEntity> {

    override suspend fun process(
        summary: ExecutionSummary,
        classification: DetailedManifestClass.SecurifyEntity
    ): PreviewType {
        val profile = getProfileUseCase()

        val address = classification.entities.first()
        val entity = when (address) {
            is AddressOfAccountOrPersona.Account -> profile.activeAccountsOnCurrentNetwork.find { it.address == address.v1 }
                ?.asProfileEntity()

            is AddressOfAccountOrPersona.Identity -> profile.activePersonasOnCurrentNetwork.find { it.address == address.v1 }
                ?.asProfileEntity()
        } ?: error("Entity not found")

        val structure = sargonOsManager.sargonOs
            .provisionalSecurityStructureOfFactorSourcesFromAddressOfAccountOrPersona(address)

        return PreviewType.UpdateSecurityStructure(
            entity = entity,
            provisionalConfig = structure,
            operation = PreviewType.UpdateSecurityStructure.Operation.ApplySecurityStructure
        )
    }
}
