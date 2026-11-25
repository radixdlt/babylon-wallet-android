package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.analysis.summary.SummarizedManifest
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.model.ControlOperation
import com.radixdlt.sargon.AccessControllerAddress
import com.radixdlt.sargon.AccessRequirementForDisplay
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.BadgeForDisplay
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.os.SargonOsManager
import rdx.works.core.sargon.accountAddressOrNull
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.asProfileEntity
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.nonFungibleGlobalIdOrNull
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

/**
 * Reviews the access controller operations. A transaction that updates, confirms or stops the shield of one of the
 * user's own entities keeps its own review; anything else, e.g. a shared account, even one in the Profile, or any
 * pre-authorization, shows the operations one by one.
 */
class AccessControllerOperationProcessor @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val generalTransferProcessor: GeneralTransferProcessor,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend fun process(summary: Summary.FromExecution, classification: DetailedManifestClass): PreviewType? {
        val executionSummary = summary.summary
        val isTransaction = summary.manifest is SummarizedManifest.Transaction

        val operations = when (classification) {
            is DetailedManifestClass.AccessControllerRecovery -> {
                ownShieldUpdate(classification.acAddresses, isTransaction, ownOperation = OwnOperation.Update)
                    ?.let { return it }
                classification.recoveryProposals.map(ControlOperation::recovery)
            }

            is DetailedManifestClass.AccessControllerTimedRecovery -> {
                ownShieldUpdate(classification.acAddresses, isTransaction, ownOperation = OwnOperation.Update)
                    ?.let { return it }
                classification.timedRecoveries.map(ControlOperation::timedRecovery)
            }

            is DetailedManifestClass.AccessControllerConfirmRecovery -> {
                ownShieldUpdate(classification.acAddresses, isTransaction, ownOperation = OwnOperation.Confirm)
                    ?.let { return it }
                classification.confirmations.map(ControlOperation::confirmation)
            }

            is DetailedManifestClass.AccessControllerCancelRecovery -> {
                // Stopping a timed recovery of the user's own entity, in a transaction. A pre-authorization
                // always shows the operations.
                ownEntity(classification.acAddresses)?.takeIf { isTransaction }?.let { entity ->
                    return PreviewType.UpdateSecurityStructure(
                        entity = entity,
                        provisionalConfig = null,
                        operation = PreviewType.UpdateSecurityStructure.Operation.StopRecovery
                    )
                }
                classification.cancellations.map(ControlOperation::cancellation)
            }

            is DetailedManifestClass.AccessControllerPrimaryRoleLock ->
                classification.operations.map(ControlOperation::primaryRoleLock)

            is DetailedManifestClass.SecurifiedEntityCreation ->
                classification.createdEntities.map(ControlOperation::created)

            is DetailedManifestClass.AccessControllerRecoveryFee ->
                classification.contributions.map(ControlOperation::recoveryFeeContribution) +
                    classification.withdrawals.map(ControlOperation::recoveryFeeWithdrawal)

            else -> return null
        }

        // A review that leaves out an access controller the manifest calls would get it signed unseen.
        if (!operations.coverAll(classification)) {
            throw RadixWalletException.DappRequestException.UnacceptableManifest(
                cause = IllegalStateException("The access controller operations don't cover every access controller called")
            )
        }

        // Whatever else the manifest moves, e.g. the recovery fees, is reviewed as any other transfer.
        val transfers = generalTransferProcessor.process(executionSummary, DetailedManifestClass.General)
        return (transfers as? PreviewType.Transaction)?.copy(controlOperations = named(operations)) ?: transfers
    }

    /** Whether there are operations, and one on every access controller the manifest calls. */
    private fun List<ControlOperation>.coverAll(classification: DetailedManifestClass): Boolean {
        if (isEmpty()) return false
        val operatedOn = mapNotNull { it.accessController }.toSet()
        return operatedOn.containsAll(classification.accessControllerAddresses)
    }

    /** Names the accounts and keys as the address book does, and marks the accounts that are the user's. */
    private suspend fun named(operations: List<ControlOperation>): List<ControlOperation> {
        val profile = getProfileUseCase()
        val ownAccounts = profile.activeAccountsOnCurrentNetwork.associateBy { it.address }
        val addressBook = profile.currentNetwork?.addressBook.orEmpty()
        val accountNames = addressBook.mapNotNull { entry -> entry.accountAddressOrNull?.let { it to entry.name.value } }.toMap()
        val keyNames = addressBook.mapNotNull { entry -> entry.nonFungibleGlobalIdOrNull?.let { it to entry.name.value } }.toMap()

        return operations.map { operation ->
            val keys = operation.rules?.let { rules ->
                listOf(rules.primaryRole, rules.recoveryRole, rules.confirmationRole).flatMap { it.unknownKeys() }
            }.orEmpty()
            val accountAddress = (operation.controlledEntity as? AddressOfAccountOrPersona.Account)?.v1
            operation.copy(
                accountName = accountAddress?.let(accountNames::get),
                // A created entity's address is only the preview's, so it is never one of the user's.
                ownedAccount = accountAddress?.takeIf { !operation.createsEntity }?.let(ownAccounts::get),
                keyNames = keys.mapNotNull { key -> keyNames[key]?.let { key to it } }.toMap()
            )
        }
    }

    private fun AccessRequirementForDisplay.unknownKeys(): List<NonFungibleGlobalId> = when (this) {
        is AccessRequirementForDisplay.Badge -> listOfNotNull((badge as? BadgeForDisplay.UnknownKey)?.badge)
        is AccessRequirementForDisplay.CountOf -> badges.mapNotNull { (it as? BadgeForDisplay.UnknownKey)?.badge }
        is AccessRequirementForDisplay.AnyOf -> requirements.flatMap { it.unknownKeys() }
        is AccessRequirementForDisplay.AllOf -> requirements.flatMap { it.unknownKeys() }
        else -> emptyList()
    }

    private enum class OwnOperation {
        Update,
        Confirm
    }

    private fun ownEntity(acAddresses: List<AccessControllerAddress>) = acAddresses.firstOrNull()?.let { address ->
        runCatching { sargonOsManager.sargonOs.entityByAccessControllerAddress(address).asProfileEntity() }.getOrNull()
    }

    /** Only a transaction on one of the user's own entities with a provisional shield is an update of their shield. */
    private fun ownShieldUpdate(
        acAddresses: List<AccessControllerAddress>,
        isTransaction: Boolean,
        ownOperation: OwnOperation
    ): PreviewType.UpdateSecurityStructure? {
        if (!isTransaction) return null
        val entity = ownEntity(acAddresses) ?: return null
        val shield = runCatching {
            sargonOsManager.sargonOs.provisionalSecurityStructureOfFactorSourcesFromAddressOfAccountOrPersona(entity.address)
        }.getOrNull() ?: return null

        return PreviewType.UpdateSecurityStructure(
            entity = entity,
            provisionalConfig = shield,
            operation = when (ownOperation) {
                OwnOperation.Update -> PreviewType.UpdateSecurityStructure.Operation.UpdateSecurityStructure
                OwnOperation.Confirm -> PreviewType.UpdateSecurityStructure.Operation.ConfirmRecovery
            }
        )
    }
}
